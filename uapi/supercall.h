#ifndef __XNSU_UAPI_SUPERCALL_H
#define __XNSU_UAPI_SUPERCALL_H

#include <linux/ioctl.h>
#include <linux/types.h>

#include "uapi/app_profile.h"

/* Magic numbers for reboot hook to install fd */
static const __u32 XNSU_INSTALL_MAGIC1 = 0xDEADBEEF;
static const __u32 XNSU_INSTALL_MAGIC2 = 0xCAFEBABE;

struct xnsu_become_daemon_cmd {
    __u8 token[65]; /* Input: daemon token (null-terminated) */
};

static const __u32 EVENT_POST_FS_DATA = 1;
static const __u32 EVENT_BOOT_COMPLETED = 2;
static const __u32 EVENT_MODULE_MOUNTED = 3;

static const __u32 XNSU_GET_INFO_FLAG_LKM = (1U << 0);
static const __u32 XNSU_GET_INFO_FLAG_MANAGER = (1U << 1);
static const __u32 XNSU_GET_INFO_FLAG_LATE_LOAD = (1U << 2);
static const __u32 XNSU_GET_INFO_FLAG_PR_BUILD = (1U << 3);

struct xnsu_get_info_cmd {
    __u32 version; /* Output: KERNEL_SU_VERSION */
    __u32 flags; /* Output: XNSU_GET_INFO_FLAG_* bits */
    __u32 features; /* Output: max feature ID supported */
};

struct xnsu_report_event_cmd {
    __u32 event; /* Input: EVENT_POST_FS_DATA, EVENT_BOOT_COMPLETED, etc. */
};

struct xnsu_set_sepolicy_cmd {
    __u64 data_len; /* Input: bytes of serialized command payload */
    __aligned_u64 data; /* Input: pointer to serialized payload */
};

struct xnsu_sepolicy_cmd_hdr {
    __u32 cmd; /* Input: command type, CMD_* */
    __u32 subcmd; /* Input: command subtype */
};
/*
 * After each xnsu_sepolicy_cmd_hdr, command arguments are encoded sequentially as:
 * [u32 len][len bytes][\0], where len excludes the trailing '\0'.
 * len == 0 represents ALL.
 * Argument count is derived from cmd:
 * XNSU_SEPOLICY_CMD_NORMAL_PERM=4, XNSU_SEPOLICY_CMD_XPERM=5,
 * XNSU_SEPOLICY_CMD_TYPE_STATE=1, XNSU_SEPOLICY_CMD_TYPE=2,
 * XNSU_SEPOLICY_CMD_TYPE_ATTR=2, XNSU_SEPOLICY_CMD_ATTR=1,
 * XNSU_SEPOLICY_CMD_TYPE_TRANSITION=5, XNSU_SEPOLICY_CMD_TYPE_CHANGE=4,
 * XNSU_SEPOLICY_CMD_GENFSCON=3.
 */

struct xnsu_check_safemode_cmd {
    __u8 in_safe_mode; /* Output: true if in safe mode, false otherwise */
};

/* deprecated */
struct xnsu_get_allow_list_cmd {
    __u32 uids[128]; /* Output: array of allowed/denied UIDs */
    __u32 count; /* Output: number of UIDs in array */
    __u8 allow; /* Input: true for allow list, false for deny list */
};

struct xnsu_new_get_allow_list_cmd {
    __u16 count; /* Input / Output: number of UIDs in array */
    __u16 total_count; /* Output: total number of UIDs in requested list */
    __u32 uids[0]; /* Output: array of allowed/denied UIDs */
};

struct xnsu_uid_granted_root_cmd {
    __u32 uid; /* Input: target UID to check */
    __u8 granted; /* Output: true if granted, false otherwise */
};

struct xnsu_uid_should_umount_cmd {
    __u32 uid; /* Input: target UID to check */
    __u8 should_umount; /* Output: true if should umount, false otherwise */
};

struct xnsu_get_manager_appid_cmd {
    __u32 appid; /* Output: manager app id */
};

struct xnsu_get_app_profile_cmd {
    struct app_profile profile; /* Input/Output: app profile structure */
};

struct xnsu_set_app_profile_cmd {
    struct app_profile profile; /* Input: app profile structure */
};

struct xnsu_get_feature_cmd {
    __u32 feature_id; /* Input: feature ID (enum xnsu_feature_id) */
    __u64 value; /* Output: feature value/state */
    __u8 supported; /* Output: true if feature is supported, false otherwise */
};

struct xnsu_set_feature_cmd {
    __u32 feature_id; /* Input: feature ID (enum xnsu_feature_id) */
    __u64 value; /* Input: feature value/state to set */
};

struct xnsu_get_wrapper_fd_cmd {
    __u32 fd; /* Input: userspace fd */
    __u32 flags; /* Input: flags of userspace fd */
};

struct xnsu_manage_mark_cmd {
    __u32 operation; /* Input: XNSU_MARK_* */
    __s32 pid; /* Input: target pid (0 for all processes) */
    __u32 result; /* Output: for get operation - mark status or reg_count */
};

static const __u32 XNSU_MARK_GET = 1;
static const __u32 XNSU_MARK_MARK = 2;
static const __u32 XNSU_MARK_UNMARK = 3;
static const __u32 XNSU_MARK_REFRESH = 4;

struct xnsu_nuke_ext4_sysfs_cmd {
    __aligned_u64 arg; /* Input: mnt pointer */
};

struct xnsu_add_try_umount_cmd {
    __aligned_u64 arg; /* char ptr, this is the mountpoint */
    __u32 flags; /* this is the flag we use for it */
    __u8 mode; /* denotes what to do with it 0:wipe_list 1:add_to_list 2:delete_entry */
};

struct xnsu_get_sulog_fd_cmd {
    __u32 flags; /* Input: reserved for future use, must be 0 */
};

static const __u8 XNSU_UMOUNT_WIPE = 0; /* ignore everything and wipe list */
static const __u8 XNSU_UMOUNT_ADD = 1; /* add entry (path + flags) */
static const __u8 XNSU_UMOUNT_DEL = 2; /* delete entry, strcmp */

/* IOCTL command definitions */
static const __u32 XNSU_IOCTL_GRANT_ROOT = _IOC(_IOC_NONE, 'K', 1, 0);
static const __u32 XNSU_IOCTL_GET_INFO = _IOC(_IOC_READ, 'K', 2, 0);
static const __u32 XNSU_IOCTL_REPORT_EVENT = _IOC(_IOC_WRITE, 'K', 3, 0);
static const __u32 XNSU_IOCTL_SET_SEPOLICY = _IOC(_IOC_READ | _IOC_WRITE, 'K', 4, 0);
static const __u32 XNSU_IOCTL_CHECK_SAFEMODE = _IOC(_IOC_READ, 'K', 5, 0);
/* deprecated */
static const __u32 XNSU_IOCTL_GET_ALLOW_LIST = _IOC(_IOC_READ | _IOC_WRITE, 'K', 6, 0);
/* deprecated */
static const __u32 XNSU_IOCTL_GET_DENY_LIST = _IOC(_IOC_READ | _IOC_WRITE, 'K', 7, 0);
static const __u32 XNSU_IOCTL_NEW_GET_ALLOW_LIST = _IOWR('K', 6, struct xnsu_new_get_allow_list_cmd);
static const __u32 XNSU_IOCTL_NEW_GET_DENY_LIST = _IOWR('K', 7, struct xnsu_new_get_allow_list_cmd);
static const __u32 XNSU_IOCTL_UID_GRANTED_ROOT = _IOC(_IOC_READ | _IOC_WRITE, 'K', 8, 0);
static const __u32 XNSU_IOCTL_UID_SHOULD_UMOUNT = _IOC(_IOC_READ | _IOC_WRITE, 'K', 9, 0);
static const __u32 XNSU_IOCTL_GET_MANAGER_APPID = _IOC(_IOC_READ, 'K', 10, 0);
static const __u32 XNSU_IOCTL_GET_APP_PROFILE = _IOC(_IOC_READ | _IOC_WRITE, 'K', 11, 0);
static const __u32 XNSU_IOCTL_SET_APP_PROFILE = _IOC(_IOC_WRITE, 'K', 12, 0);
static const __u32 XNSU_IOCTL_GET_FEATURE = _IOC(_IOC_READ | _IOC_WRITE, 'K', 13, 0);
static const __u32 XNSU_IOCTL_SET_FEATURE = _IOC(_IOC_WRITE, 'K', 14, 0);
static const __u32 XNSU_IOCTL_GET_WRAPPER_FD = _IOC(_IOC_WRITE, 'K', 15, 0);
static const __u32 XNSU_IOCTL_MANAGE_MARK = _IOC(_IOC_READ | _IOC_WRITE, 'K', 16, 0);
static const __u32 XNSU_IOCTL_NUKE_EXT4_SYSFS = _IOC(_IOC_WRITE, 'K', 17, 0);
static const __u32 XNSU_IOCTL_ADD_TRY_UMOUNT = _IOC(_IOC_WRITE, 'K', 18, 0);
static const __u32 XNSU_IOCTL_SET_INIT_PGRP = _IO('K', 19);
static const __u32 XNSU_IOCTL_GET_SULOG_FD = _IOW('K', 20, struct xnsu_get_sulog_fd_cmd);

struct xnsu_uts_spoof_cmd {
    __aligned_u64 release; /* Input: user ptr to spoofed release, or 0 to keep */
    __aligned_u64 version; /* Input: user ptr to spoofed version, or 0 to keep */
    __u8 op; /* 0 = reset to captured original, 1 = set spoofed strings */
};
static const __u32 XNSU_IOCTL_SET_UTS_SPOOF = _IOW('K', 21, struct xnsu_uts_spoof_cmd);

struct xnsu_net_isolate_cmd {
    __u32 uid; /* Input: target uid for ADD/REMOVE (appid-normalised in kernel) */
    __u8 operation; /* XNSU_NI_ADD=1, XNSU_NI_REMOVE=2, XNSU_NI_CLEAR=3 */
};
static const __u32 XNSU_NI_ADD = 1;
static const __u32 XNSU_NI_REMOVE = 2;
static const __u32 XNSU_NI_CLEAR = 3;
static const __u32 XNSU_IOCTL_MANAGE_NET_ISOLATE = _IOW('K', 22, struct xnsu_net_isolate_cmd);

struct xnsu_path_hide_cmd {
    __aligned_u64 path; /* Input: user ptr to path string (ADD_PATH/REMOVE_PATH) */
    __u32 uid; /* Input: target uid (ADD_UID/REMOVE_UID; appid-normalised in kernel) */
    __u8 op; /* XNSU_PH_* */
    __u8 flag; /* Input: value for SET_FILTER_SYSTEM */
};
static const __u32 XNSU_PH_ADD_PATH = 1;
static const __u32 XNSU_PH_REMOVE_PATH = 2;
static const __u32 XNSU_PH_CLEAR_PATHS = 3;
static const __u32 XNSU_PH_ADD_UID = 4;
static const __u32 XNSU_PH_REMOVE_UID = 5;
static const __u32 XNSU_PH_CLEAR_UIDS = 6;
static const __u32 XNSU_PH_SET_FILTER_SYSTEM = 7;
static const __u32 XNSU_IOCTL_SET_PATH_HIDE = _IOW('K', 23, struct xnsu_path_hide_cmd);

#endif
