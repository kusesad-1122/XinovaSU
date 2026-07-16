//! Bootloader / verified-boot state hiding (FolkPatch-style "hide service").
//!
//! Rewrites a curated set of `ro.boot.*` / verified-boot system properties to
//! the values a locked, verified, production device reports, so naive software
//! checks that read these props see a "locked / green / production" device.
//! Only properties that already exist are modified (never created), matching
//! FolkPatch's behaviour and avoiding fingerprintable phantom props.
//!
//! Limitations: this only rewrites the RAM-backed `__properties__` area
//! (non-persistent, reverts on reboot); it does NOT touch vbmeta, dm-verity,
//! the TEE bootloader flag or hardware keystore attestation, so hardware-backed
//! checks (Play Integrity STRONG, key attestation) still observe the real
//! unlocked state.

use crate::defs;
use anyhow::{Context, Result};
use log::{info, warn};
use prop_rs_android::resetprop::ResetProp;
use prop_rs_android::sys_prop;
use std::path::Path;

const fn resetprop() -> ResetProp {
    ResetProp {
        skip_svc: true,
        persistent: false,
        persist_only: false,
        verbose: false,
        show_context: false,
    }
}

/// Properties rewritten to their "locked / verified / production" values.
const PATCH_LIST: &[(&str, &str)] = &[
    ("ro.boot.vbmeta.device_state", "locked"),
    ("ro.boot.verifiedbootstate", "green"),
    ("ro.boot.flash.locked", "1"),
    ("ro.boot.veritymode", "enforcing"),
    ("vendor.boot.vbmeta.device_state", "locked"),
    ("vendor.boot.verifiedbootstate", "green"),
    ("ro.boot.vbmeta.invalidate_on_error", "yes"),
    ("ro.boot.vbmeta.avb_version", "1.0"),
    ("ro.boot.vbmeta.hash_alg", "sha256"),
    ("ro.boot.vbmeta.size", "4096"),
    ("ro.boot.warranty_bit", "0"),
    ("ro.warranty_bit", "0"),
    ("ro.vendor.boot.warranty_bit", "0"),
    ("ro.vendor.warranty_bit", "0"),
    ("sys.oem_unlock_allowed", "0"),
    ("ro.build.type", "user"),
    ("ro.build.tags", "release-keys"),
    ("ro.secureboot.lockstate", "locked"),
    ("ro.debuggable", "0"),
    ("ro.force.debuggable", "0"),
    ("ro.secure", "1"),
    ("ro.adb.secure", "1"),
    ("ro.boot.realmebootstate", "green"),
    ("ro.boot.realme.lockstate", "1"),
];

/// Boot-mode properties: if they report `recovery`, normalise to `unknown`.
const BOOT_KEYS: &[&str] = &["ro.bootmode", "ro.boot.bootmode", "vendor.boot.bootmode"];

/// Whether the hide service is enabled (marker file present).
pub fn is_enabled() -> bool {
    Path::new(defs::BL_HIDE_ENABLE_FILE).exists()
}

/// Rewrite the bootloader / verified-boot properties in place. Only properties
/// that currently exist are touched; missing ones are left absent.
pub fn run_bl_hide() -> Result<()> {
    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();

    let mut patched = 0u32;
    for &(key, value) in PATCH_LIST {
        if rp.get(key).is_none() {
            continue; // never create props the device doesn't already have
        }
        if let Err(e) = rp.set(key, value) {
            warn!("bl_hide: set {key}={value} failed: {e}");
        } else {
            patched += 1;
        }
    }

    for &key in BOOT_KEYS {
        if let Some(val) = rp.get(key)
            && val.contains("recovery")
        {
            if let Err(e) = rp.set(key, "unknown") {
                warn!("bl_hide: set {key}=unknown failed: {e}");
            } else {
                patched += 1;
            }
        }
    }

    info!("bl_hide: patched {patched} properties");
    Ok(())
}
