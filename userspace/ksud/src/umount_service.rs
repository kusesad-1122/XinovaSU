//! User-configurable umount service.
//!
//! Lets the user pin a list of mount points that should be unmounted for
//! non-root apps, reusing the existing kernel try-umount infrastructure
//! (`XNSU_IOCTL_ADD_TRY_UMOUNT` -> kernel `mount_list` -> per-app unmount on
//! setuid in `kernel/feature/kernel_umount.c`). The user paths are appended to
//! the same `mount_list` the module mounts use, so they are unmounted per-app
//! exactly like module mounts — this is the correct hiding semantics, and is
//! strictly better than a global one-shot detach.
//!
//! The persisted list lives at [`defs::UMOUNT_LIST_PATH`], one entry per line:
//! `<path>` optionally followed by whitespace and numeric umount `<flags>`
//! (defaults to `MNT_DETACH`). Blank lines and `#` comments are ignored.

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;
use std::path::Path;

/// Default umount flags applied to user-configured mount points (`MNT_DETACH`).
const DEFAULT_UMOUNT_FLAGS: u32 = 2; // MNT_DETACH

/// Parse the persisted config into `(path, flags)` pairs.
fn parse_list(content: &str) -> Vec<(String, u32)> {
    let mut entries = Vec::new();
    for line in content.lines() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let mut fields = line.split_whitespace();
        let Some(path) = fields.next() else {
            continue;
        };
        let flags = fields
            .next()
            .and_then(|f| f.parse::<u32>().ok())
            .unwrap_or(DEFAULT_UMOUNT_FLAGS);
        entries.push((path.to_string(), flags));
    }
    entries
}

/// Read the persisted umount list. Returns an empty list when the file is
/// absent or unreadable.
pub fn read_list() -> Vec<(String, u32)> {
    fs::read_to_string(defs::UMOUNT_LIST_PATH)
        .map(|content| parse_list(&content))
        .unwrap_or_default()
}

/// Persist the given paths (one per line) to [`defs::UMOUNT_LIST_PATH`].
fn write_list(paths: &[String]) -> Result<()> {
    if let Some(parent) = Path::new(defs::UMOUNT_LIST_PATH).parent() {
        let _ = fs::create_dir_all(parent);
    }
    let body = paths
        .iter()
        .map(|p| p.trim())
        .filter(|p| !p.is_empty())
        .collect::<Vec<_>>()
        .join("\n");
    fs::write(defs::UMOUNT_LIST_PATH, body)?;
    Ok(())
}

/// Add every persisted mount point to the kernel try-umount list.
///
/// This is add-only and deliberately never wipes the list, so unrelated
/// entries (e.g. module mounts already present in the kernel `mount_list`) are
/// preserved. The kernel `add` is idempotent, so calling this repeatedly is
/// safe. Run at boot after modules are mounted.
pub fn apply_umount_list() {
    for (path, flags) in read_list() {
        if let Err(e) = ksucalls::umount_list_add(&path, flags) {
            warn!("umount_service: add {path} failed: {e}");
        }
    }
}

/// Persist a new mount-point set (from the manager) and reconcile the kernel
/// list: previously persisted entries are removed and the new set added,
/// leaving unrelated (module) entries untouched.
pub fn save_list(paths: &[String]) -> Result<()> {
    for (old_path, _) in read_list() {
        if let Err(e) = ksucalls::umount_list_del(&old_path) {
            warn!("umount_service: del {old_path} failed: {e}");
        }
    }
    write_list(paths)?;
    apply_umount_list();
    Ok(())
}

/// Print the persisted umount list as `<path>\t<flags>` lines.
pub fn print_list() {
    for (path, flags) in read_list() {
        println!("{path}\t{flags}");
    }
}
