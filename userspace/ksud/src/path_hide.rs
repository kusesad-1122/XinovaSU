//! Per-app path hiding.
//!
//! Persists the enable flag, filter-system flag, hidden paths and target uids,
//! and replays them into the kernel at boot. The kernel marks the target apps
//! so their openat/newfstatat/faccessat syscalls reach the hooks, and a
//! matching hidden path (exact or directory prefix) returns -ENOENT to them.
//!
//! Config ([`defs::PATH_HIDE_CONFIG`]), one directive per line:
//! `enabled=0|1`, `filter_system=0|1`, `path=<path>`, `uid=<uid>`.

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fmt::Write as _;
use std::fs;

// XNSU_FEATURE_PATH_HIDE (uapi/feature.h); the master switch rides the generic
// feature IOCTL, the paths/uids ride the dedicated path-hide IOCTL.
const FEATURE_PATH_HIDE: u32 = 7;

struct Config {
    enabled: bool,
    filter_system: bool,
    paths: Vec<String>,
    uids: Vec<u32>,
}

fn read_config() -> Option<Config> {
    let content = fs::read_to_string(defs::PATH_HIDE_CONFIG).ok()?;
    let mut cfg = Config {
        enabled: false,
        filter_system: false,
        paths: Vec::new(),
        uids: Vec::new(),
    };
    for line in content.lines() {
        let line = line.trim();
        if let Some(v) = line.strip_prefix("enabled=") {
            cfg.enabled = v == "1";
        } else if let Some(v) = line.strip_prefix("filter_system=") {
            cfg.filter_system = v == "1";
        } else if let Some(v) = line.strip_prefix("path=")
            && !v.is_empty()
        {
            cfg.paths.push(v.to_string());
        } else if let Some(v) = line.strip_prefix("uid=")
            && let Ok(uid) = v.parse::<u32>()
        {
            cfg.uids.push(uid);
        }
    }
    Some(cfg)
}

/// Re-apply the persisted path-hide state to the kernel (used at boot).
pub fn apply_from_config() {
    let Some(cfg) = read_config() else {
        return;
    };
    let _ = ksucalls::path_hide_clear_paths();
    let _ = ksucalls::path_hide_clear_uids();
    for path in &cfg.paths {
        if let Err(e) = ksucalls::path_hide_add_path(path) {
            warn!("path_hide: add path {path} failed: {e}");
        }
    }
    for uid in &cfg.uids {
        if let Err(e) = ksucalls::path_hide_add_uid(*uid) {
            warn!("path_hide: add uid {uid} failed: {e}");
        }
    }
    let _ = ksucalls::path_hide_set_filter_system(cfg.filter_system);
    if let Err(e) = ksucalls::set_feature(FEATURE_PATH_HIDE, u64::from(cfg.enabled)) {
        warn!("path_hide: set feature failed: {e}");
    }
}

/// Persist the full path-hide state and apply it immediately (for the manager).
pub fn save(enabled: bool, filter_system: bool, paths: &[String], uids: &[u32]) -> Result<()> {
    let mut body = String::new();
    let _ = writeln!(body, "enabled={}", u8::from(enabled));
    let _ = writeln!(body, "filter_system={}", u8::from(filter_system));
    for path in paths {
        let path = path.trim();
        if !path.is_empty() {
            let _ = writeln!(body, "path={path}");
        }
    }
    for uid in uids {
        let _ = writeln!(body, "uid={uid}");
    }
    fs::write(defs::PATH_HIDE_CONFIG, body)?;
    apply_from_config();
    Ok(())
}
