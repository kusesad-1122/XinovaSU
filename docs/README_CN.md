# XinovaSU

一个基于 Linux 内核的 Android Root 方案，基于 KernelSU。

[![许可证：GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](/LICENSE)

XinovaSU 在 Linux 内核层授予和管理 Root 权限，而非依赖用户空间实现，
因此能够提供更细粒度的控制与更强的隐藏能力。它还提供面向隐藏与反检测的
内核级扩展功能。

## 功能

- 内核级 su 与按应用授权（白名单、每应用 SELinux 域）
- 无系统模块系统与 WebUI，兼容主流 KernelSU 模块生态
- 开机自动挂载与按应用 umount 控制
- 支持 Android GKI KMI：android12-5.10 至 android16-6.12（arm64-v8a、x86_64）

## XinovaSU 专属功能（设置 → Functions）

- **Hide Service**：针对朴素的属性检测，隐藏 Bootloader 解锁与 verified-boot 状态
- **Umount Service**：开机自动为选定应用卸载指定挂载点
- **Kernel Spoof**：伪装 uname 版本与构建时间（uname、/proc/version、osrelease）
- **Path Hide**：在内核层对选定应用隐藏文件和目录（ENOENT 与目录项过滤）
- **Network Isolation**：在内核层阻断选定应用的网络访问

## 使用

- 安装与构建方式请参阅文档。

## 许可证与致谢

XinovaSU 遵循 GPL，基于 tiann 的 [KernelSU](https://github.com/tiann/KernelSU)。
本仓库保留原始 KernelSU 的版权声明与许可证声明。
