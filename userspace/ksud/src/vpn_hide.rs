//! Per-app VPN-detection hiding.
//!
//! Persists an enable flag plus a uid list and replays them into the kernel at
//! boot. While the feature is enabled, the kernel filters VPN-ish network
//! interfaces (tun/tap/ppp/wg/ipsec/utun ...) out of `/sys/class/net` readdir
//! for the selected app uids (appid-normalised), so those apps cannot detect an
//! active VPN by enumerating interfaces.
//!
//! Config ([`defs::VPN_HIDE_CONFIG`]): line 1 = "1"/"0" (enabled), the
//! remaining lines = target uids (one per line).

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;

// XNSU_FEATURE_VPN_HIDE (uapi/feature.h). The master switch is toggled through
// the generic feature IOCTL; the target list rides the dedicated vpn-hide IOCTL.
const FEATURE_VPN_HIDE: u32 = 8;

fn read_config() -> Option<(bool, Vec<u32>)> {
    let content = fs::read_to_string(defs::VPN_HIDE_CONFIG).ok()?;
    let mut lines = content.lines();
    let enabled = lines.next().unwrap_or("0").trim() == "1";
    let uids: Vec<u32> = lines.filter_map(|l| l.trim().parse::<u32>().ok()).collect();
    Some((enabled, uids))
}

/// Re-apply the persisted vpn-hide state to the kernel (used at boot).
pub fn apply_from_config() {
    let Some((enabled, uids)) = read_config() else {
        return;
    };
    let _ = ksucalls::vpn_hide_clear();
    for uid in &uids {
        if let Err(e) = ksucalls::vpn_hide_add(*uid) {
            warn!("vpn_hide: add {uid} failed: {e}");
        }
    }
    if let Err(e) = ksucalls::set_feature(FEATURE_VPN_HIDE, u64::from(enabled)) {
        warn!("vpn_hide: set feature failed: {e}");
    }
}

/// Persist the full vpn-hide state (enabled + uids) and apply it immediately.
pub fn save(enabled: bool, uids: &[u32]) -> Result<()> {
    let mut body = String::from(if enabled { "1\n" } else { "0\n" });
    for uid in uids {
        body.push_str(&uid.to_string());
        body.push('\n');
    }
    fs::write(defs::VPN_HIDE_CONFIG, body)?;
    apply_from_config();
    Ok(())
}
