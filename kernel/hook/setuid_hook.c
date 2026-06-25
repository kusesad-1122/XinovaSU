#include <linux/compiler.h>
#include <linux/version.h>
#include <linux/slab.h>
#include <linux/task_work.h>
#include <linux/thread_info.h>
#include <linux/seccomp.h>
#include <linux/printk.h>
#include <linux/sched.h>
#include <linux/sched/signal.h>
#include <linux/string.h>
#include <linux/types.h>
#include <linux/uaccess.h>
#include <linux/uidgid.h>

#include "policy/allowlist.h"
#include "hook/setuid_hook.h"
#include "klog.h" // IWYU pragma: keep
#include "manager/manager_identity.h"
#include "infra/seccomp_cache.h"
#include "supercall/supercall.h"
#include "hook/tp_marker.h"
#include "feature/kernel_umount.h"

int xnsu_handle_setresuid(uid_t old_uid, uid_t new_uid)
{
    // we rely on the fact that zygote always call setresuid(3) with same uids

    pr_info("handle_setresuid from %d to %d\n", old_uid, new_uid);

    if (unlikely(is_uid_manager(new_uid))) {
        spin_lock_irq(&current->sighand->siglock);
        xnsu_seccomp_allow_cache(current->seccomp.filter, __NR_reboot);
        xnsu_set_task_tracepoint_flag(current);
        spin_unlock_irq(&current->sighand->siglock);

        pr_info("install fd for manager: %d\n", new_uid);
        xnsu_install_fd();
        return 0;
    }

    if (xnsu_is_allow_uid_for_current(new_uid)) {
        if (current->seccomp.mode == SECCOMP_MODE_FILTER && current->seccomp.filter) {
            spin_lock_irq(&current->sighand->siglock);
            xnsu_seccomp_allow_cache(current->seccomp.filter, __NR_reboot);
            spin_unlock_irq(&current->sighand->siglock);
        }
        xnsu_set_task_tracepoint_flag(current);
    } else {
        xnsu_clear_task_tracepoint_flag_if_needed(current);
    }

    // Handle kernel umount
    xnsu_handle_umount(old_uid, new_uid);

    return 0;
}

void __init xnsu_setuid_hook_init(void)
{
    xnsu_kernel_umount_init();
}

void __exit xnsu_setuid_hook_exit(void)
{
    pr_info("xnsu_core_exit\n");
    xnsu_kernel_umount_exit();
}
