# XinovaSU

A kernel-based root solution for Android — based on KernelSU.

[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](/LICENSE)

XinovaSU grants and manages root at the Linux kernel level rather than in
userspace, making it stealthier and more granular. It ships extra kernel-level
features focused on hiding and anti-detection.

## Features

- Kernel-level `su` and per-app root authorization (allowlist, per-app SELinux domain)
- Systemless module system + WebUI, compatible with the mainstream KernelSU module ecosystem
- Boot-time auto mount / per-app umount control
- Supports Android GKI KMIs android12-5.10 through android16-6.12 (arm64-v8a, x86_64)

## XinovaSU-exclusive features (Settings → Functions)

- **Hide Service** — masks Bootloader-unlock / verified-boot state against naive property checks
- **Umount Service** — auto-unmount chosen mount points at boot, per-app
- **Kernel Spoof** — spoof uname version / build time (uname, /proc/version, osrelease) via `XNSU_IOCTL_SET_UTS_SPOOF` (25)
- **Path Hide** — hide files/dirs from selected apps at the kernel level (ENOENT + directory-listing filtering, supports `../`/`./` and sdcard alias canonicalization, optional `filter_system`)
- **Network Isolation** — block chosen apps' *new* IP `connect()` at LSM level (`AF_INET`/`AF_INET6`)
- **VPN Hide** — hide VPN interfaces (`tun`/`tap`/`ppp`/`wg`/`ipsec`/`utun`/`ccmni`) from `getdents`/`netlink`/`getifaddrs`/`SIOCGIFCONF` and direct `/sys/class/net/<vpn>` stat; framework `ConnectivityManager` hiding via Zygisk companion (`zygisk-vpnhide`)

## Zygisk VPN Hide

`zygisk-vpnhide` is the framework-layer companion to `kernel/feature/vpn_hide`. The kernel covers native enumeration; the Zygisk module covers `ConnectivityManager`/`NetworkCapabilities` (via LSPlant when vendored) and native `ioctl(SIOCGIFCONF)`. See `zygisk-vpnhide/src/main.cpp` and `zygisk-vpnhide/src/lsplant.hpp` (stub, replace with real LSPlant for full ART hooks).

## Usage

- Installation / How to build: see the docs.

## License & Credits

GPL. XinovaSU is based on [KernelSU](https://github.com/tiann/KernelSU) by tiann.
The original KernelSU copyright and license notices are retained in this repository.
