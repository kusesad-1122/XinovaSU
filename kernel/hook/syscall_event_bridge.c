#include "linux/compiler.h"
#include "linux/cred.h"
#include "linux/jump_label.h"
#include "linux/printk.h"
#include "selinux/selinux.h"
#include <asm/syscall.h>
#include <linux/ptrace.h>
#include <linux/static_key.h>

#include "arch.h"
#include "klog.h" // IWYU pragma: keep
#include "hook/tp_marker.h"
#include "feature/sucompat.h"
#include "hook/setuid_hook.h"
#include "policy/app_profile.h"
#include "runtime/xnsusd.h"
#include "sulog/event.h"
#include "hook/syscall_hook.h"
#include "hook/syscall_event_bridge.h"
#include "feature/adb_root.h"
#include "feature/path_hide.h"
#include "feature/vpn_hide.h"

static int xnsu_handle_init_mark_tracker(const char __user **filename_user)
{
    char path[64];
    unsigned long addr;
    const char __user *fn;
    long ret;

    if (unlikely(!filename_user))
        return 0;

    addr = untagged_addr((unsigned long)*filename_user);
    fn = (const char __user *)addr;
    ret = strncpy_from_user(path, fn, sizeof(path));
    if (ret < 0)
        return 0;

    path[sizeof(path) - 1] = '\0';
    if (unlikely(strcmp(path, KSUD_PATH) == 0)) {
        pr_info("hook_manager: escape to root for init executing xnsusd: %d\n", current->pid);
        escape_to_root_for_init();
    } else if (likely(strstr(path, "/app_process") == NULL && strstr(path, "/adbd") == NULL)) {
        pr_info("hook_manager: unmark %d exec %s\n", current->pid, path);
        xnsu_clear_task_tracepoint_flag_if_needed(current);
    }

    return 0;
}

long __nocfi xnsu_hook_newfstatat(int orig_nr, const struct pt_regs *regs)
{
    int *dfd = (int *)&PT_REGS_PARM1(regs);
    const char __user **filename_user = (const char __user **)&PT_REGS_PARM2(regs);
    int *flags;

    if (xnsu_path_hide_should_hide(dfd, filename_user))
        return -ENOENT;

    if (!xnsu_su_compat_enabled)
        return xnsu_syscall_table[orig_nr](regs);

    flags = (int *)&PT_REGS_SYSCALL_PARM4(regs);
    xnsu_handle_stat(dfd, filename_user, flags);

    return xnsu_syscall_table[orig_nr](regs);
}

long __nocfi xnsu_hook_faccessat(int orig_nr, const struct pt_regs *regs)
{
    int *dfd = (int *)&PT_REGS_PARM1(regs);
    const char __user **filename_user = (const char __user **)&PT_REGS_PARM2(regs);
    int *mode;

    if (xnsu_path_hide_should_hide(dfd, filename_user))
        return -ENOENT;

    if (!xnsu_su_compat_enabled)
        return xnsu_syscall_table[orig_nr](regs);

    mode = (int *)&PT_REGS_PARM3(regs);
    xnsu_handle_faccessat(dfd, filename_user, mode, NULL);

    return xnsu_syscall_table[orig_nr](regs);
}

long __nocfi xnsu_hook_openat(int orig_nr, const struct pt_regs *regs)
{
    int *dfd = (int *)&PT_REGS_PARM1(regs);
    const char __user **filename_user = (const char __user **)&PT_REGS_PARM2(regs);

    if (xnsu_path_hide_should_hide(dfd, filename_user))
        return -ENOENT;

    return xnsu_syscall_table[orig_nr](regs);
}

long __nocfi xnsu_hook_getdents64(int orig_nr, const struct pt_regs *regs)
{
    unsigned int fd = (unsigned int)PT_REGS_PARM1(regs);
    void __user *dirp = (void __user *)PT_REGS_PARM2(regs);
    bool filter_paths = xnsu_path_hide_should_filter_dents();
    bool filter_vpn = xnsu_vpn_hide_should_filter_dents();
    long ret;

    if (!filter_paths && !filter_vpn)
        return xnsu_syscall_table[orig_nr](regs);

    // Run the real syscall, then drop hidden entries (path-hide and/or vpn-hide,
    // each filtering the buffer in turn). If a whole batch is fully hidden,
    // filtering yields 0 bytes -- which libc reads as end-of-dir, prematurely
    // stopping enumeration. So loop and fetch the next batch (the real syscall
    // has already advanced the directory position) until we have a non-empty
    // result or hit the true end of directory.
    do {
        ret = xnsu_syscall_table[orig_nr](regs);
        if (ret <= 0)
            return ret;
        if (filter_paths)
            ret = xnsu_path_hide_filter_getdents64(fd, dirp, ret);
        if (filter_vpn && ret > 0)
            ret = xnsu_vpn_hide_filter_getdents64(fd, dirp, ret);
    } while (ret == 0);

    return ret;
}

long __nocfi xnsu_hook_recvmsg(int orig_nr, const struct pt_regs *regs)
{
    unsigned int fd = (unsigned int)PT_REGS_PARM1(regs);
    void __user *msg = (void __user *)PT_REGS_PARM2(regs);
    long ret;

    if (!xnsu_vpn_hide_should_filter_netlink())
        return xnsu_syscall_table[orig_nr](regs);

    // Single-pass: run the real recvmsg, then drop VPN interfaces from the
    // netlink reply. Unlike getdents there is no re-fetch loop -- shrinking the
    // returned length once is enough.
    ret = xnsu_syscall_table[orig_nr](regs);
    if (ret <= 0)
        return ret;
    return xnsu_vpn_hide_filter_netlink_recvmsg(fd, msg, ret);
}

long __nocfi xnsu_hook_recvfrom(int orig_nr, const struct pt_regs *regs)
{
    unsigned int fd = (unsigned int)PT_REGS_PARM1(regs);
    void __user *ubuf = (void __user *)PT_REGS_PARM2(regs);
    unsigned long buflen = (unsigned long)PT_REGS_PARM3(regs);
    long ret;

    if (!xnsu_vpn_hide_should_filter_netlink())
        return xnsu_syscall_table[orig_nr](regs);

    ret = xnsu_syscall_table[orig_nr](regs);
    if (ret <= 0)
        return ret;
    return xnsu_vpn_hide_filter_netlink_recvfrom(fd, ubuf, ret, buflen);
}

DEFINE_STATIC_KEY_TRUE(xnsusd_execve_key);

void xnsu_stop_xnsusd_execve_hook()
{
    static_branch_disable(&xnsusd_execve_key);
}

long __nocfi xnsu_hook_execve(int orig_nr, const struct pt_regs *regs)
{
    const char __user **filename_user = (const char __user **)&PT_REGS_PARM1(regs);
    const char __user *const __user *argv_user = (const char __user *const __user *)PT_REGS_PARM2(regs);
    bool current_is_init = is_init(current_cred());
    struct xnsu_sulog_pending_event *pending_root_execve = NULL;
    long ret;

    if (static_branch_unlikely(&xnsusd_execve_key))
        xnsu_execve_hook_xnsusd(regs);

    if (current_euid().val == 0)
        pending_root_execve = xnsu_sulog_capture_root_execve(*filename_user, argv_user, GFP_KERNEL);

    if (current->pid != 1 && current_is_init) {
        xnsu_handle_init_mark_tracker(filename_user);
        ret = xnsu_adb_root_handle_execve((struct pt_regs *)regs);
        if (ret) {
            pr_err("adb root failed: %ld\n", ret);
        }
    } else if (xnsu_su_compat_enabled) {
        ret = xnsu_handle_execve_sucompat(filename_user, orig_nr, regs);
        xnsu_sulog_emit_pending(pending_root_execve, ret, GFP_KERNEL);
        return ret;
    }

    ret = xnsu_syscall_table[orig_nr](regs);
    xnsu_sulog_emit_pending(pending_root_execve, ret, GFP_KERNEL);
    return ret;
}

long __nocfi xnsu_hook_setresuid(int orig_nr, const struct pt_regs *regs)
{
    uid_t old_uid = current_uid().val;
    long ret = xnsu_syscall_table[orig_nr](regs);

    if (ret < 0)
        return ret;

    xnsu_handle_setresuid(old_uid, current_uid().val);
    return ret;
}
