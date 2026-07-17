#include "linux/printk.h"
#include <linux/spinlock.h>
#include <linux/kprobes.h>
#include <linux/tracepoint.h>
#include <asm/syscall.h>
#include <linux/ptrace.h>
#include <linux/slab.h>
#include <trace/events/syscalls.h>

#include <linux/version.h>
#if LINUX_VERSION_CODE < KERNEL_VERSION(6, 7, 0)
#include <linux/compat.h>
#include <linux/sched/task_stack.h>
#endif

#include "arch.h"
#include "klog.h" // IWYU pragma: keep
#include "hook/syscall_hook_manager.h"
#include "hook/tp_marker.h"
#include "feature/sucompat.h"
#include "hook/setuid_hook.h"
#include "hook/syscall_hook.h"
#include "hook/syscall_event_bridge.h"

#ifdef CONFIG_KRETPROBES

static struct kretprobe *init_kretprobe(const char *name, kretprobe_handler_t handler)
{
    struct kretprobe *rp = kzalloc(sizeof(struct kretprobe), GFP_KERNEL);
    if (!rp)
        return NULL;
    rp->kp.symbol_name = name;
    rp->handler = handler;
    rp->data_size = 0;
    rp->maxactive = 0;

    int ret = register_kretprobe(rp);
    pr_info("hook_manager: register_%s kretprobe: %d\n", name, ret);
    if (ret) {
        kfree(rp);
        return NULL;
    }

    return rp;
}

static void destroy_kretprobe(struct kretprobe **rp_ptr)
{
    struct kretprobe *rp = *rp_ptr;
    if (!rp)
        return;
    unregister_kretprobe(rp);
    synchronize_rcu();
    kfree(rp);
    *rp_ptr = NULL;
}

static int syscall_regfunc_handler(struct kretprobe_instance *ri, struct pt_regs *regs)
{
    unsigned long flags;
    xnsu_tp_marker_lock(&flags);
    if (xnsu_tp_marker_reg_count() < 1) {
        // while install our tracepoint, mark our processes
        xnsu_mark_running_process_locked();
    } else if (xnsu_tp_marker_reg_count() == 1) {
        // while other tracepoint first added, mark all processes
        xnsu_mark_all_process();
    }
    xnsu_tp_marker_inc_reg_count();
    xnsu_tp_marker_unlock(&flags);
    return 0;
}

static int syscall_unregfunc_handler(struct kretprobe_instance *ri, struct pt_regs *regs)
{
    unsigned long flags;
    xnsu_tp_marker_lock(&flags);
    xnsu_tp_marker_dec_reg_count();
    if (xnsu_tp_marker_reg_count() <= 0) {
        // while no tracepoint left, unmark all processes
        xnsu_unmark_all_process();
    } else if (xnsu_tp_marker_reg_count() == 1) {
        // while just our tracepoint left, unmark disallowed processes
        xnsu_mark_running_process_locked();
    }
    xnsu_tp_marker_unlock(&flags);
    return 0;
}

static struct kretprobe *syscall_regfunc_rp = NULL;
static struct kretprobe *syscall_unregfunc_rp = NULL;
#endif

#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
// sys_enter handler: redirect hooked syscalls to the dispatcher
static void xnsu_sys_enter_handler(void *data, struct pt_regs *regs, long id)
{
#if defined(__x86_64__)
    if (unlikely(in_compat_syscall()))
#elif defined(__aarch64__)
    if (unlikely(is_compat_task()))
#endif
        return;

    if (xnsu_dispatcher_nr < 0)
        return;

    if (xnsu_has_syscall_hook(id)) {
        struct pt_regs *current_regs = task_pt_regs(current);

#if defined(__x86_64__)
        // Stash the original syscall number in ax.
        // We use ax because it currently just holds -ENOSYS and is safe to overwrite.
        current_regs->ax = id;
        current_regs->orig_ax = xnsu_dispatcher_nr;
#elif defined(__aarch64__)
        PT_REGS_ORIG_SYSCALL(current_regs) = id;
        current_regs->syscallno = xnsu_dispatcher_nr;
#endif
    }
}
#endif

void __init xnsu_syscall_hook_manager_init(void)
{
    int ret;
    pr_info("hook_manager: xnsu_hook_manager_init called\n");

#ifdef CONFIG_KRETPROBES
    syscall_regfunc_rp = init_kretprobe("syscall_regfunc", syscall_regfunc_handler);
    syscall_unregfunc_rp = init_kretprobe("syscall_unregfunc", syscall_unregfunc_handler);
#endif

    // Register syscall hooks via dispatcher
    xnsu_register_syscall_hook(__NR_setresuid, xnsu_hook_setresuid);
    xnsu_register_syscall_hook(__NR_execve, xnsu_hook_execve);
    xnsu_register_syscall_hook(__NR_newfstatat, xnsu_hook_newfstatat);
    xnsu_register_syscall_hook(__NR_faccessat, xnsu_hook_faccessat);
    xnsu_register_syscall_hook(__NR_openat, xnsu_hook_openat);
    xnsu_register_syscall_hook(__NR_getdents64, xnsu_hook_getdents64);
    // vpn-hide: filter netlink RTM_GETLINK/GETADDR replies (getifaddrs /
    // NetworkInterface). Gated per-call on the vpn-hide feature + target.
    xnsu_register_syscall_hook(__NR_recvmsg, xnsu_hook_recvmsg);
    xnsu_register_syscall_hook(__NR_recvfrom, xnsu_hook_recvfrom);

#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
    ret = register_trace_sys_enter(xnsu_sys_enter_handler, NULL);
#ifndef CONFIG_KRETPROBES
    xnsu_mark_running_process_locked();
#endif
    if (ret) {
        pr_err("hook_manager: failed to register sys_enter tracepoint: %d\n", ret);
    } else {
        pr_info("hook_manager: sys_enter tracepoint registered\n");
    }
#endif

    xnsu_setuid_hook_init();
    xnsu_sucompat_init();
}

void __exit xnsu_syscall_hook_manager_exit(void)
{
    pr_info("hook_manager: xnsu_hook_manager_exit called\n");
#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
    unregister_trace_sys_enter(xnsu_sys_enter_handler, NULL);
    tracepoint_synchronize_unregister();
    pr_info("hook_manager: sys_enter tracepoint unregistered\n");
#endif

#ifdef CONFIG_KRETPROBES
    destroy_kretprobe(&syscall_regfunc_rp);
    destroy_kretprobe(&syscall_unregfunc_rp);
#endif

    xnsu_unregister_syscall_hook(__NR_setresuid);
    xnsu_unregister_syscall_hook(__NR_execve);
    xnsu_unregister_syscall_hook(__NR_newfstatat);
    xnsu_unregister_syscall_hook(__NR_faccessat);
    xnsu_unregister_syscall_hook(__NR_openat);
    xnsu_unregister_syscall_hook(__NR_getdents64);
    xnsu_unregister_syscall_hook(__NR_recvmsg);
    xnsu_unregister_syscall_hook(__NR_recvfrom);

    xnsu_syscall_hook_exit();

    xnsu_sucompat_exit();
    xnsu_setuid_hook_exit();
}
