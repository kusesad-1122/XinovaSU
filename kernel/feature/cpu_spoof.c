// SPDX-License-Identifier: GPL-2.0
/*
 * CPU spoof — kernel-level path redirection for hardware-identity files.
 *
 * Ported from xinmax-lkm's VFS getname redirect into the XinovaSU feature
 * framework (path_hide.c is the sibling "hide" feature).
 *
 * Why a getname hook instead of the primitives we already have:
 *   - path_hide returns -ENOENT ("this file does not exist"). CPU spoofing
 *     must *replace content* — the reader has to receive a different file's
 *     bytes, not an error.
 *   - /proc/cpuinfo, /proc/cmdline and /proc/bootconfig are opened by
 *     unprivileged detection SDKs with the *reader's* credentials, so no
 *     permission or bind-mount trick applies.
 *   - procfs regenerates those files on every read, so patching the on-disk
 *     copy is impossible.
 * getname_flags() is the single place where a user path becomes a struct
 * filename; substituting the decoy there is transparent to every reader.
 *
 * The pre-handler runs in preempt-disabled (atomic) context, therefore:
 *   - the user string is copied with copy_from_user_nofault() (never sleeps)
 *   - the replacement struct filename is built with GFP_ATOMIC out of
 *     names_cachep, so the kernel's own putname()/__putname() frees it
 *
 * Rules live in a small spinlock-protected fixed table: the lookup runs on
 * every path resolution while the feature is on, so it must be allocation-free
 * on the hot path and O(small).
 */

#include <linux/atomic.h>
#include <linux/err.h>
#include <linux/fs.h>
#include <linux/kprobes.h>
#include <linux/module.h>
#include <linux/ptrace.h>
#include <linux/slab.h>
#include <linux/spinlock.h>
#include <linux/string.h>
#include <linux/uaccess.h>
#include <linux/version.h>

#include "feature/cpu_spoof.h"
#include "infra/symbol_resolver.h"
#include "policy/feature.h"
#include "arch.h"
#include "klog.h" // IWYU pragma: keep

struct xnsu_cs_rule {
	bool used;
	char src[XNSU_CS_PATH_MAX];
	char dst[XNSU_CS_PATH_MAX];
};

static struct xnsu_cs_rule xnsu_cs_rules[XNSU_CS_MAX_RULES];
static DEFINE_SPINLOCK(xnsu_cs_lock);
static atomic_t xnsu_cs_rule_count = ATOMIC_INIT(0);
static bool xnsu_cs_enabled;
static bool xnsu_cs_kp_registered;
static struct kmem_cache *xnsu_cs_names_cachep;

/*
 * Build a putname()-compatible struct filename in atomic context.
 *
 * getname_kernel() cannot be used here: its __getname() allocates with
 * GFP_KERNEL and may schedule, which is illegal with preemption disabled.
 * Allocating the object from names_cachep keeps it freeable by __putname().
 */
static struct filename *xnsu_cs_getname_atomic(const char *path)
{
	struct filename *res;
	size_t len = strlen(path) + 1;
	size_t embedded_max = PATH_MAX - offsetof(struct filename, iname);

	if (!xnsu_cs_names_cachep || len > embedded_max)
		return ERR_PTR(-ENAMETOOLONG);

	res = kmem_cache_alloc(xnsu_cs_names_cachep, GFP_ATOMIC);
	if (!res)
		return ERR_PTR(-ENOMEM);

	memcpy((char *)res->iname, path, len);
	res->name = res->iname;
	res->uptr = NULL;
	res->aname = NULL;
	/* struct filename.refcnt became atomic_t in v6.5 (KMI: plain int on
	 * 5.x/6.1, atomic_t on 6.6+). Keep this builder portable over all 7
	 * supported KMIs. */
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 5, 0)
	atomic_set(&res->refcnt, 1);
#else
	res->refcnt = 1;
#endif
	return res;
}

/* Caller must hold xnsu_cs_lock. Returns a GFP_ATOMIC copy of the decoy. */
static char *xnsu_cs_lookup_locked(const char *path)
{
	int i;

	for (i = 0; i < XNSU_CS_MAX_RULES; i++) {
		if (xnsu_cs_rules[i].used && strcmp(xnsu_cs_rules[i].src, path) == 0)
			return kstrdup(xnsu_cs_rules[i].dst, GFP_ATOMIC);
	}
	return NULL;
}

static int __nocfi xnsu_cs_getname_pre(struct kprobe *p, struct pt_regs *regs)
{
	const char __user *filename_user;
	struct filename *fname;
	char buf[XNSU_CS_PATH_MAX];
	char *dst;
	unsigned long flags;

	(void)p;

	if (unlikely(!READ_ONCE(xnsu_cs_enabled)))
		return 0;
	if (atomic_read(&xnsu_cs_rule_count) == 0)
		return 0;

	filename_user = (const char __user *)PT_REGS_PARM1(regs);
	if (!filename_user)
		return 0;

	if (copy_from_user_nofault(buf, filename_user, sizeof(buf) - 1))
		return 0;
	buf[sizeof(buf) - 1] = '\0';

	/* Only absolute paths can match a redirect rule. */
	if (buf[0] != '/')
		return 0;

	spin_lock_irqsave(&xnsu_cs_lock, flags);
	dst = xnsu_cs_lookup_locked(buf);
	spin_unlock_irqrestore(&xnsu_cs_lock, flags);
	if (!dst)
		return 0;

	fname = xnsu_cs_getname_atomic(dst);
	kfree(dst);
	if (IS_ERR(fname))
		return 0;

	/*
	 * Hand the decoy's struct filename back and skip the original
	 * getname_flags() entirely: set the return register, then jump to the
	 * caller's return address. On x86_64 that address sits on the stack,
	 * so pop it to complete a manual "ret"; on arm64 it is in LR.
	 */
	PT_REGS_RC(regs) = (unsigned long)fname;
#if defined(__x86_64__)
	instruction_pointer_set(regs, *(unsigned long *)PT_REGS_RET(regs));
	regs->sp += 8;
#else
	instruction_pointer_set(regs, PT_REGS_RET(regs));
#endif
	return 1;
}

static struct kprobe xnsu_cs_getname_kp = {
	.symbol_name = "getname_flags",
	.pre_handler = xnsu_cs_getname_pre,
};

int xnsu_cpu_spoof_add_rule(const char *src, const char *dst)
{
	unsigned long flags;
	int i, slot = -1, ret = 0;

	if (!src || src[0] != '/' || strlen(src) >= XNSU_CS_PATH_MAX)
		return -EINVAL;
	if (!dst || dst[0] != '/' || strlen(dst) >= XNSU_CS_PATH_MAX)
		return -EINVAL;
	if (!xnsu_cs_names_cachep)
		return -ENOSYS;

	spin_lock_irqsave(&xnsu_cs_lock, flags);
	for (i = 0; i < XNSU_CS_MAX_RULES; i++) {
		if (xnsu_cs_rules[i].used) {
			/* Re-adding an existing source re-points the decoy. */
			if (strcmp(xnsu_cs_rules[i].src, src) == 0) {
				strscpy(xnsu_cs_rules[i].dst, dst, XNSU_CS_PATH_MAX);
				goto out;
			}
		} else if (slot < 0) {
			slot = i;
		}
	}
	if (slot < 0) {
		ret = -ENOSPC;
		goto out;
	}
	strscpy(xnsu_cs_rules[slot].src, src, XNSU_CS_PATH_MAX);
	strscpy(xnsu_cs_rules[slot].dst, dst, XNSU_CS_PATH_MAX);
	xnsu_cs_rules[slot].used = true;
	atomic_inc(&xnsu_cs_rule_count);
out:
	spin_unlock_irqrestore(&xnsu_cs_lock, flags);
	if (!ret)
		pr_info("cpu_spoof: rule %s -> %s\n", src, dst);
	return ret;
}

int xnsu_cpu_spoof_remove_rule(const char *src)
{
	unsigned long flags;
	int i, ret = -ENOENT;

	if (!src || !src[0])
		return -EINVAL;

	spin_lock_irqsave(&xnsu_cs_lock, flags);
	for (i = 0; i < XNSU_CS_MAX_RULES; i++) {
		if (xnsu_cs_rules[i].used && strcmp(xnsu_cs_rules[i].src, src) == 0) {
			memset(&xnsu_cs_rules[i], 0, sizeof(xnsu_cs_rules[i]));
			atomic_dec(&xnsu_cs_rule_count);
			ret = 0;
			break;
		}
	}
	spin_unlock_irqrestore(&xnsu_cs_lock, flags);
	return ret;
}

void xnsu_cpu_spoof_clear_rules(void)
{
	unsigned long flags;

	spin_lock_irqsave(&xnsu_cs_lock, flags);
	memset(xnsu_cs_rules, 0, sizeof(xnsu_cs_rules));
	atomic_set(&xnsu_cs_rule_count, 0);
	spin_unlock_irqrestore(&xnsu_cs_lock, flags);
}

static int xnsu_cs_feature_get(u64 *value)
{
	*value = READ_ONCE(xnsu_cs_enabled) ? 1 : 0;
	return 0;
}

static int xnsu_cs_feature_set(u64 value)
{
	if (value) {
		/* Install the hook lazily: with the switch off there is exactly
		 * zero overhead on the getname_flags() hot path. */
		if (!xnsu_cs_kp_registered) {
			int rc = register_kprobe(&xnsu_cs_getname_kp);

			if (rc) {
				pr_err("cpu_spoof: register getname_flags kprobe failed: %d\n", rc);
				return rc;
			}
			xnsu_cs_kp_registered = true;
		}
		WRITE_ONCE(xnsu_cs_enabled, true);
	} else {
		WRITE_ONCE(xnsu_cs_enabled, false);
		if (xnsu_cs_kp_registered) {
			unregister_kprobe(&xnsu_cs_getname_kp);
			xnsu_cs_kp_registered = false;
		}
		/* Drop the rules as well: keeping decoy targets behind an off
		 * switch would be a silent failure mode. */
		xnsu_cpu_spoof_clear_rules();
	}
	pr_info("cpu_spoof: set to %llu\n", value);
	return 0;
}

static const struct xnsu_feature_handler cpu_spoof_handler = {
	.feature_id = XNSU_FEATURE_CPU_SPOOF,
	.name = "cpu_spoof",
	.get_handler = xnsu_cs_feature_get,
	.set_handler = xnsu_cs_feature_set,
};

void __init xnsu_cpu_spoof_init(void)
{
	unsigned long nc;

	if (xnsu_register_feature_handler(&cpu_spoof_handler))
		pr_err("Failed to register cpu_spoof feature handler\n");

	memset(xnsu_cs_rules, 0, sizeof(xnsu_cs_rules));

	/* names_cachep is a pointer *variable*, not a function: resolve its
	 * address and dereference it to obtain the cache. It is not exported,
	 * but it is in kallsyms, so the shared resolver finds it. */
	nc = (unsigned long)xnsu_resolve_symbol_for_functable_hook("names_cachep");
	if (nc)
		xnsu_cs_names_cachep = *(struct kmem_cache **)nc;
	if (!xnsu_cs_names_cachep)
		pr_warn("cpu_spoof: names_cachep not found, redirect unavailable\n");
}

void __exit xnsu_cpu_spoof_exit(void)
{
	WRITE_ONCE(xnsu_cs_enabled, false);
	if (xnsu_cs_kp_registered) {
		unregister_kprobe(&xnsu_cs_getname_kp);
		xnsu_cs_kp_registered = false;
	}
	xnsu_cpu_spoof_clear_rules();
	xnsu_unregister_feature_handler(XNSU_FEATURE_CPU_SPOOF);
}
