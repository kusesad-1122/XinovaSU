//! Per-app VPN-detection hiding.
//!
//! Persists an enable flag plus a uid list and replays them into the kernel at
//! boot. While the feature is enabled, the kernel filters VPN-ish network
//! interfaces (tun/tap/ppp/wg/ipsec/utun ...) out of `/sys/class/net` readdir,
//! netlink dumps, `/proc/net/{route,dev,if_inet6}` reads and SIOCGIF* ioctls
//! for the selected app uids (appid-normalised), so those apps cannot detect
//! an active VPN through any native enumeration vector.
//!
//! Config ([`defs::VPN_HIDE_CONFIG`]): line 1 = "1"/"0" (enabled), the
//! remaining lines = target uids (one per line).
//!
//! Optional ports layer ([`defs::VPN_PORTS_CONFIG`]): while enabled, the
//! kernel refuses connect() to 127.0.0.1 / ::1 for the same target apps, so
//! locally running VPN / proxy daemons cannot be found by loopback probing.
//! Shares the target list with the main layer.

use crate::{defs, ksucalls};
use anyhow::Result;
use log::warn;
use std::fs;

// XNSU_FEATURE_* (uapi/feature.h). The master switches are toggled through the
// generic feature IOCTL; the target list rides the dedicated vpn-hide IOCTL.
const FEATURE_VPN_HIDE: u32 = 8;
const FEATURE_VPN_PORTS: u32 = 9;

fn read_config() -> Option<(bool, Vec<u32>)> {
    let content = fs::read_to_string(defs::VPN_HIDE_CONFIG).ok()?;
    let mut lines = content.lines();
    let enabled = lines.next().unwrap_or("0").trim() == "1";
    let uids: Vec<u32> = lines.filter_map(|l| l.trim().parse::<u32>().ok()).collect();
    Some((enabled, uids))
}

fn read_ports_config() -> Option<bool> {
    let content = fs::read_to_string(defs::VPN_PORTS_CONFIG).ok()?;
    Some(content.lines().next().unwrap_or("0").trim() == "1")
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
    // The ports layer shares the target list; its toggle rides the generic
    // feature IOCTL with its own feature id.
    let ports = read_ports_config().unwrap_or(false);
    if let Err(e) = ksucalls::set_feature(FEATURE_VPN_PORTS, u64::from(ports)) {
        warn!("vpn_ports: set feature failed: {e}");
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

/// Persist the ports-layer toggle and apply it immediately (shares the
/// vpn-hide target list, so only the switch needs persisting).
pub fn save_ports(enabled: bool) -> Result<()> {
    fs::write(
        defs::VPN_PORTS_CONFIG,
        if enabled { "1\n" } else { "0\n" },
    )?;
    apply_from_config();
    Ok(())
}
