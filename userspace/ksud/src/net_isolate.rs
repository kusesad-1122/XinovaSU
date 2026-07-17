//! Per-app network isolation.
//!
//! Persists an enable flag plus a uid blocklist and replays them into the
//! kernel at boot. The kernel LSM-hooks `socket_connect` and returns `-EPERM`
//! for blocked app uids (appid-normalised) while the feature is enabled.
//!
//! Config ([`defs::NET_ISOLATE_CONFIG`]): line 1 = "1"/"0" (enabled), the
//! remaining lines = blocked uids (one per line).

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;

// XNSU_FEATURE_NET_ISOLATE (uapi/feature.h). The master switch is toggled
// through the generic feature IOCTL; the blocklist rides the dedicated
// net-isolate IOCTL.
const FEATURE_NET_ISOLATE: u32 = 6;

fn read_config() -> Option<(bool, Vec<u32>)> {
    let content = fs::read_to_string(defs::NET_ISOLATE_CONFIG).ok()?;
    let mut lines = content.lines();
    let enabled = lines.next().unwrap_or("0").trim() == "1";
    let uids: Vec<u32> = lines.filter_map(|l| l.trim().parse::<u32>().ok()).collect();
    Some((enabled, uids))
}

/// Re-apply the persisted isolation state to the kernel (used at boot).
pub fn apply_from_config() {
    let Some((enabled, uids)) = read_config() else {
        return;
    };
    let _ = ksucalls::net_isolate_clear();
    for uid in &uids {
        if let Err(e) = ksucalls::net_isolate_add(*uid) {
            warn!("net_isolate: add {uid} failed: {e}");
        }
    }
    if let Err(e) = ksucalls::set_feature(FEATURE_NET_ISOLATE, u64::from(enabled)) {
        warn!("net_isolate: set feature failed: {e}");
    }
}

/// Persist the full isolation state (enabled + uids) and apply it immediately.
pub fn save(enabled: bool, uids: &[u32]) -> Result<()> {
    let mut body = String::from(if enabled { "1\n" } else { "0\n" });
    for uid in uids {
        body.push_str(&uid.to_string());
        body.push('\n');
    }
    fs::write(defs::NET_ISOLATE_CONFIG, body)?;
    apply_from_config();
    Ok(())
}
