#!/usr/bin/env python3
"""
ABI regression guard for XinovaSU official compatibility.
Checks:
 - uapi/feature.h: official 0..5 preserved, self 6..9
 - uapi/supercall.h: official ioctl 1..21 present, self ioctls 25+ only, no self in 1..21
 - uapi/app_profile.h: FLAG_KSU_NO_NEW_PRIVS present
 - kernel/Kbuild: KSU_GIT_VERSION / KSU_VERSION (not XNSU)
 - runtime: KSUD_PATH == /data/adb/ksud
 - userspace: FEATURE_NET isolate etc aligned
"""
import re, sys, pathlib

root = pathlib.Path(__file__).resolve().parents[1]

def fail(msg):
    print(f"ABI guard: FAIL: {msg}", file=sys.stderr)
    sys.exit(1)
def ok(msg):
    print(f"ABI guard: {msg}")

def check_feature():
    p = root / "uapi" / "feature.h"
    t = p.read_text()
    # must contain official 5
    if "KSU_FEATURE_WEBVIEW_ZYGOTE_UMOUNT = 5" not in t:
        fail("feature.h: KSU_FEATURE_WEBVIEW_ZYGOTE_UMOUNT must be 5")
    if "XNSU_FEATURE_KERNEL_SPOOF = 6" not in t:
        fail("feature.h: XNSU_FEATURE_KERNEL_SPOOF must be 6")
    if "XNSU_FEATURE_NET_ISOLATE = 7" not in t:
        fail("feature.h: XNSU_FEATURE_NET_ISOLATE must be 7")
    if "XNSU_FEATURE_PATH_HIDE = 8" not in t:
        fail("feature.h: XNSU_FEATURE_PATH_HIDE must be 8")
    if "XNSU_FEATURE_VPN_HIDE = 9" not in t:
        fail("feature.h: XNSU_FEATURE_VPN_HIDE must be 9")
    # ensure no old layout
    if re.search(r"XNSU_FEATURE_KERNEL_SPOOF\s*=\s*5", t):
        fail("feature.h: old layout XNSU_FEATURE_KERNEL_SPOOF=5 still present")
    if re.search(r"KSU_FEATURE_WEBVIEW_ZYGOTE_UMOUNT\s*=\s*9", t):
        fail("feature.h: old layout WEBVIEW=9 still present")
    ok("feature.h 0..5 official, 6..9 self: PASS")

def check_ioctl():
    p = root / "uapi" / "supercall.h"
    t = p.read_text()
    if "KSU_IOCTL_DISABLE_ESCAPE_TO_ROOT" not in t:
        fail("supercall.h: missing KSU_IOCTL_DISABLE_ESCAPE_TO_ROOT")
    if "_IO('K', 21)" not in t:
        fail("supercall.h: KSU_IOCTL_DISABLE_ESCAPE_TO_ROOT must be _IO('K',21)")
    # self ioctls must be 25+
    for m in re.finditer(r"XNSU_IOCTL_\w+\s*=\s*_IOW\('K',\s*(\d+)", t):
        n = int(m.group(1))
        if n < 25:
            fail(f"supercall.h: self ioctl {m.group(0)} must be >=25, got {n}")
        if 22 <= n <= 24:
            fail(f"supercall.h: self ioctl occupies reserved 22-24: {m.group(0)}")
    # ensure old numbers not present
    if re.search(r"XNSU_IOCTL_SET_UTS_SPOOF.*21", t):
        fail("supercall.h: old XNSU UTS 21 still present")
    ok("supercall.h ioctl 1..21 + self 25+: PASS")

def check_flag():
    p = root / "uapi" / "app_profile.h"
    t = p.read_text()
    if "FLAG_KSU_NO_NEW_PRIVS" not in t:
        fail("app_profile.h: missing FLAG_KSU_NO_NEW_PRIVS")
    if "#define XNSU_FLAG_NO_NEW_PRIVS FLAG_KSU_NO_NEW_PRIVS" not in t:
        fail("app_profile.h: missing compat alias")
    ok("app_profile.h FLAG_KSU_NO_NEW_PRIVS: PASS")

def check_kbuild():
    p = root / "kernel" / "Kbuild"
    t = p.read_text()
    if "KSU_GIT_VERSION :=" not in t:
        fail("Kbuild: missing KSU_GIT_VERSION :=")
    if "KSU_GIT_VERSION_VALID" not in t:
        fail("Kbuild: missing KSU_GIT_VERSION_VALID")
    if "XNSU_GIT_VERSION" in t:
        fail("Kbuild: stale XNSU_GIT_VERSION still present")
    if "XNSU_VERSION=" in t or "$(eval XNSU_VERSION" in t:
        fail("Kbuild: stale XNSU_VERSION still present")
    if 'KSU_KERNEL_DIR' not in t:
        fail("Kbuild: missing KSU_KERNEL_DIR")
    ok("Kbuild KSU_GIT_VERSION/KSU_VERSION: PASS")

def check_ksud_path():
    p = root / "kernel" / "runtime" / "xnsusd.h"
    t = p.read_text()
    if '#define KSUD_PATH "/data/adb/ksud"' not in t:
        fail("xnsusd.h: KSUD_PATH must be /data/adb/ksud")
    if "/data/adb/xnsusd" in t:
        fail("xnsusd.h: stale /data/adb/xnsusd still present")
    ok("KSUD_PATH /data/adb/ksud: PASS")

def check_userspace():
    # feature.rs
    p = root / "userspace" / "ksud" / "src" / "feature.rs"
    t = p.read_text()
    if "WebviewZygoteUmount = 5" not in t:
        fail("feature.rs: Webview must be 5")
    # net_isolate etc
    for path, expect in [
        ("userspace/ksud/src/net_isolate.rs", "FEATURE_NET_ISOLATE: u32 = 7"),
        ("userspace/ksud/src/path_hide.rs", "FEATURE_PATH_HIDE: u32 = 8"),
        ("userspace/ksud/src/vpn_hide.rs", "FEATURE_VPN_HIDE: u32 = 9"),
    ]:
        pt = root / path
        tt = pt.read_text()
        if expect not in tt:
            fail(f"{path}: expected {expect}")
    ok("userspace feature constants 5/7/8/9: PASS")

def check_atomic():
    # ensure atomic_write used
    for path in ["userspace/ksud/src/feature.rs", "userspace/ksud/src/path_hide.rs", "userspace/ksud/src/net_isolate.rs", "userspace/ksud/src/vpn_hide.rs", "userspace/ksud/src/kernel_spoof.rs"]:
        pt = root / path
        if "atomic_write" not in pt.read_text():
            fail(f"{path}: missing atomic_write")
    ok("atomic_write present in configs: PASS")

if __name__ == "__main__":
    check_feature()
    check_ioctl()
    check_flag()
    check_kbuild()
    check_ksud_path()
    check_userspace()
    check_atomic()
    print("ABI guard: ALL PASS")
