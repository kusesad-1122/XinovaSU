#![allow(clippy::unreadable_literal)]
use crate::xnsu_uapi;
use std::fs;
use std::os::fd::RawFd;
use std::sync::OnceLock;

// Global driver fd cache
static DRIVER_FD: OnceLock<RawFd> = OnceLock::new();
static INFO_CACHE: OnceLock<xnsu_uapi::xnsu_get_info_cmd> = OnceLock::new();

fn scan_driver_fd() -> Option<RawFd> {
    let fd_dir = fs::read_dir("/proc/self/fd").ok()?;

    for entry in fd_dir.flatten() {
        if let Ok(fd_num) = entry.file_name().to_string_lossy().parse::<i32>() {
            let link_path = format!("/proc/self/fd/{fd_num}");
            if let Ok(target) = fs::read_link(&link_path) {
                let target_str = target.to_string_lossy();
                if target_str.contains("[xnsu_driver]") {
                    return Some(fd_num);
                }
            }
        }
    }

    None
}

// Get cached driver fd
fn init_driver_fd() -> Option<RawFd> {
    let fd = scan_driver_fd();
    if fd.is_none() {
        let mut fd = -1;
        unsafe {
            libc::syscall(
                libc::SYS_reboot,
                xnsu_uapi::XNSU_INSTALL_MAGIC1,
                xnsu_uapi::XNSU_INSTALL_MAGIC2,
                0,
                &mut fd,
            );
        };
        if fd >= 0 { Some(fd) } else { None }
    } else {
        fd
    }
}

// ioctl wrapper using libc
fn ksuctl<T>(request: u32, arg: *mut T) -> std::io::Result<i32> {
    use std::io;

    let fd = *DRIVER_FD.get_or_init(|| init_driver_fd().unwrap_or(-1));
    unsafe {
        let ret = libc::ioctl(fd as libc::c_int, request as i32, arg);
        if ret < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(ret)
        }
    }
}

// API implementations
fn get_info() -> xnsu_uapi::xnsu_get_info_cmd {
    *INFO_CACHE.get_or_init(|| {
        let mut cmd = xnsu_uapi::xnsu_get_info_cmd {
            version: 0,
            flags: 0,
            features: 0,
        };
        let _ = ksuctl(xnsu_uapi::XNSU_IOCTL_GET_INFO, &raw mut cmd);
        cmd
    })
}

pub fn get_version() -> i32 {
    get_info().version as i32
}

pub fn is_late_load() -> bool {
    get_info().flags & xnsu_uapi::XNSU_GET_INFO_FLAG_LATE_LOAD != 0
}

pub fn grant_root() -> std::io::Result<()> {
    ksuctl(xnsu_uapi::XNSU_IOCTL_GRANT_ROOT, std::ptr::null_mut::<u8>())?;
    Ok(())
}

fn report_event(event: u32) {
    let mut cmd = xnsu_uapi::xnsu_report_event_cmd { event };
    let _ = ksuctl(xnsu_uapi::XNSU_IOCTL_REPORT_EVENT, &raw mut cmd);
}

pub fn report_post_fs_data() {
    report_event(xnsu_uapi::EVENT_POST_FS_DATA);
}

pub fn report_boot_complete() {
    report_event(xnsu_uapi::EVENT_BOOT_COMPLETED);
}

pub fn report_module_mounted() {
    report_event(xnsu_uapi::EVENT_MODULE_MOUNTED);
}

pub fn check_kernel_safemode() -> bool {
    let mut cmd = xnsu_uapi::xnsu_check_safemode_cmd { in_safe_mode: 0 };
    let _ = ksuctl(xnsu_uapi::XNSU_IOCTL_CHECK_SAFEMODE, &raw mut cmd);
    cmd.in_safe_mode != 0
}

pub fn set_sepolicy(payload: *const u8, payload_len: u64) -> std::io::Result<i32> {
    let mut ioctl_cmd = crate::xnsu_uapi::xnsu_set_sepolicy_cmd {
        data_len: payload_len,
        data: payload as u64,
    };

    ksuctl(xnsu_uapi::XNSU_IOCTL_SET_SEPOLICY, &raw mut ioctl_cmd)
}

/// Get feature value and support status from kernel
/// Returns (value, supported)
pub fn get_feature(feature_id: u32) -> std::io::Result<(u64, bool)> {
    let mut cmd = xnsu_uapi::xnsu_get_feature_cmd {
        feature_id,
        value: 0,
        supported: 0,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_GET_FEATURE, &raw mut cmd)?;
    Ok((cmd.value, cmd.supported != 0))
}

/// Set feature value in kernel
pub fn set_feature(feature_id: u32, value: u64) -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_set_feature_cmd { feature_id, value };
    ksuctl(xnsu_uapi::XNSU_IOCTL_SET_FEATURE, &raw mut cmd)?;
    Ok(())
}

pub fn get_wrapped_fd(fd: RawFd) -> std::io::Result<RawFd> {
    let mut cmd = xnsu_uapi::xnsu_get_wrapper_fd_cmd {
        fd: fd as u32,
        flags: 0,
    };
    let result = ksuctl(xnsu_uapi::XNSU_IOCTL_GET_WRAPPER_FD, &raw mut cmd)?;
    Ok(result)
}

pub fn get_sulog_fd() -> std::io::Result<RawFd> {
    let mut cmd = xnsu_uapi::xnsu_get_sulog_fd_cmd { flags: 0 };
    let result = ksuctl(xnsu_uapi::XNSU_IOCTL_GET_SULOG_FD, &raw mut cmd)?;
    Ok(result)
}

/// Get mark status for a process (pid=0 returns total marked count)
pub fn mark_get(pid: i32) -> std::io::Result<u32> {
    let mut cmd = xnsu_uapi::xnsu_manage_mark_cmd {
        operation: xnsu_uapi::XNSU_MARK_GET,
        pid,
        result: 0,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(cmd.result)
}

/// Mark a process (pid=0 marks all processes)
pub fn mark_set(pid: i32) -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_manage_mark_cmd {
        operation: xnsu_uapi::XNSU_MARK_MARK,
        pid,
        result: 0,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Unmark a process (pid=0 unmarks all processes)
pub fn mark_unset(pid: i32) -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_manage_mark_cmd {
        operation: xnsu_uapi::XNSU_MARK_UNMARK,
        pid,
        result: 0,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Refresh mark for all running processes
pub fn mark_refresh() -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_manage_mark_cmd {
        operation: xnsu_uapi::XNSU_MARK_REFRESH,
        pid: 0,
        result: 0,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

pub fn nuke_ext4_sysfs(mnt: &str) -> anyhow::Result<()> {
    let c_mnt = std::ffi::CString::new(mnt)?;
    let mut ioctl_cmd = xnsu_uapi::xnsu_nuke_ext4_sysfs_cmd {
        arg: c_mnt.as_ptr() as u64,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_NUKE_EXT4_SYSFS, &raw mut ioctl_cmd)?;
    Ok(())
}

/// Wipe all entries from umount list
pub fn umount_list_wipe() -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_add_try_umount_cmd {
        arg: 0,
        flags: 0,
        mode: xnsu_uapi::XNSU_UMOUNT_WIPE,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Add mount point to umount list
pub fn umount_list_add(path: &str, flags: u32) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = xnsu_uapi::xnsu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags,
        mode: xnsu_uapi::XNSU_UMOUNT_ADD,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Delete mount point from umount list
pub fn umount_list_del(path: &str) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = xnsu_uapi::xnsu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags: 0,
        mode: xnsu_uapi::XNSU_UMOUNT_DEL,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Apply (op=1) or reset (op=0) kernel uname spoofing. Empty strings leave the
/// corresponding utsname field unchanged.
pub fn uts_spoof_apply(op: u8, release: &str, version: &str) -> anyhow::Result<()> {
    let c_release = std::ffi::CString::new(release)?;
    let c_version = std::ffi::CString::new(version)?;
    let mut cmd = xnsu_uapi::xnsu_uts_spoof_cmd {
        release: if release.is_empty() {
            0
        } else {
            c_release.as_ptr() as u64
        },
        version: if version.is_empty() {
            0
        } else {
            c_version.as_ptr() as u64
        },
        op,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_SET_UTS_SPOOF, &raw mut cmd)?;
    Ok(())
}

fn net_isolate_op(operation: u8, uid: u32) -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_net_isolate_cmd { uid, operation };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_NET_ISOLATE, &raw mut cmd)?;
    Ok(())
}

/// Add a uid to the network-isolation blocklist.
pub fn net_isolate_add(uid: u32) -> std::io::Result<()> {
    net_isolate_op(xnsu_uapi::XNSU_NI_ADD as u8, uid)
}

/// Clear the network-isolation blocklist.
pub fn net_isolate_clear() -> std::io::Result<()> {
    net_isolate_op(xnsu_uapi::XNSU_NI_CLEAR as u8, 0)
}

fn vpn_hide_op(operation: u8, uid: u32) -> std::io::Result<()> {
    let mut cmd = xnsu_uapi::xnsu_vpn_hide_cmd { uid, operation };
    ksuctl(xnsu_uapi::XNSU_IOCTL_MANAGE_VPN_HIDE, &raw mut cmd)?;
    Ok(())
}

/// Add a uid to the vpn-hide target list.
pub fn vpn_hide_add(uid: u32) -> std::io::Result<()> {
    vpn_hide_op(xnsu_uapi::XNSU_VH_ADD as u8, uid)
}

/// Clear the vpn-hide target list.
pub fn vpn_hide_clear() -> std::io::Result<()> {
    vpn_hide_op(xnsu_uapi::XNSU_VH_CLEAR as u8, 0)
}

fn path_hide_op(operation: u8, uid: u32, path: Option<&str>, flag: u8) -> anyhow::Result<()> {
    let c_path = path.map(std::ffi::CString::new).transpose()?;
    let mut cmd = xnsu_uapi::xnsu_path_hide_cmd {
        path: c_path.as_ref().map_or(0, |c| c.as_ptr() as u64),
        uid,
        op: operation,
        flag,
    };
    ksuctl(xnsu_uapi::XNSU_IOCTL_SET_PATH_HIDE, &raw mut cmd)?;
    Ok(())
}

/// Add a hidden path.
pub fn path_hide_add_path(path: &str) -> anyhow::Result<()> {
    path_hide_op(xnsu_uapi::XNSU_PH_ADD_PATH as u8, 0, Some(path), 0)
}

/// Clear all hidden paths.
pub fn path_hide_clear_paths() -> anyhow::Result<()> {
    path_hide_op(xnsu_uapi::XNSU_PH_CLEAR_PATHS as u8, 0, None, 0)
}

/// Add a target uid to the path-hide list.
pub fn path_hide_add_uid(uid: u32) -> anyhow::Result<()> {
    path_hide_op(xnsu_uapi::XNSU_PH_ADD_UID as u8, uid, None, 0)
}

/// Clear the path-hide target uid list.
pub fn path_hide_clear_uids() -> anyhow::Result<()> {
    path_hide_op(xnsu_uapi::XNSU_PH_CLEAR_UIDS as u8, 0, None, 0)
}

/// Set the path-hide filter-system flag.
pub fn path_hide_set_filter_system(on: bool) -> anyhow::Result<()> {
    path_hide_op(
        xnsu_uapi::XNSU_PH_SET_FILTER_SYSTEM as u8,
        0,
        None,
        u8::from(on),
    )
}

/// Set current process's process group to init_group (pgid = 0)
pub fn set_init_pgrp() -> std::io::Result<()> {
    ksuctl(
        xnsu_uapi::XNSU_IOCTL_SET_INIT_PGRP,
        std::ptr::null_mut::<u8>(),
    )?;
    Ok(())
}
