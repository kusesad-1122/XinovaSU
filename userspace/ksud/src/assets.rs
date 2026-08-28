use anyhow::Result;
use rust_embed::RustEmbed;

#[cfg(target_os = "android")]
mod android {
    use crate::assets::Asset;
    use crate::defs::BINARY_DIR;
    use crate::utils::ensure_binary;
    use const_format::concatcp;

    pub const RESETPROP_PATH: &str = concatcp!(BINARY_DIR, "resetprop");
    pub const BUSYBOX_PATH: &str = concatcp!(BINARY_DIR, "busybox");
    pub const BOOTCTL_PATH: &str = concatcp!(BINARY_DIR, "bootctl");

    // KernelSU userspace-compat symlinks. ZygiskNext (>= 1.2.2) and various
    // modules detect the KernelSU *userspace* install by the existence of
    // /data/adb/ksud (and expect `ksud` on the module PATH). We renamed the
    // daemon to ksud, so expose ksud -> ksud links. Both live under
    // /data/adb (mode 0700, root-only), so they are invisible to non-root
    // detectors and cost nothing in terms of hiding.
    const KSUD_COMPAT_LINK: &str = "/data/adb/ksud";
    const KSUD_BIN_COMPAT_LINK: &str = concatcp!(BINARY_DIR, "ksud");

    pub fn ensure_binaries(ignore_if_exist: bool) -> anyhow::Result<()> {
        for file in Asset::iter() {
            if file == "ksuinit.bin" || file.ends_with(".ko") {
                // don't extract ksuinit and kernel modules
                continue;
            }
            let asset =
                Asset::get(&file).ok_or_else(|| anyhow::anyhow!("asset not found: {file}"))?;
            ensure_binary(format!("{BINARY_DIR}{file}"), &asset.data, ignore_if_exist)?;
        }

        // Create resetprop -> ksud symlink (resetprop is now built into ksud)
        let resetprop_link = RESETPROP_PATH;
        let _ = std::fs::remove_file(resetprop_link);
        std::os::unix::fs::symlink("/data/adb/ksud", resetprop_link)?;

        // KernelSU userspace-compat symlinks (ksud -> ksud). Best-effort: never
        // fail boot over them, and self-heal on every post-fs-data run.
        for link in [KSUD_COMPAT_LINK, KSUD_BIN_COMPAT_LINK] {
            let _ = std::fs::remove_file(link);
            if let Err(e) = std::os::unix::fs::symlink("/data/adb/ksud", link) {
                log::warn!("failed to create ksud compat symlink {link}: {e}");
            }
        }

        Ok(())
    }
}

#[cfg(target_os = "android")]
pub use android::*;

#[cfg(all(target_arch = "x86_64", target_os = "android"))]
#[derive(RustEmbed)]
#[folder = "bin/x86_64"]
struct Asset;

// IF NOT x86_64 ANDROID, ie. macos, linux, windows, always use aarch64
#[cfg(not(all(target_arch = "x86_64", target_os = "android")))]
#[derive(RustEmbed)]
#[folder = "bin/aarch64"]
struct Asset;

#[allow(unused)]
pub fn get_asset_data(name: &str) -> Result<std::borrow::Cow<'static, [u8]>> {
    let asset = Asset::get(name).ok_or_else(|| anyhow::anyhow!("asset not found: {name}"))?;
    Ok(asset.data)
}

pub fn get_asset(name: &str) -> Result<Box<dyn AsRef<[u8]>>> {
    let asset = Asset::get(name).ok_or_else(|| anyhow::anyhow!("asset not found: {name}"))?;
    Ok(Box::new(asset.data))
}

pub fn list_supported_kmi() -> std::vec::Vec<std::string::String> {
    let mut list = Vec::new();
    for file in Asset::iter() {
        // kmi_name = "xxx_kernelsu.ko"
        if let Some(kmi) = file.strip_suffix("_kernelsu.ko") {
            list.push(kmi.to_string());
        }
    }
    list
}
