#include <linux/export.h>
#include <linux/fs.h>
#include <linux/kobject.h>
#include <linux/module.h>
#include <linux/rcupdate.h>
#include <linux/sched.h>
#include <linux/workqueue.h>
#include <linux/moduleparam.h>

#include "policy/allowlist.h"
#include "policy/app_profile.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep
#include "manager/manager_observer.h"
#include "manager/throne_tracker.h"
#include "hook/syscall_hook_manager.h"
#include "hook/lsm_hook.h"
#include "runtime/xnsusd.h"
#include "runtime/xnsusd_boot.h"
#include "feature/sulog.h"
#include "supercall/supercall.h"
#include "xnsu.h"
#include "infra/file_wrapper.h"
#include "selinux/selinux.h"
#include "hook/syscall_hook.h"
#include "feature/adb_root.h"
#include "feature/kernel_spoof.h"
#include "feature/net_isolate.h"
#include "feature/path_hide.h"
#include "feature/selinux_hide.h"
#include "infra/symbol_resolver.h"

#if defined(__x86_64__)
#include <asm/cpufeature.h>
#include <linux/version.h>
#ifndef X86_FEATURE_INDIRECT_SAFE
#error "FATAL: Your kernel is missing the indirect syscall bypass patches!"
#endif
#endif

// workaround for A12-5.10 kernel
// Some third-party kernel (e.g. linegaeOS) uses wrong toolchain, which supports
// CC_HAVE_STACKPROTECTOR_SYSREG while gki's toolchain doesn't.
// Therefore, ksu lkm, which uses gki toolchain, requires this __stack_chk_guard,
// while those third-party kernel can't provide.
// Thus, we manually provide it instead of using kernel's
#if defined(CONFIG_STACKPROTECTOR) &&                                                                                  \
    (defined(CONFIG_ARM64) && defined(MODULE) && !defined(CONFIG_STACKPROTECTOR_PER_TASK))
#include <linux/stackprotector.h>
#include <linux/random.h>
unsigned long __stack_chk_guard __ro_after_init __attribute__((visibility("hidden")));

__attribute__((no_stack_protector)) void __init xnsu_setup_stack_chk_guard()
{
    unsigned long canary;

    /* Try to get a semi random initial value. */
    get_random_bytes(&canary, sizeof(canary));
    canary ^= LINUX_VERSION_CODE;
    canary &= CANARY_MASK;
    __stack_chk_guard = canary;
}

__attribute__((naked)) int __init xinovasu_init_early(void)
{
    asm("mov x19, x30;\n"
        "bl xnsu_setup_stack_chk_guard;\n"
        "mov x30, x19;\n"
        "b xinovasu_init;\n");
}
#define NEED_OWN_STACKPROTECTOR 1
#else
#define NEED_OWN_STACKPROTECTOR 0
#endif

struct cred *xnsu_cred;
bool xnsu_late_loaded;

#ifdef CONFIG_XNSU_DEBUG
bool allow_shell = true;
#else
bool allow_shell = false;
#endif
module_param(allow_shell, bool, 0);

bool xnsu_no_custom_rc = false;
module_param_named(norc, xnsu_no_custom_rc, bool, 0);

int __init xinovasu_init(void)
{
#if defined(__x86_64__)
    // If the kernel has the hardening patch, X86_FEATURE_INDIRECT_SAFE must be set
    if (!boot_cpu_has(X86_FEATURE_INDIRECT_SAFE)) {
        pr_alert("*************************************************************");
        pr_alert("**     NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE    **");
        pr_alert("**                                                         **");
        pr_alert("**        X86_FEATURE_INDIRECT_SAFE is not enabled!        **");
        pr_alert("**      XinovaSU will abort initialization to prevent      **");
        pr_alert("**                     kernel panic.                       **");
        pr_alert("**                                                         **");
        pr_alert("**     NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE    **");
        pr_alert("*************************************************************");
        return -ENOSYS;
    }
#endif

#ifdef MODULE
    xnsu_late_loaded = (current->pid != 1);
#else
    xnsu_late_loaded = false;
#endif

#ifdef CONFIG_XNSU_DEBUG
    pr_alert("*************************************************************");
    pr_alert("**     NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE    **");
    pr_alert("**                                                         **");
    pr_alert("**         You are running XinovaSU in DEBUG mode          **");
    pr_alert("**                                                         **");
    pr_alert("**     NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE NOTICE    **");
    pr_alert("*************************************************************");
#endif
    if (allow_shell) {
        pr_alert("shell is allowed at init!");
    }

    xnsu_cred = prepare_creds();
    if (!xnsu_cred) {
        pr_err("prepare cred failed!\n");
    }

    xnsu_init_symbol_resolver();
    xnsu_syscall_hook_init();

    xnsu_feature_init();
    xnsu_sulog_init();
    xnsu_adb_root_init();
    xnsu_kernel_spoof_init();
    xnsu_lsm_hook_init();
    xnsu_selinux_hide_init();
    xnsu_net_isolate_init();
    xnsu_path_hide_init();

    xnsu_supercalls_init();

    if (xnsu_late_loaded) {
        pr_info("late load mode, skipping kprobe hooks\n");

        apply_xinovasu_rules();
        cache_sid();
        setup_xnsu_cred();

        // Grant current process (xnsusd late-load) root
        // with KSU SELinux domain before enforcing SELinux, so it
        // can continue to access /data/app etc. after enforcement.
        escape_to_root_for_init();

        xnsu_allowlist_init();
        xnsu_load_allow_list();

        xnsu_syscall_hook_manager_init();

        xnsu_throne_tracker_init();
        xnsu_observer_init();
        xnsu_file_wrapper_init();

        xnsu_boot_completed = true;
        track_throne(false);

        if (!getenforce()) {
            pr_info("Permissive SELinux, enforcing\n");
            setenforce(true);
        }

    } else {
        xnsu_syscall_hook_manager_init();

        xnsu_allowlist_init();

        xnsu_throne_tracker_init();

        xnsu_xnsusd_init();

        xnsu_file_wrapper_init();
    }

#ifdef MODULE
#ifndef CONFIG_XNSU_DEBUG
    kobject_del(&THIS_MODULE->mkobj.kobj);
#endif
#endif
    return 0;
}

void __exit xinovasu_exit(void)
{
    // Phase 1: Stop all hooks first to prevent new callbacks
    xnsu_syscall_hook_manager_exit();

    xnsu_supercalls_exit();

    if (!xnsu_late_loaded)
        xnsu_xnsusd_exit();

    // Wait for any in-flight RCU readers (e.g. handler traversing allow_list)
    synchronize_rcu();

    // Phase 2: Now safe to release data structures
    xnsu_observer_exit();

    xnsu_throne_tracker_exit();

    xnsu_allowlist_exit();

    xnsu_selinux_hide_exit();
    xnsu_net_isolate_exit();
    xnsu_path_hide_exit();
    xnsu_lsm_hook_exit();
    xnsu_adb_root_exit();
    xnsu_kernel_spoof_exit();
    xnsu_sulog_exit();
    xnsu_feature_exit();

    if (xnsu_cred) {
        put_cred(xnsu_cred);
    }
}

#if NEED_OWN_STACKPROTECTOR
module_init(xinovasu_init_early);
#else
module_init(xinovasu_init);
#endif
module_exit(xinovasu_exit);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("weishu");
MODULE_DESCRIPTION("Android XinovaSU");
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 13, 0)
MODULE_IMPORT_NS("VFS_internal_I_am_really_a_filesystem_and_am_NOT_a_driver");
#else
MODULE_IMPORT_NS(VFS_internal_I_am_really_a_filesystem_and_am_NOT_a_driver);
#endif
