#ifndef __XNSU_H_VPN_HIDE
#define __XNSU_H_VPN_HIDE

#include <linux/types.h>

// Per-app "hide VPN from detection" -- kernel layer, native enumeration vectors.
//
// For a selected app the kernel hides VPN-ish interfaces (tun, tap, ppp, wg ...)
// from four native enumeration paths, so a "is there a tun0" check comes up empty:
//   - getdents on /sys/class/net (the sysfs directory listing)
//   - netlink RTM_GETLINK/GETADDR dumps (what getifaddrs and
//     NetworkInterface.getNetworkInterfaces actually use) via recvmsg/recvfrom
//   - reads of /proc/net/{route,dev,if_inet6} (line-wise filtering)
//   - ioctl SIOCGIF* queries (SIOCGIFCONF entries compacted; SIOCGIFNAME
//     resolving a tunnel reports ENODEV; name-input queries report ENODEV)
//   - SO_BINDTODEVICE naming a tunnel is denied (EPERM) -- mitigation for the
//     unprivileged VPN-exit-IP leak tracked as Google issue 516559265.
// The master switch is the XNSU_FEATURE_VPN_HIDE toggle; the per-app target
// list is managed here (appid-normalised).
//
// NOT covered (needs an in-process / Zygisk hook, out of kernel scope):
//   - the ConnectivityManager / VpnService framework APIs, whose answers come
//     from system_server rather than the app's own syscalls.
//
// Optional ports layer (XNSU_FEATURE_VPN_PORTS): while on, target apps'
// connect() to 127.0.0.1 / ::1 fails with ECONNREFUSED so locally running
// VPN / proxy daemons cannot be found by loopback port probing.

int xnsu_vpn_hide_add_uid(u32 uid);
int xnsu_vpn_hide_remove_uid(u32 uid);
void xnsu_vpn_hide_clear_uids(void);

// True if uid's appid is a target (used to keep the app's tracepoint mark so
// its syscalls reach the hooks). Alive while either vpn-hide layer is on.
bool xnsu_vpn_hide_is_target(uid_t uid);

// Cheap gates: the respective feature is on, current task is not init, and
// current uid is a target. Checked before the (heavier) filters.
bool xnsu_vpn_hide_should_filter_dents(void);
bool xnsu_vpn_hide_should_filter_netlink(void);
bool xnsu_vpn_hide_should_filter_read(void);
bool xnsu_vpn_hide_should_filter_ioctl(void);
bool xnsu_vpn_hide_should_filter_sockopt(void);
bool xnsu_vpn_ports_should_block_connect(void);

// Filter a getdents64 result buffer in place when the directory is /sys/class/net
// and the caller is a target: drop VPN-ish interface entries. Returns the new
// byte count (<= total); returns total unchanged on any error / not applicable.
long xnsu_vpn_hide_filter_getdents64(unsigned int fd, void __user *dirp, long total);

// Filter a netlink RTM_GETLINK/GETADDR reply in place when fd is an AF_NETLINK
// socket: drop whole messages describing VPN-ish interfaces. Returns the new
// byte count (<= total); returns total unchanged on any error / not applicable.
// The recvmsg variant only rewrites single-iovec replies.
long xnsu_vpn_hide_filter_netlink_recvfrom(unsigned int fd, void __user *ubuf, long total, unsigned long buflen);
long xnsu_vpn_hide_filter_netlink_recvmsg(unsigned int fd, void __user *msg_user, long total);

// Filter a read() result when the fd is /proc/net/{route,dev,if_inet6} and the
// caller is a target: drop lines naming a VPN-ish interface.
long xnsu_vpn_hide_filter_read(unsigned int fd, void __user *ubuf, long total);

// ioctl: pre-block name-input SIOCGIF* queries naming a tunnel (ENODEV, the
// real syscall is skipped); post-filter SIOCGIFNAME and SIOCGIFCONF results.
bool xnsu_vpn_hide_ioctl_pre(unsigned int cmd, void __user *arg);
long xnsu_vpn_hide_ioctl_post(unsigned int cmd, void __user *arg, long ret);

// setsockopt: deny SO_BINDTODEVICE naming a tunnel (-EPERM short-circuit).
int xnsu_vpn_hide_filter_setsockopt(unsigned int fd, int level, int optname,
                                    void __user *optval, int optlen);

// connect: deny loopback targets for target apps while the ports layer is on
// (-ECONNREFUSED short-circuit).
int xnsu_vpn_ports_filter_connect(void __user *addr_user, int addrlen);

void xnsu_vpn_hide_init(void);
void xnsu_vpn_hide_exit(void);

#endif // __XNSU_H_VPN_HIDE
