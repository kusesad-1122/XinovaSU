//! Kernel-side log suppression.
//!
//! Persists an enable flag and replays it into the kernel at boot. While the
//! feature is enabled the kernel rewrites the entry instruction of two record
//! producers so nothing reaches the audit ring buffer:
//!
//! * `audit_log_start()`  -> returns NULL, so every `audit_log_*` caller bails
//! * `avc_denied()`       -> returns 0, so no "avc: denied" record is emitted
//!
//! Both are best-effort: a symbol the running KMI does not export is skipped
//! with a warning and the other still applies. The original instruction words
//! are saved in the kernel and restored when the feature is switched off, so
//! toggling does not require a reboot.
//!
//! Config ([`defs::LOG_HIDE_CONFIG`]): a single line, "1" or "0".

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;

// XNSU_FEATURE_LOG_HIDE (uapi/feature.h). The master switch rides the generic
// feature IOCTL; there is no per-target list today.
const FEATURE_LOG_HIDE: u32 = 10;

pub fn read_config() -> bool {
    match fs::read_to_string(defs::LOG_HIDE_CONFIG) {
        Ok(content) => content.trim().starts_with('1'),
        Err(_) => false,
    }
}

pub fn write_config(enabled: bool) -> Result<()> {
    if let Some(parent) = std::path::Path::new(defs::LOG_HIDE_CONFIG).parent() {
        let _ = fs::create_dir_all(parent);
    }
    fs::write(defs::LOG_HIDE_CONFIG, if enabled { "1\n" } else { "0\n" })?;
    Ok(())
}

pub fn save_enabled(enabled: bool) -> Result<()> {
    write_config(enabled)?;
    if let Err(e) = ksucalls::set_feature(FEATURE_LOG_HIDE, u64::from(enabled)) {
        warn!("log_hide: set_feature failed: {e}");
        return Err(e.into());
    }
    Ok(())
}

/// Replay the persisted state into the kernel. Called once at boot.
pub fn apply() {
    let enabled = read_config();
    if !enabled {
        return;
    }
    if let Err(e) = ksucalls::set_feature(FEATURE_LOG_HIDE, 1) {
        warn!("log_hide: apply failed: {e}");
    }
}

pub fn status() -> bool {
    read_config()
}
