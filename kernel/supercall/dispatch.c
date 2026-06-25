#include <linux/capability.h>
#include <linux/cred.h>
#include <linux/slab.h>
#include <linux/uaccess.h>
#include <linux/version.h>

#include "uapi/supercall.h"
#include "supercall/internal.h"
#include "arch.h" // IWYU pragma: keep
#include "policy/allowlist.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep
#include "xnsu.h"
#include "runtime/xnsusd_boot.h"
#include "feature/kernel_umount.h"
#include "manager/manager_identity.h"
#include "selinux/selinux.h"
#include "infra/file_wrapper.h"
#include "hook/tp_marker.h"
#include "policy/app_profile.h"
#include "sulog/event.h"
#include "sulog/fd.h"
#include "supercall/supercall.h"

static int do_grant_root(void __user *arg)
{
    int ret;
    __u32 audit_uid = current_uid().val;
    __u32 audit_euid = current_euid().val;

    // we already check uid above on allowed_for_su()

    pr_info("allow root for: %d\n", audit_uid);
    ret = escape_with_root_profile();
    xnsu_sulog_emit_grant_root(ret, audit_uid, audit_euid, GFP_KERNEL);

    return ret;
}

static int do_get_info(void __user *arg)
{
    struct xnsu_get_info_cmd cmd = { .version = KERNEL_SU_VERSION, .flags = 0 };

#ifdef MODULE
    cmd.flags |= XNSU_GET_INFO_FLAG_LKM;
#endif

    if (is_manager()) {
        cmd.flags |= XNSU_GET_INFO_FLAG_MANAGER;
    }
    if (xnsu_late_loaded) {
        cmd.flags |= XNSU_GET_INFO_FLAG_LATE_LOAD;
    }
#ifdef EXPECTED_SIZE2
    cmd.flags |= XNSU_GET_INFO_FLAG_PR_BUILD;
#endif
    cmd.features = XNSU_FEATURE_MAX;

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("get_version: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_report_event(void __user *arg)
{
    struct xnsu_report_event_cmd cmd;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        return -EFAULT;
    }

    switch (cmd.event) {
    case EVENT_POST_FS_DATA: {
        static bool post_fs_data_lock = false;
        if (!post_fs_data_lock) {
            post_fs_data_lock = true;
            if (xnsu_late_loaded) {
                pr_info("post-fs-data skipped (late load)\n");
            } else {
                pr_info("post-fs-data triggered\n");
                on_post_fs_data();
            }
        }
        break;
    }
    case EVENT_BOOT_COMPLETED: {
        static bool boot_complete_lock = false;
        if (!boot_complete_lock) {
            boot_complete_lock = true;
            if (xnsu_late_loaded) {
                pr_info("boot_complete skipped (late load)\n");
            } else {
                pr_info("boot_complete triggered\n");
                on_boot_completed();
            }
        }
        break;
    }
    case EVENT_MODULE_MOUNTED: {
        pr_info("module mounted!\n");
        on_module_mounted();
        break;
    }
    default:
        break;
    }

    return 0;
}

static int do_set_sepolicy(void __user *arg)
{
    struct xnsu_set_sepolicy_cmd cmd;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        return -EFAULT;
    }

    return handle_sepolicy((void __user *)cmd.data, cmd.data_len);
}

static int do_check_safemode(void __user *arg)
{
    struct xnsu_check_safemode_cmd cmd;

    cmd.in_safe_mode = xnsu_is_safe_mode();

    if (cmd.in_safe_mode) {
        pr_warn("safemode enabled!\n");
    }

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("check_safemode: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_new_get_allow_list_common(void __user *arg, bool allow)
{
    struct xnsu_new_get_allow_list_cmd cmd;
    int *arr = NULL;
    int err = 0;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        return -EFAULT;
    }

    if (cmd.count) {
        arr = kmalloc(sizeof(int) * cmd.count, GFP_KERNEL);
        if (!arr) {
            return -ENOMEM;
        }
    }

    bool success = xnsu_get_allow_list(arr, cmd.count, &cmd.count, &cmd.total_count, allow);

    if (!success) {
        err = -EFAULT;
        goto out;
    }

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("new_get_allow_list: copy_to_user count failed\n");
        err = -EFAULT;
        goto out;
    }

    if (cmd.count && copy_to_user(&((struct xnsu_new_get_allow_list_cmd *)arg)->uids, arr, sizeof(int) * cmd.count)) {
        pr_err("new_get_allow_list: copy_to_user uids failed\n");
        err = -EFAULT;
    }

out:
    if (arr) {
        kfree(arr);
    }
    return err;
}

static int do_new_get_deny_list(void __user *arg)
{
    return do_new_get_allow_list_common(arg, false);
}

static int do_new_get_allow_list(void __user *arg)
{
    return do_new_get_allow_list_common(arg, true);
}

static int do_get_allow_list_common(void __user *arg, bool allow)
{
    int *arr = NULL;
    int err = 0;
    u16 count;
    u32 out_count;
    static const u16 kSize = 128;

    arr = kmalloc(sizeof(int) * kSize, GFP_KERNEL);
    if (!arr) {
        return -ENOMEM;
    }

    bool success = xnsu_get_allow_list(arr, kSize, &count, NULL, allow);

    if (!success) {
        err = -EFAULT;
        goto out;
    }

    out_count = count;

    if (copy_to_user(arg + offsetof(struct xnsu_get_allow_list_cmd, count), &out_count, sizeof(u32))) {
        pr_err("get_allow_list: copy_to_user count failed\n");
        err = -EFAULT;
        goto out;
    }

    if (copy_to_user(arg, arr, sizeof(u32) * count)) {
        pr_err("get_allow_list: copy_to_user uids failed\n");
        err = -EFAULT;
    }

out:
    if (arr) {
        kfree(arr);
    }
    return err;
}

static int do_get_deny_list(void __user *arg)
{
    return do_get_allow_list_common(arg, false);
}

static int do_get_allow_list(void __user *arg)
{
    return do_get_allow_list_common(arg, true);
}

static int do_uid_granted_root(void __user *arg)
{
    struct xnsu_uid_granted_root_cmd cmd;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        return -EFAULT;
    }

    cmd.granted = xnsu_is_allow_uid_for_current(cmd.uid);

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("uid_granted_root: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_uid_should_umount(void __user *arg)
{
    struct xnsu_uid_should_umount_cmd cmd;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        return -EFAULT;
    }

    cmd.should_umount = xnsu_uid_should_umount(cmd.uid);

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("uid_should_umount: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_get_manager_appid(void __user *arg)
{
    struct xnsu_get_manager_appid_cmd cmd;

    cmd.appid = xnsu_get_manager_appid();

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("get_manager_appid: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_get_app_profile(void __user *arg)
{
#ifdef CONFIG_XNSU_DISABLE_POLICY
    return -EOPNOTSUPP;
#endif
    uid_t uid;
    struct app_profile *profile;
    int ret = 0;

    if (copy_from_user(&uid, (char __user *)arg + offsetof(struct xnsu_get_app_profile_cmd, profile.curr_uid),
                       sizeof(uid_t))) {
        pr_err("get_app_profile: copy_from_user failed\n");
        return -EFAULT;
    }

    rcu_read_lock();
    profile = xnsu_get_app_profile(uid);
    rcu_read_unlock();
    if (!profile) {
        ret = -ENOENT;
    } else {
        if (copy_to_user((char __user *)arg + offsetof(struct xnsu_get_app_profile_cmd, profile), profile,
                         sizeof(struct app_profile))) {
            pr_err("get_app_profile: copy_to_user failed\n");
            ret = -EFAULT;
        }
        xnsu_put_app_profile(profile);
    }
    return ret;
}

static int do_set_app_profile(void __user *arg)
{
#ifdef CONFIG_XNSU_DISABLE_POLICY
    return -EOPNOTSUPP;
#endif

    struct xnsu_set_app_profile_cmd cmd;
    int ret;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("set_app_profile: copy_from_user failed\n");
        return -EFAULT;
    }

    ret = xnsu_set_app_profile(&cmd.profile);
    if (!ret) {
        xnsu_persistent_allow_list();
        xnsu_mark_running_process();
    }
    return ret;
}

static int do_get_feature(void __user *arg)
{
    struct xnsu_get_feature_cmd cmd;
    bool supported;
    int ret;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("get_feature: copy_from_user failed\n");
        return -EFAULT;
    }

    ret = xnsu_get_feature(cmd.feature_id, &cmd.value, &supported);
    cmd.supported = supported ? 1 : 0;

    if (ret && supported) {
        pr_err("get_feature: failed for feature %u: %d\n", cmd.feature_id, ret);
        return ret;
    }

    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("get_feature: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_set_feature(void __user *arg)
{
    struct xnsu_set_feature_cmd cmd;
    int ret;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("set_feature: copy_from_user failed\n");
        return -EFAULT;
    }

    ret = xnsu_set_feature(cmd.feature_id, cmd.value);
    if (ret) {
        pr_err("set_feature: failed for feature %u: %d\n", cmd.feature_id, ret);
        return ret;
    }

    return 0;
}

static int do_get_wrapper_fd(void __user *arg)
{
    if (!xnsu_file_sid) {
        return -EINVAL;
    }

    struct xnsu_get_wrapper_fd_cmd cmd;
    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("get_wrapper_fd: copy_from_user failed\n");
        return -EFAULT;
    }

    return xnsu_install_file_wrapper(cmd.fd);
}

static int do_manage_mark(void __user *arg)
{
    struct xnsu_manage_mark_cmd cmd;
    int ret = 0;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("manage_mark: copy_from_user failed\n");
        return -EFAULT;
    }

    switch (cmd.operation) {
    case XNSU_MARK_GET: {
        // Get task mark status
        ret = xnsu_get_task_mark(cmd.pid);
        if (ret < 0) {
            pr_err("manage_mark: get failed for pid %d: %d\n", cmd.pid, ret);
            return ret;
        }
        cmd.result = (u32)ret;
        break;
    }
    case XNSU_MARK_MARK: {
        if (cmd.pid == 0) {
            xnsu_mark_all_process();
        } else {
            ret = xnsu_set_task_mark(cmd.pid, true);
            if (ret < 0) {
                pr_err("manage_mark: set_mark failed for pid %d: %d\n", cmd.pid, ret);
                return ret;
            }
        }
        break;
    }
    case XNSU_MARK_UNMARK: {
        if (cmd.pid == 0) {
            xnsu_unmark_all_process();
        } else {
            ret = xnsu_set_task_mark(cmd.pid, false);
            if (ret < 0) {
                pr_err("manage_mark: set_unmark failed for pid %d: %d\n", cmd.pid, ret);
                return ret;
            }
        }
        break;
    }
    case XNSU_MARK_REFRESH: {
        xnsu_mark_running_process();
        pr_info("manage_mark: refreshed running processes\n");
        break;
    }
    default: {
        pr_err("manage_mark: invalid operation %u\n", cmd.operation);
        return -EINVAL;
    }
    }
    if (copy_to_user(arg, &cmd, sizeof(cmd))) {
        pr_err("manage_mark: copy_to_user failed\n");
        return -EFAULT;
    }

    return 0;
}

static int do_nuke_ext4_sysfs(void __user *arg)
{
    struct xnsu_nuke_ext4_sysfs_cmd cmd;
    char mnt[256];
    long ret;

    if (copy_from_user(&cmd, arg, sizeof(cmd)))
        return -EFAULT;

    if (!cmd.arg)
        return -EINVAL;

    memset(mnt, 0, sizeof(mnt));

    ret = strncpy_from_user(mnt, cmd.arg, sizeof(mnt));
    if (ret < 0) {
        pr_err("nuke ext4 copy mnt failed: %ld\n", ret);
        return -EFAULT;
    }

    if (ret == sizeof(mnt)) {
        pr_err("nuke ext4 mnt path too long\n");
        return -ENAMETOOLONG;
    }

    pr_info("do_nuke_ext4_sysfs: %s\n", mnt);

    return nuke_ext4_sysfs(mnt);
}

struct list_head mount_list = LIST_HEAD_INIT(mount_list);
DECLARE_RWSEM(mount_list_lock);

static int add_try_umount(void __user *arg)
{
    struct mount_entry *new_entry, *entry, *tmp;
    struct xnsu_add_try_umount_cmd cmd;
    char buf[256] = { 0 };

    if (copy_from_user(&cmd, arg, sizeof cmd))
        return -EFAULT;

    switch (cmd.mode) {
    case XNSU_UMOUNT_WIPE: {
        struct mount_entry *entry, *tmp;
        down_write(&mount_list_lock);
        list_for_each_entry_safe (entry, tmp, &mount_list, list) {
            pr_info("wipe_umount_list: removing entry: %s\n", entry->umountable);
            list_del(&entry->list);
            kfree(entry->umountable);
            kfree(entry);
        }
        up_write(&mount_list_lock);

        return 0;
    }

    case XNSU_UMOUNT_ADD: {
        long len = strncpy_from_user(buf, (const char __user *)cmd.arg, 256);
        if (len <= 0)
            return -EFAULT;

        buf[sizeof(buf) - 1] = '\0';

        new_entry = kzalloc(sizeof(*new_entry), GFP_KERNEL);
        if (!new_entry)
            return -ENOMEM;

        new_entry->umountable = kstrdup(buf, GFP_KERNEL);
        if (!new_entry->umountable) {
            kfree(new_entry);
            return -ENOMEM;
        }

        down_write(&mount_list_lock);

        // disallow dupes
        // if this gets too many, we can consider moving this whole task to a kthread
        list_for_each_entry (entry, &mount_list, list) {
            if (!strcmp(entry->umountable, buf)) {
                pr_info("cmd_add_try_umount: %s is already here!\n", buf);
                up_write(&mount_list_lock);
                kfree(new_entry->umountable);
                kfree(new_entry);
                return -EEXIST;
            }
        }

        // now check flags and add
        // this also serves as a null check
        if (cmd.flags)
            new_entry->flags = cmd.flags;
        else
            new_entry->flags = 0;

        // debug
        list_add(&new_entry->list, &mount_list);
        up_write(&mount_list_lock);
        pr_info("cmd_add_try_umount: %s added!\n", buf);

        return 0;
    }

    // this is just strcmp'd wipe anyway
    case XNSU_UMOUNT_DEL: {
        long len = strncpy_from_user(buf, (const char __user *)cmd.arg, sizeof(buf) - 1);
        if (len <= 0)
            return -EFAULT;

        buf[sizeof(buf) - 1] = '\0';

        down_write(&mount_list_lock);
        list_for_each_entry_safe (entry, tmp, &mount_list, list) {
            if (!strcmp(entry->umountable, buf)) {
                pr_info("cmd_add_try_umount: entry removed: %s\n", entry->umountable);
                list_del(&entry->list);
                kfree(entry->umountable);
                kfree(entry);
            }
        }
        up_write(&mount_list_lock);

        return 0;
    }

    default: {
        pr_err("cmd_add_try_umount: invalid operation %u\n", cmd.mode);
        return -EINVAL;
    }

    } // switch(cmd.mode)

    return 0;
}

static int do_set_init_pgrp(void __user *arg)
{
    int err;
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 15, 0)
    struct pid *pids[PIDTYPE_MAX] = { 0 };
#endif

    write_lock_irq(&tasklist_lock);
    struct task_struct *p = current->group_leader;
    struct pid *init_group = task_pgrp(&init_task);

    err = -EPERM;
    if (task_session(p) != task_session(&init_task))
        goto out;

    err = 0;
    if (task_pgrp(p) != init_group) {
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 15, 0)
        change_pid(pids, p, PIDTYPE_PGID, init_group);
#else
        change_pid(p, PIDTYPE_PGID, init_group);
#endif
    }

out:
    write_unlock_irq(&tasklist_lock);
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 15, 0)
    free_pids(pids);
#endif

    return err;
}

static int do_get_sulog_fd(void __user *arg)
{
    struct xnsu_get_sulog_fd_cmd cmd;

    if (copy_from_user(&cmd, arg, sizeof(cmd))) {
        pr_err("get_sulog_fd: copy_from_user failed\n");
        return -EFAULT;
    }

    if (cmd.flags) {
        pr_err("get_sulog_fd: unsupported flags 0x%x\n", cmd.flags);
        return -EINVAL;
    }

    return xnsu_install_sulog_fd();
}

// IOCTL handlers mapping table
// clang-format off
static const struct xnsu_ioctl_cmd_map xnsu_ioctl_handlers[] = {
    { 
        .cmd = XNSU_IOCTL_GRANT_ROOT,
        .name = "GRANT_ROOT",
        .handler = do_grant_root,
        .perm_check = allowed_for_su 
    },
    {
        .cmd = XNSU_IOCTL_GET_INFO,
        .name = "GET_INFO",
        .handler = do_get_info,
        .perm_check = always_allow
    },
    {
        .cmd = XNSU_IOCTL_REPORT_EVENT,
        .name = "REPORT_EVENT",
        .handler = do_report_event,
        .perm_check = only_root
    },
    {
        .cmd = XNSU_IOCTL_SET_SEPOLICY,
        .name = "SET_SEPOLICY",
        .handler = do_set_sepolicy,
        .perm_check = only_root
    },
    {
        .cmd = XNSU_IOCTL_CHECK_SAFEMODE,
        .name = "CHECK_SAFEMODE",
        .handler = do_check_safemode,
        .perm_check = always_allow
    },
    {
        .cmd = XNSU_IOCTL_GET_ALLOW_LIST,
        .name = "GET_ALLOW_LIST",
        .handler = do_get_allow_list,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_GET_DENY_LIST,
        .name = "GET_DENY_LIST",
        .handler = do_get_deny_list,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_NEW_GET_ALLOW_LIST,
        .name = "NEW_GET_ALLOW_LIST",
        .handler = do_new_get_allow_list,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_NEW_GET_DENY_LIST,
        .name = "NEW_GET_DENY_LIST",
        .handler = do_new_get_deny_list,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_UID_GRANTED_ROOT,
        .name = "UID_GRANTED_ROOT",
        .handler = do_uid_granted_root,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_UID_SHOULD_UMOUNT,
        .name = "UID_SHOULD_UMOUNT",
        .handler = do_uid_should_umount,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_GET_MANAGER_APPID,
        .name = "GET_MANAGER_APPID",
        .handler = do_get_manager_appid,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_GET_APP_PROFILE,
        .name = "GET_APP_PROFILE",
        .handler = do_get_app_profile,
        .perm_check = only_manager
    },
    {
        .cmd = XNSU_IOCTL_SET_APP_PROFILE,
        .name = "SET_APP_PROFILE",
        .handler = do_set_app_profile,
        .perm_check = only_manager
    },
    {
        .cmd = XNSU_IOCTL_GET_FEATURE,
        .name = "GET_FEATURE",
        .handler = do_get_feature,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_SET_FEATURE,
        .name = "SET_FEATURE",
        .handler = do_set_feature,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_GET_WRAPPER_FD,
        .name = "GET_WRAPPER_FD",
        .handler = do_get_wrapper_fd,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_MANAGE_MARK,
        .name = "MANAGE_MARK",
        .handler = do_manage_mark,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_NUKE_EXT4_SYSFS,
        .name = "NUKE_EXT4_SYSFS",
        .handler = do_nuke_ext4_sysfs,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_ADD_TRY_UMOUNT,
        .name = "ADD_TRY_UMOUNT",
        .handler = add_try_umount,
        .perm_check = manager_or_root
    },
    {
        .cmd = XNSU_IOCTL_SET_INIT_PGRP,
        .name = "SET_INIT_PGRP",
        .handler = do_set_init_pgrp,
        .perm_check = only_root
    },
    {
        .cmd = XNSU_IOCTL_GET_SULOG_FD,
        .name = "GET_SULOG_FD",
        .handler = do_get_sulog_fd,
        .perm_check = only_root
    },
    {
        .cmd = 0,
        .name = NULL,
        .handler = NULL,
        .perm_check = NULL
    } // Sentinel
};
// clang-format on

long xnsu_supercall_handle_ioctl(unsigned int cmd, void __user *argp)
{
    int i;

#ifdef CONFIG_XNSU_DEBUG
    pr_info("ksu ioctl: cmd=0x%x from uid=%d\n", cmd, current_uid().val);
#endif

    for (i = 0; xnsu_ioctl_handlers[i].handler; i++) {
        if (cmd == xnsu_ioctl_handlers[i].cmd) {
            // Check permission first
            if (xnsu_ioctl_handlers[i].perm_check && !xnsu_ioctl_handlers[i].perm_check()) {
                pr_warn("ksu ioctl: permission denied for cmd=0x%x uid=%d\n", cmd, current_uid().val);
                return -EPERM;
            }
            // Execute handler
            return xnsu_ioctl_handlers[i].handler(argp);
        }
    }

    pr_warn("ksu ioctl: unsupported command 0x%x\n", cmd);
    return -ENOTTY;
}

void __init xnsu_supercall_dump_commands(void)
{
    int i;

    pr_info("XinovaSU IOCTL Commands:\n");
    for (i = 0; xnsu_ioctl_handlers[i].handler; i++) {
        pr_info("  %-18s = 0x%08x\n", xnsu_ioctl_handlers[i].name, xnsu_ioctl_handlers[i].cmd);
    }
}

void xnsu_supercall_cleanup_state(void)
{
    struct mount_entry *entry, *tmp;

    down_write(&mount_list_lock);
    list_for_each_entry_safe (entry, tmp, &mount_list, list) {
        list_del(&entry->list);
        kfree(entry->umountable);
        kfree(entry);
    }
    up_write(&mount_list_lock);
}
