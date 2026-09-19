//! CPU spoof — persisted kernel-level hardware-identity redirection.
//!
//! The kernel side (`feature/cpu_spoof.c`) redirects a few hardware-identity
//! files to decoys for *every* reader. This module owns the decoy material and
//! the persisted switch.
//!
//! Material is fetched per device from the internal-tier licensing server,
//! which also enforces the internal allowlist: hardware spoofing is a licensed
//! capability, not a free one. The request carries the same hardware ids the
//! legacy activation script reported, so existing whitelist entries keep
//! matching without any manual re-enrolment.
//!
//! Config ([`defs::CPU_SPOOF_CONFIG`]), one directive per line:
//! `enabled=0|1`, `template=<id>`, `server=<url>`.
//!
//! Decoys live under [`defs::CPU_SPOOF_DIR`] with mode 0644 while the directory
//! is 0755 — see the comment on that constant: the open happens with the
//! *reader's* credentials, so tightening either one breaks non-root reads.
#![allow(clippy::missing_errors_doc)]

use crate::{defs, ksucalls, resetprop};
use anyhow::{Context, Result, bail};
use log::{info, warn};
use std::fmt::Write as _;
use std::fs;
use std::io::{Read, Write};
use std::net::TcpStream;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;
use std::time::Duration;

/// XNSU_FEATURE_CPU_SPOOF (uapi/feature.h).
const FEATURE_CPU_SPOOF: u32 = 9;

/// (source path, decoy file name). Any absolute path works kernel-side; these
/// are the files detection SDKs actually read for CPU/board identity.
const TARGETS: &[(&str, &str)] = &[
    ("/proc/cpuinfo", "cpuinfo"),
    ("/proc/cmdline", "cmdline"),
    ("/proc/bootconfig", "bootconfig"),
];

/// Internal-tier licensing endpoint (the tier that owns the allowlist).
const DEFAULT_SERVER: &str = "http://119.29.88.82:8443";

/// `POST <server>/xnsu/cpu-spoof` — allowlist check + decoy material.
const ENDPOINT: &str = "/xnsu/cpu-spoof";

const HTTP_TIMEOUT: Duration = Duration::from_secs(20);
const DECOY_MODE: u32 = 0o644;
const DECOY_DIR_MODE: u32 = 0o755;

struct Config {
    enabled: bool,
    template: String,
    server: String,
}

/// Material the server hands back for one template.
struct Material {
    cpuinfo: String,
    cmdline: String,
    bootconfig: String,
    /// system.prop-formatted property lines (ro.soc.*, ro.board.platform, ...).
    props: String,
}

fn default_config() -> Config {
    Config {
        enabled: false,
        template: String::new(),
        server: DEFAULT_SERVER.to_string(),
    }
}

fn read_config() -> Config {
    let mut cfg = default_config();
    let Ok(content) = fs::read_to_string(defs::CPU_SPOOF_CONFIG) else {
        return cfg;
    };
    for line in content.lines() {
        let line = line.trim();
        if let Some(v) = line.strip_prefix("enabled=") {
            cfg.enabled = v == "1";
        } else if let Some(v) = line.strip_prefix("template=") {
            cfg.template = v.trim().to_string();
        } else if let Some(v) = line.strip_prefix("server=")
            && !v.trim().is_empty()
        {
            cfg.server = v.trim().to_string();
        }
    }
    cfg
}

fn write_config(enabled: bool, template: &str, server: &str) -> Result<()> {
    let mut body = String::new();
    let _ = writeln!(body, "enabled={}", u8::from(enabled));
    let _ = writeln!(body, "template={template}");
    let _ = writeln!(body, "server={server}");
    fs::write(defs::CPU_SPOOF_CONFIG, body)?;
    Ok(())
}

/// Hardware ids, mirroring the legacy activation script's collect_ids() so
/// existing whitelist entries match unchanged.
fn collect_ids() -> Vec<String> {
    let mut ids: Vec<String> = Vec::new();

    if let Ok(cmdline) = fs::read_to_string("/proc/cmdline") {
        for tok in cmdline.split_whitespace() {
            if let Some(v) = tok
                .strip_prefix("androidboot.serialno=")
                .or_else(|| tok.strip_prefix("oplusboot.serialno="))
                && !v.is_empty()
            {
                ids.push(v.to_string());
            }
        }
    }

    for (dev, prefix) in [("wlan0", "wlan0:"), ("eth0", "eth0:")] {
        if let Ok(mac) = fs::read_to_string(format!("/sys/class/net/{dev}/address")) {
            let mac = mac.trim();
            if !mac.is_empty() && mac != "00:00:00:00:00:00" {
                ids.push(format!("{prefix}{mac}"));
            }
        }
    }

    if let Ok(cpuinfo) = fs::read_to_string("/proc/cpuinfo") {
        for line in cpuinfo.lines() {
            if let Some((key, value)) = line.split_once(':')
                && key.trim().eq_ignore_ascii_case("serial")
            {
                let value = value.trim();
                if !value.is_empty() && value != "0000000000000000" {
                    ids.push(format!("cpu:{value}"));
                }
            }
        }
    }

    ids.sort();
    ids.dedup();
    ids
}

fn kernel_release() -> String {
    fs::read_to_string("/proc/sys/kernel/osrelease")
        .map_or_else(|_| String::new(), |s| s.trim().to_string())
}

fn parse_server(server: &str) -> Result<(String, u16)> {
    let rest = server.strip_prefix("http://").unwrap_or(server);
    let rest = rest.trim_end_matches('/');
    match rest.split_once(':') {
        Some((host, port)) => Ok((
            host.to_string(),
            port.parse().with_context(|| format!("bad port in {server}"))?,
        )),
        None => Ok((rest.to_string(), 80)),
    }
}

/// Minimal cleartext HTTP POST. The licensing tier is plain HTTP (Chinese
/// carrier proxies break TLS pinning anyway), so no HTTP dependency is pulled
/// in for this single call.
fn http_post(server: &str, path: &str, body: &str) -> Result<String> {
    let (host, port) = parse_server(server)?;
    let mut stream = TcpStream::connect((host.as_str(), port))
        .with_context(|| format!("cannot reach {host}:{port}"))?;
    stream.set_read_timeout(Some(HTTP_TIMEOUT)).ok();
    stream.set_write_timeout(Some(HTTP_TIMEOUT)).ok();

    let request = format!(
        "POST {path} HTTP/1.1\r\nHost: {host}:{port}\r\nUser-Agent: ksud\r\n\
         Content-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
        body.len()
    );
    stream.write_all(request.as_bytes())?;

    let mut raw = String::new();
    stream.read_to_string(&mut raw)?;
    let (head, payload) = raw
        .split_once("\r\n\r\n")
        .context("malformed HTTP response")?;

    let status = head.lines().next().unwrap_or_default();
    if !status.contains(" 200") {
        bail!("licensing server refused: {status}");
    }
    Ok(payload.to_string())
}

fn fetch_material(server: &str, template: &str) -> Result<Material> {
    let payload = serde_json::json!({
        "ids": collect_ids(),
        "kernel": kernel_release(),
        "template": template,
    });
    let response = http_post(server, ENDPOINT, &payload.to_string())?;
    let value: serde_json::Value =
        serde_json::from_str(&response).context("licensing server returned invalid JSON")?;

    if value.get("allowed").and_then(serde_json::Value::as_bool) != Some(true) {
        bail!("this device is not on the internal allowlist");
    }

    let get = |key: &str| {
        value
            .get(key)
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default()
            .to_string()
    };
    let material = Material {
        cpuinfo: get("cpuinfo"),
        cmdline: get("cmdline"),
        bootconfig: get("bootconfig"),
        props: get("props"),
    };
    if material.cpuinfo.is_empty() {
        bail!("licensing server returned an empty cpuinfo decoy");
    }
    Ok(material)
}

fn decoy_path(name: &str) -> String {
    format!("{}{name}", defs::CPU_SPOOF_DIR)
}

/// Write decoys and return the (source, decoy) pairs that actually exist.
fn write_decoys(material: &Material) -> Result<Vec<(String, String)>> {
    fs::create_dir_all(defs::CPU_SPOOF_DIR)
        .with_context(|| format!("create {}", defs::CPU_SPOOF_DIR))?;
    // 0755: unprivileged readers must be able to traverse into the directory.
    fs::set_permissions(defs::CPU_SPOOF_DIR, fs::Permissions::from_mode(DECOY_DIR_MODE))?;

    let mut installed = Vec::new();
    for (src, name) in TARGETS {
        let content = match *name {
            "cpuinfo" => &material.cpuinfo,
            "cmdline" => &material.cmdline,
            "bootconfig" => &material.bootconfig,
            _ => continue,
        };
        if content.is_empty() {
            continue;
        }
        let path = decoy_path(name);
        fs::write(&path, content).with_context(|| format!("write {path}"))?;
        // 0644: the kernel redirect opens this under the *reader's* creds.
        fs::set_permissions(&path, fs::Permissions::from_mode(DECOY_MODE))?;
        installed.push(((*src).to_string(), path));
    }
    Ok(installed)
}

fn apply_props(props: &str) -> Result<()> {
    if props.trim().is_empty() {
        return Ok(());
    }
    let path = decoy_path("system.prop");
    fs::write(&path, props).with_context(|| format!("write {path}"))?;
    fs::set_permissions(&path, fs::Permissions::from_mode(DECOY_MODE))?;
    // Reuses the built-in resetprop implementation, so no external binary and
    // no shell are involved.
    resetprop::load_system_prop_file(Path::new(&path))
}

fn install_rules(installed: &[(String, String)]) -> Result<()> {
    ksucalls::cpu_spoof_clear_rules()?;
    for (src, dst) in installed {
        ksucalls::cpu_spoof_add_rule(src, dst)
            .with_context(|| format!("install redirect {src} -> {dst}"))?;
    }
    Ok(())
}

/// Turn hardware spoofing on for `template`, fetching fresh material.
pub fn enable(template: &str) -> Result<()> {
    if template.trim().is_empty() {
        bail!("a CPU template id is required");
    }
    let cfg = read_config();
    let server = cfg.server;

    let material = fetch_material(&server, template)?;
    let installed = write_decoys(&material)?;
    if installed.is_empty() {
        bail!("no decoy could be written");
    }
    apply_props(&material.props)?;

    // Rules first, feature second: the feature toggle installs the kprobe, and
    // it must never be live while the rule table is still empty (that would
    // briefly redirect nothing while reporting "on").
    install_rules(&installed)?;
    ksucalls::set_feature(FEATURE_CPU_SPOOF, 1)
        .context("enable CPU_SPOOF feature in kernel")?;

    write_config(true, template, &server)?;
    info!("cpu_spoof: enabled with template {template}");
    Ok(())
}

/// Turn hardware spoofing off and drop the rules + decoys.
pub fn disable() -> Result<()> {
    ksucalls::set_feature(FEATURE_CPU_SPOOF, 0).context("disable CPU_SPOOF feature")?;
    let _ = ksucalls::cpu_spoof_clear_rules();
    let _ = fs::remove_dir_all(defs::CPU_SPOOF_DIR);

    let cfg = read_config();
    write_config(false, &cfg.template, &cfg.server)?;
    info!("cpu_spoof: disabled");
    Ok(())
}

/// Re-apply the persisted state at boot.
pub fn apply_from_config() {
    let cfg = read_config();

    if !cfg.enabled {
        if let Err(e) = ksucalls::set_feature(FEATURE_CPU_SPOOF, 0) {
            warn!("cpu_spoof: disable failed: {e}");
        }
        return;
    }

    // Decoys survive on disk across reboots, so no network round-trip is needed
    // to come back up; the allowlist was already checked when it was enabled.
    let mut installed = Vec::new();
    for (src, name) in TARGETS {
        let path = decoy_path(name);
        if Path::new(&path).exists() {
            installed.push(((*src).to_string(), path));
        }
    }
    if installed.is_empty() {
        warn!("cpu_spoof: enabled in config but no decoys on disk; ignoring");
        return;
    }
    if let Err(e) = install_rules(&installed) {
        warn!("cpu_spoof: reinstall rules failed: {e}");
        return;
    }
    if let Err(e) = ksucalls::set_feature(FEATURE_CPU_SPOOF, 1) {
        warn!("cpu_spoof: enable failed: {e}");
    }
}
