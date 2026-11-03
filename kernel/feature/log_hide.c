// SPDX-License-Identifier: GPL-2.0-only
/*
 * Kernel-side log suppression.
 *
 * Two optional stubs, installed by rewriting the first instruction of a
 * resolved kernel function with a "return <const>" sequence:
 *
 *   audit_log_start  -> return NULL   (every audit_log_* caller bails out)
 *   avc_denied       -> return 0      (no "avc: denied" record produced)
 *
 * Both targets are resolved by name at enable time. A target that cannot be
 * resolved is skipped with a warning so a KMI that renamed or inlined it does
 * not take the whole feature down.
 *
 * The original instruction word is saved before patching and restored on
 * disable, so this is fully reversible without a reboot.
 */

#include <linux/kernel.h>
#include <linux/printk.h>
#include <linux/slab.h>
#include <linux/spinlock.h>
#include <linux/static_key.h>
#include <linux/string.h>
#include <linux/types.h>
#include <asm/byteorder.h>

#include "feature/log_hide.h"
#include "hook/patch_memory.h"
#include "infra/symbol_resolver.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep

#define XNSU_LH_MAX_TARGETS 8
#define XNSU_LH_NAME_LEN 64

/* a64 instruction encodings used as stubs */
#define A64_MOV_X0_0 0xD2800000u /* mov  x0, #0   */
#define A64_MOV_W0_0 0x52800000u /* mov  w0, #0   */
#define A64_RET 0xD65F03C0u /* ret           */

enum lh_ret_kind {
    LH_RET_NULL = 0, /* pointer-returning: return NULL  */
    LH_RET_ZERO = 1, /* int-returning:     return 0     */
};

struct lh_target {
    char name[XNSU_LH_NAME_LEN];
    enum lh_ret_kind kind;
    void *addr; /* resolved function entry          */
    u32 orig_insn; /* saved first instruction word      */
    u32 orig_insn2; /* saved second word (stub is 8 B)   */
    bool installed;
};

static DEFINE_SPINLOCK(lh_lock);
static struct lh_target lh_targets[XNSU_LH_MAX_TARGETS];
static int lh_target_count;

static DEFINE_STATIC_KEY_FALSE(xnsu_log_hide);
static bool lh_enabled;

/**
 * lh_patch_entry - rewrite a function entry with an 8-byte stub
 * @t: target whose .addr/.kind must already be set
 *
 * Stub is "mov x0/w0, #0 ; ret". The two original instruction words are saved
 * into @t first so lh_unpatch_entry() can put them back.
 *
 * Return: 0 on success, negative errno from xnsu_patch_text() otherwise.
 */
static int lh_patch_entry(struct lh_target *t)
{
    u32 stub[2];
    __le32 orig_le[2];
    int ret;

    if (!t || !t->addr)
        return -EINVAL;

    stub[0] = (t->kind == LH_RET_NULL) ? A64_MOV_X0_0 : A64_MOV_W0_0;
    stub[1] = A64_RET;

    /* Read through __le32 like the kernel's own text_poke helpers do, to keep
     * sparse/CONFIG_CPU_BIG_ENDIAN builds quiet. On arm64 these are plain
     * 32-bit instructions. */
    memcpy(orig_le, t->addr, sizeof(orig_le));
    t->orig_insn = le32_to_cpu(orig_le[0]);
    t->orig_insn2 = le32_to_cpu(orig_le[1]);

    ret = xnsu_patch_text(t->addr, stub, sizeof(stub), XNSU_PATCH_TEXT_FLUSH_DCACHE);
    if (ret) {
        pr_err("log_hide: patch %s failed: %d\n", t->name, ret);
        return ret;
    }

    t->installed = true;
    pr_info("log_hide: %s patched, orig=0x%08x kinds=%d\n", t->name, t->orig_insn, (int)t->kind);
    return 0;
}

static void lh_unpatch_entry(struct lh_target *t)
{
    __le32 orig[2];
    int ret;

    if (!t || !t->installed || !t->addr)
        return;

    orig[0] = cpu_to_le32(t->orig_insn);
    orig[1] = cpu_to_le32(t->orig_insn2);

    ret = xnsu_patch_text(t->addr, (u32 *)orig, sizeof(orig), XNSU_PATCH_TEXT_FLUSH_DCACHE);
    if (ret)
        pr_err("log_hide: restore %s failed: %d\n", t->name, ret);
    else
        pr_info("log_hide: %s restored\n", t->name);

    t->installed = false;
}

/*
 * Resolve and install one target by name. Returns 0 on success, -ENOSYS if the
 * symbol is not present in this kernel.
 */
static int lh_install_by_name(const char *name, enum lh_ret_kind kind)
{
    struct lh_target *t;
    void *addr = NULL;
    int ret;

    if (!name || !name[0])
        return -EINVAL;
    if (lh_target_count >= XNSU_LH_MAX_TARGETS)
        return -ENOSPC;

    addr = xnsu_resolve_symbol_for_functable_hook(name);
    if (!addr) {
        pr_warn("log_hide: symbol '%s' not found, skipped\n", name);
        return -ENOSYS;
    }

    t = &lh_targets[lh_target_count];
    memset(t, 0, sizeof(*t));
    strscpy(t->name, name, sizeof(t->name));
    t->kind = kind;
    t->addr = addr;

    ret = lh_patch_entry(t);
    if (ret) {
        /* Leave the slot clean so a later target reuses it instead of
         * inheriting a half-filled entry. */
        memset(t, 0, sizeof(*t));
        return ret;
    }

    lh_target_count++;
    return 0;
}

static int lh_install_defaults(void)
{
    int installed = 0;

    /* audit_log_start(): struct audit_buffer *audit_log_start(...) */
    if (lh_install_by_name("audit_log_start", LH_RET_NULL) == 0)
        installed++;

    /* avc_denied(): int avc_denied(...) */
    if (lh_install_by_name("avc_denied", LH_RET_ZERO) == 0)
        installed++;

    return installed;
}

static void lh_uninstall_all(void)
{
    int i;

    for (i = lh_target_count - 1; i >= 0; i--)
        lh_unpatch_entry(&lh_targets[i]);

    lh_target_count = 0;
    memset(lh_targets, 0, sizeof(lh_targets));
}

/* ---- feature handler ---- */

static int log_hide_feature_get(u64 *value)
{
    *value = static_key_enabled(&xnsu_log_hide) ? 1 : 0;
    return 0;
}

static int log_hide_feature_set(u64 value)
{
    int installed;
    unsigned long flags;

    if (value) {
        /* Resolve + patch outside the lock: xnsu_patch_text() goes through
         * stop_machine(), which must not be called with IRQs disabled. */
        if (READ_ONCE(lh_enabled))
            return 0;

        installed = lh_install_defaults();
        if (installed == 0) {
            pr_err("log_hide: no target could be patched\n");
            return -ENOSYS;
        }

        spin_lock_irqsave(&lh_lock, flags);
        static_key_enable(&xnsu_log_hide.key);
        WRITE_ONCE(lh_enabled, true);
        spin_unlock_irqrestore(&lh_lock, flags);

        pr_info("log_hide: enabled, %d target(s)\n", installed);
    } else {
        if (!READ_ONCE(lh_enabled))
            return 0;

        spin_lock_irqsave(&lh_lock, flags);
        static_key_disable(&xnsu_log_hide.key);
        WRITE_ONCE(lh_enabled, false);
        spin_unlock_irqrestore(&lh_lock, flags);

        lh_uninstall_all();
        pr_info("log_hide: disabled\n");
    }

    return 0;
}

static const struct xnsu_feature_handler log_hide_handler = {
    .feature_id = XNSU_FEATURE_LOG_HIDE,
    .name = "log_hide",
    .get_handler = log_hide_feature_get,
    .set_handler = log_hide_feature_set,
};

void __init xnsu_log_hide_init(void)
{
    if (xnsu_register_feature_handler(&log_hide_handler))
        pr_err("log_hide: failed to register feature handler\n");
}

void __exit xnsu_log_hide_exit(void)
{
    unsigned long flags;

    if (READ_ONCE(lh_enabled)) {
        spin_lock_irqsave(&lh_lock, flags);
        static_key_disable(&xnsu_log_hide.key);
        WRITE_ONCE(lh_enabled, false);
        spin_unlock_irqrestore(&lh_lock, flags);

        lh_uninstall_all();
    }

    xnsu_unregister_feature_handler(XNSU_FEATURE_LOG_HIDE);
}

/* ---- programmatic add/remove (for future per-target control) ---- */

int xnsu_log_hide_add_symbol(const char *symbol_name, u16 ret_mode)
{
    enum lh_ret_kind kind;

    if (ret_mode != 0 && ret_mode != 1)
        return -EINVAL;
    kind = (ret_mode == 0) ? LH_RET_NULL : LH_RET_ZERO;

    return lh_install_by_name(symbol_name, kind);
}

int xnsu_log_hide_remove_symbol(const char *symbol_name)
{
    unsigned long flags;
    struct lh_target victim;
    bool found = false;
    int i;

    if (!symbol_name || !symbol_name[0])
        return -EINVAL;

    spin_lock_irqsave(&lh_lock, flags);
    for (i = 0; i < lh_target_count; i++) {
        if (strncmp(lh_targets[i].name, symbol_name, XNSU_LH_NAME_LEN) == 0) {
            victim = lh_targets[i];
            memmove(&lh_targets[i], &lh_targets[i + 1], (size_t)(lh_target_count - i - 1) * sizeof(lh_targets[0]));
            lh_target_count--;
            found = true;
            break;
        }
    }
    spin_unlock_irqrestore(&lh_lock, flags);

    if (!found)
        return -ENOENT;

    /* Unpatch outside the lock (stop_machine must not run IRQs-off). */
    lh_unpatch_entry(&victim);
    return 0;
}

void xnsu_log_hide_clear_symbols(void)
{
    /* lh_uninstall_all() patches text; run it lock-free. */
    lh_uninstall_all();
}
