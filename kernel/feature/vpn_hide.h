#ifndef __XNSU_H_VPN_HIDE
#define __XNSU_H_VPN_HIDE

#include <linux/types.h>

// Per-app "hide VPN from detection" -- kernel layer, native enumeration vectors.
//
// For a selected app the kernel hides VPN-ish interfaces (tun, tap, ppp, wg ...)
// from two native enumeration paths, so a "is there a tun0" check comes up empty:
//   - getdents on /sys/class/net (the sysfs directory listing), and
//   - netlink RTM_GETLINK/GETADDR dumps (what getifaddrs and
//     NetworkInterface.getNetworkInterfaces actually use) via recvmsg/recvfrom.
// The master switch is the XNSU_FEATURE_VPN_HIDE toggle; the per-app target list
// is managed here (appid-normalised).
//
// NOT covered (needs an in-process / Zygisk hook, out of kernel scope):
//   - the ConnectivityManager / VpnService framework APIs, whose answers come
//     from system_server rather than the app's own syscalls.

int xnsu_vpn_hide_add_uid(u32 uid);
int xnsu_vpn_hide_remove_uid(u32 uid);
void xnsu_vpn_hide_clear_uids(void);

// True if uid's appid is a target (used to keep the app's tracepoint mark so its
// getdents syscalls reach the hook).
bool xnsu_vpn_hide_is_target(uid_t uid);

// Cheap gate: current task is a target with the feature on. Checked before the
// (heavier) getdents filter.
bool xnsu_vpn_hide_should_filter_dents(void);

// Same gate for the netlink recv path (recvmsg/recvfrom).
bool xnsu_vpn_hide_should_filter_netlink(void);

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

void xnsu_vpn_hide_init(void);
void xnsu_vpn_hide_exit(void);

#endif // __XNSU_H_VPN_HIDE
