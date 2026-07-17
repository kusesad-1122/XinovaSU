//! Kernel uname (utsname) spoofing.
//!
//! Persists a spoofed `uname -r` (release) and `uname -v` (version) and replays
//! them into the kernel at boot via `XNSU_IOCTL_SET_UTS_SPOOF`. The kernel
//! rewrites the global utsname, so uname(2), /proc/version and
//! /proc/sys/kernel/osrelease all report the spoofed values.
//!
//! Config file ([`defs::UTS_SPOOF_CONFIG`]): line 1 = release, line 2 = version.
//! Presence of the file means spoofing is enabled.

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;

/// Read `(release, version)` from the config, or `None` when disabled.
pub fn read_config() -> Option<(String, String)> {
    let content = fs::read_to_string(defs::UTS_SPOOF_CONFIG).ok()?;
    let mut lines = content.lines();
    let release = lines.next().unwrap_or_default().trim().to_string();
    let version = lines.next().unwrap_or_default().trim().to_string();
    if release.is_empty() && version.is_empty() {
        return None;
    }
    Some((release, version))
}

/// Re-apply the persisted spoof to the kernel (used at boot).
pub fn apply_from_config() {
    if let Some((release, version)) = read_config()
        && let Err(e) = ksucalls::uts_spoof_apply(1, &release, &version)
    {
        warn!("kernel_spoof: apply failed: {e}");
    }
}

/// Persist a spoof and apply it immediately (used by the manager).
pub fn save(release: &str, version: &str) -> Result<()> {
    fs::write(defs::UTS_SPOOF_CONFIG, format!("{release}\n{version}\n"))?;
    ksucalls::uts_spoof_apply(1, release, version)?;
    Ok(())
}

/// Remove the persisted spoof and restore the original uname.
pub fn clear() -> Result<()> {
    let _ = fs::remove_file(defs::UTS_SPOOF_CONFIG);
    ksucalls::uts_spoof_apply(0, "", "")?;
    Ok(())
}
