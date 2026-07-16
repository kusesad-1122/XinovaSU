# XinovaSU

一个基于 Linux 内核的 Android Root 方案，基于 KernelSU。

[![许可证：GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](/LICENSE)

XinovaSU 在 Linux 内核层授予和管理 Root 权限，而不是在用户空间实现，
从而提供更隐蔽、更细粒度的控制。它还提供面向隐藏和反检测的内核级功能。

## 功能

- 内核级 `su` 与按应用 Root 授权（白名单、每应用 SELinux 域）
- Systemless 模块系统与 WebUI，兼容主流 KernelSU 模块生态
- 开机自动挂载与按应用卸载控制
- 支持 Android GKI KMI：android12-5.10 至 android16-6.12（arm64-v8a、x86_64）

## XinovaSU 专属功能（设置 → 功能）

- **隐藏服务** — 针对朴素的属性检查隐藏 Bootloader 解锁与 verified-boot 状态
- **卸载服务** — 在开机时按应用自动卸载选定挂载点
- **内核伪装** — 伪装 uname 版本、构建时间、`/proc/version` 与 osrelease
- **路径隐藏** — 在内核层对选定应用隐藏文件/目录（ENOENT 与目录列表过滤）
- **网络隔离** — 在内核层阻断选定应用的网络访问

## 使用

- 安装与构建方式请参阅文档。

## 许可证与致谢

GPL。XinovaSU 基于 tiann 的 [KernelSU](https://github.com/tiann/KernelSU)，遵循 GPL，
并在本仓库中保留原始 KernelSU 的版权与许可证声明。
