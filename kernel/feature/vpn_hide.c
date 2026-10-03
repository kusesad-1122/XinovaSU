#include <linux/cred.h>
#include <linux/dcache.h>
#include <linux/err.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/if.h>
#include <linux/if_addr.h>
#include <linux/if_link.h>
#include <linux/in.h>
#include <linux/in6.h>
#include <linux/kernel.h>
#include <linux/netlink.h>
#include <linux/printk.h>
#include <linux/rtnetlink.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/socket.h>
#include <linux/sockios.h>
#include <linux/spinlock.h>
#include <linux/static_key.h>
#include <linux/stddef.h>
#include <linux/string.h>
#include <linux/types.h>
#include <linux/uaccess.h>
#include <linux/uio.h>

#include "feature/vpn_hide.h"
#include "hook/tp_marker.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep

#define XNSU_VH_APPID(uid) ((uid) % 100000)
#define XNSU_VH_APPID_MAX 100000
#define XNSU_VH_PATH_LEN 256
#define XNSU_VH_DENTS_MAX (256 * 1024)
// Upper bound on a netlink dump / proc read we will rewrite; larger replies
// pass through untouched.
#define XNSU_VH_BUF_MAX (256 * 1024)

// The directory apps read to enumerate interfaces ("is there a tun0?").
#define XNSU_VH_NET_DIR "/sys/class/net"

// Interface-name prefixes treated as VPN-ish. These are effectively always VPN
// tunnels; real transports (wlan/eth/rmnet/lo) are never matched.
static const char *const vh_iface_prefixes[] = {
    "tun", "tap", "ppp", "wg", "ipsec", "utun",
};

struct xnsu_vh_dirent64 {
    u64 d_ino;
    s64 d_off;
    unsigned short d_reclen;
    unsigned char d_type;
    char d_name[];
};

// Master switch: the XNSU_FEATURE_VPN_HIDE toggle. Keeps the marked hot getdents
// path near-free while the feature is off.
static DEFINE_STATIC_KEY_FALSE(xnsu_vpn_hide);
// Optional loopback-port blocking (XNSU_FEATURE_VPN_PORTS): while on, target
// apps' connect() to 127.0.0.1 / ::1 fails with ECONNREFUSED, so local proxy
// daemons cannot be discovered by port probing. Aligned with the upstream
// vpnhide "ports" module.
static DEFINE_STATIC_KEY_FALSE(xnsu_vpn_ports);
static DEFINE_SPINLOCK(vh_lock);
// appid-space bitmap (appid = uid % 100000): O(1) lockless is_target on the
// hot read/ioctl/connect paths, replacing the previous array+count linear
// scan that took a spinlock per call. Writers still serialize on vh_lock.
static DECLARE_BITMAP(vh_appids, XNSU_VH_APPID_MAX);

int xnsu_vpn_hide_add_uid(u32 uid)
{
    u32 appid = XNSU_VH_APPID(uid);

    // Atomic set: concurrent add/remove/clear are serialized on vh_lock below
    // is not required for correctness of a single bit; keep it lock-free so the
    // manager can toggle targets cheaply.
    test_and_set_bit(appid, vh_appids);
    // Re-evaluate marks so the newly targeted app is marked.
    xnsu_mark_running_process();
    return 0;
}

int xnsu_vpn_hide_remove_uid(u32 uid)
{
    u32 appid = XNSU_VH_APPID(uid);

    test_and_clear_bit(appid, vh_appids);
    xnsu_mark_running_process();
    return 0;
}

void xnsu_vpn_hide_clear_uids(void)
{
    spin_lock_irq(&vh_lock);
    bitmap_zero(vh_appids, XNSU_VH_APPID_MAX);
    spin_unlock_irq(&vh_lock);
    xnsu_mark_running_process();
}

// Bitmap membership only. Callers gate on their own feature's static key:
// the combined check below merely keeps the tracepoint mark alive while
// either vpn-hide layer is active (the mark is what routes target apps'
// syscalls into the hook dispatcher at all).
bool xnsu_vpn_hide_is_target(uid_t uid)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide) &&
        !static_branch_unlikely(&xnsu_vpn_ports)) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(uid), vh_appids);
}

bool xnsu_vpn_hide_should_filter_dents(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

// Gates for the read/ioctl/setsockopt hooks: same shape as the dents gate.
bool xnsu_vpn_hide_should_filter_read(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

bool xnsu_vpn_hide_should_filter_ioctl(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

bool xnsu_vpn_hide_should_filter_sockopt(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

// Ports blocking gates on its own static key, sharing the target bitmap.
bool xnsu_vpn_ports_should_block_connect(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_ports)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

bool xnsu_vpn_hide_should_filter_netlink(void)
{
    // Same gating as the getdents path: feature on, not init, current is a target.
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return test_bit(XNSU_VH_APPID(current_uid().val), vh_appids);
}

static bool vh_name_is_vpn(const char *name)
{
    size_t i;

    for (i = 0; i < ARRAY_SIZE(vh_iface_prefixes); i++) {
        size_t len = strlen(vh_iface_prefixes[i]);
        // Prefix match: "tun", "tun0", "wg0" ... hide; "wlan0"/"eth0" stay.
        if (strncmp(name, vh_iface_prefixes[i], len) == 0) {
            return true;
        }
    }
    return false;
}

long xnsu_vpn_hide_filter_getdents64(unsigned int fd, void __user *dirp, long total)
{
    struct file *f;
    char *dirbuf;
    char *kbuf;
    char *dpath;
    long off, wr;
    bool bail = false;

    if (total <= 0 || total > XNSU_VH_DENTS_MAX) {
        return total;
    }

    f = fget(fd);
    if (!f) {
        return total;
    }
    dirbuf = kmalloc(XNSU_VH_PATH_LEN, GFP_KERNEL);
    if (!dirbuf) {
        fput(f);
        return total;
    }
    dpath = d_path(&f->f_path, dirbuf, XNSU_VH_PATH_LEN);
    fput(f);
    if (IS_ERR(dpath)) {
        kfree(dirbuf);
        return total;
    }
    // Only the interface-enumeration directory is filtered.
    if (strcmp(dpath, XNSU_VH_NET_DIR) != 0) {
        kfree(dirbuf);
        return total;
    }
    kfree(dirbuf);

    kbuf = kmalloc(total, GFP_KERNEL);
    if (!kbuf) {
        return total;
    }
    if (copy_from_user(kbuf, dirp, total)) {
        kfree(kbuf);
        return total;
    }

    off = 0;
    wr = 0;
    while (off < total) {
        struct xnsu_vh_dirent64 *d = (struct xnsu_vh_dirent64 *)(kbuf + off);
        unsigned short reclen;
        bool hide = false;

        if (off + (long)offsetof(struct xnsu_vh_dirent64, d_name) > total) {
            bail = true;
            break;
        }
        reclen = d->d_reclen;
        if (reclen < offsetof(struct xnsu_vh_dirent64, d_name) || off + reclen > total) {
            bail = true;
            break;
        }

        if (d->d_name[0] != '\0' && strcmp(d->d_name, ".") != 0 && strcmp(d->d_name, "..") != 0) {
            hide = vh_name_is_vpn(d->d_name);
        }

        if (!hide) {
            if (wr != off) {
                memmove(kbuf + wr, kbuf + off, reclen);
            }
            wr += reclen;
        }
        off += reclen;
    }

    if (bail || wr == total) {
        kfree(kbuf);
        return total;
    }

    if (copy_to_user(dirp, kbuf, wr)) {
        kfree(kbuf);
        return total;
    }
    kfree(kbuf);
    return wr;
}

// Bounded prefix match for an rtattr-carried interface name. The name lives in
// our kernel copy of the netlink buffer, so reads never fault, but we still cap
// the comparison at the attribute payload length.
static bool vh_attr_name_is_vpn(const char *name, int maxlen)
{
    size_t i;

    if (maxlen <= 0) {
        return false;
    }
    for (i = 0; i < ARRAY_SIZE(vh_iface_prefixes); i++) {
        size_t plen = strlen(vh_iface_prefixes[i]);
        if ((int)plen <= maxlen && strncmp(name, vh_iface_prefixes[i], plen) == 0) {
            return true;
        }
    }
    return false;
}

// Decide whether a single netlink message describes a VPN-ish interface.
// RTM_NEWLINK/DELLINK carry the interface name in IFLA_IFNAME; RTM_NEWADDR/
// DELADDR carry it (best effort) in IFA_LABEL. Anything else is never hidden.
static bool vh_nlmsg_is_vpn(const struct nlmsghdr *nlh)
{
    unsigned short type = nlh->nlmsg_type;

    if (type == RTM_NEWLINK || type == RTM_DELLINK) {
        struct ifinfomsg *ifi = NLMSG_DATA(nlh);
        int attrlen = (int)nlh->nlmsg_len - (int)NLMSG_LENGTH(sizeof(*ifi));
        // IFLA_RTA(ifi) equivalent (the macro is not exposed to modules).
        struct rtattr *rta = (struct rtattr *)((char *)ifi + NLMSG_ALIGN(sizeof(*ifi)));

        while (RTA_OK(rta, attrlen)) {
            if (rta->rta_type == IFLA_IFNAME) {
                return vh_attr_name_is_vpn((const char *)RTA_DATA(rta), (int)RTA_PAYLOAD(rta));
            }
            rta = RTA_NEXT(rta, attrlen);
        }
    } else if (type == RTM_NEWADDR || type == RTM_DELADDR) {
        struct ifaddrmsg *ifa = NLMSG_DATA(nlh);
        int attrlen = (int)nlh->nlmsg_len - (int)NLMSG_LENGTH(sizeof(*ifa));
        // IFA_RTA(ifa) equivalent (the macro is not exposed to modules).
        struct rtattr *rta = (struct rtattr *)((char *)ifa + NLMSG_ALIGN(sizeof(*ifa)));

        while (RTA_OK(rta, attrlen)) {
            if (rta->rta_type == IFA_LABEL) {
                return vh_attr_name_is_vpn((const char *)RTA_DATA(rta), (int)RTA_PAYLOAD(rta));
            }
            rta = RTA_NEXT(rta, attrlen);
        }
    }
    return false;
}

// Rewrite a netlink reply in place, dropping whole messages for VPN interfaces.
// Returns the new length (<= len), or len unchanged on any parse anomaly.
static long vh_filter_nl_buf(char *buf, long len)
{
    long off = 0, wr = 0;
    bool changed = false;

    while (off < len) {
        struct nlmsghdr *nlh = (struct nlmsghdr *)(buf + off);
        u32 mlen, alen;

        if (off + (long)NLMSG_HDRLEN > len) {
            return len; // truncated header: bail
        }
        mlen = nlh->nlmsg_len;
        alen = NLMSG_ALIGN(mlen);
        if (mlen < NLMSG_HDRLEN || off + (long)alen > len) {
            return len; // malformed / unaligned tail: bail
        }

        if (vh_nlmsg_is_vpn(nlh)) {
            changed = true;
        } else {
            if (wr != off) {
                memmove(buf + wr, buf + off, alen);
            }
            wr += alen;
        }
        off += alen;
    }

    return changed ? wr : len;
}

// Copy a user netlink buffer in, filter it, copy the (shrunk) result back.
// Returns the new byte count, or total unchanged on any error.
//
// Note: the GKI/DDK headers do not expose socket internals (struct socket_alloc
// / SOCKET_I are hidden), so we cannot cheaply confirm the fd is AF_NETLINK.
// Instead vh_filter_nl_buf parses strictly -- it only shrinks the reply when the
// whole buffer tiles as valid nlmsghdr records AND one is a well-formed RTM
// link/addr message naming a VPN interface. Any other reply (TCP/UDP/other
// netlink families) is returned byte-for-byte unchanged.
static long vh_rewrite_user_nl(void __user *ubuf, long total)
{
    char *kbuf;
    long wr;

    if (!ubuf || total <= 0 || total > XNSU_VH_BUF_MAX) {
        return total;
    }
    kbuf = kmalloc(total, GFP_KERNEL);
    if (!kbuf) {
        return total;
    }
    if (copy_from_user(kbuf, ubuf, total)) {
        kfree(kbuf);
        return total;
    }

    wr = vh_filter_nl_buf(kbuf, total);
    if (wr >= total || wr <= 0) {
        kfree(kbuf);
        return total;
    }
    if (copy_to_user(ubuf, kbuf, wr)) {
        kfree(kbuf);
        return total;
    }
    kfree(kbuf);
    return wr;
}

long xnsu_vpn_hide_filter_netlink_recvfrom(unsigned int fd, void __user *ubuf, long total, unsigned long buflen)
{
    (void)fd;
    // Guard against MSG_TRUNC returning more than the user buffer holds -- never
    // read/write past the caller's buffer.
    if (total > (long)buflen) {
        return total;
    }
    return vh_rewrite_user_nl(ubuf, total);
}

long xnsu_vpn_hide_filter_netlink_recvmsg(unsigned int fd, void __user *msg_user, long total)
{
    struct user_msghdr umsg;
    struct iovec iov;

    (void)fd;
    if (copy_from_user(&umsg, msg_user, sizeof(umsg))) {
        return total;
    }
    // Only the single-iovec case (what getifaddrs / NetworkInterface use) is
    // rewritten; scatter-gather replies pass through untouched.
    if (umsg.msg_iovlen != 1 || !umsg.msg_iov) {
        return total;
    }
    if (copy_from_user(&iov, umsg.msg_iov, sizeof(iov))) {
        return total;
    }
    if (!iov.iov_base || (long)iov.iov_len < total) {
        return total;
    }
    return vh_rewrite_user_nl(iov.iov_base, total);
}

// ── /proc/net reads ─────────────────────────────────────────────────────────
//
// /proc/net/{route,dev,if_inet6} name every interface including tunnels; apps
// that cannot use netlink (or simply grep) read these instead. Filtered by
// line: a line is dropped when any of its whitespace/colon-delimited tokens
// names a VPN-ish interface. The iface column differs per file (route: first
// token, dev: token before ':', if_inet6: last token) -- matching any token
// covers all three shapes without per-file parsers. Hex fields cannot
// false-match: none of the prefixes starts with a hex digit.

static const char *const vh_proc_files[] = { "route", "dev", "if_inet6" };

// Identify the interesting files by dentry names: the opened path is
// /proc/<pid>/net/<name> (proc "net" is a self-symlink, so d_path would leak
// the pid form), which makes "parent is net, name is one of ours" the stable
// check regardless of which pid form the opener used.
static bool vh_file_is_proc_net_iface_table(const struct file *f)
{
    const unsigned char *name, *parent;
    size_t i;

    if (!f || !f->f_path.dentry || !f->f_path.dentry->d_parent)
        return false;
    name = f->f_path.dentry->d_name.name;
    parent = f->f_path.dentry->d_parent->d_name.name;
    if (!name || !parent || strcmp(parent, "net") != 0)
        return false;
    for (i = 0; i < ARRAY_SIZE(vh_proc_files); i++) {
        if (strcmp(name, vh_proc_files[i]) == 0)
            return true;
    }
    return false;
}

static bool vh_line_names_vpn(const char *line, long len)
{
    long i = 0;

    while (i < len) {
        long start;
        char name[IFNAMSIZ];
        long n;

        while (i < len && (line[i] == ' ' || line[i] == '\t' || line[i] == ':'))
            i++;
        start = i;
        while (i < len && line[i] != ' ' && line[i] != '\t' && line[i] != ':')
            i++;
        n = i - start;
        if (n <= 0)
            continue;
        if (n >= sizeof(name))
            n = sizeof(name) - 1;
        memcpy(name, line + start, n);
        name[n] = '\0';
        if (vh_name_is_vpn(name))
            return true;
    }
    return false;
}

// Post-filter a read() result: drop VPN-naming lines. Line fragments split
// across reads are a known limitation (seq_file rarely splits these small
// tables in practice).
long xnsu_vpn_hide_filter_read(unsigned int fd, void __user *ubuf, long total)
{
    struct file *f;
    char *kbuf;
    long off, wr;
    bool changed = false;

    if (total <= 0 || total > XNSU_VH_BUF_MAX)
        return total;
    f = fget(fd);
    if (!f)
        return total;
    if (!vh_file_is_proc_net_iface_table(f)) {
        fput(f);
        return total;
    }
    fput(f);

    kbuf = kmalloc(total, GFP_KERNEL);
    if (!kbuf)
        return total;
    if (copy_from_user(kbuf, ubuf, total)) {
        kfree(kbuf);
        return total;
    }

    off = 0;
    wr = 0;
    while (off < total) {
        long end = off;
        long n;

        while (end < total && kbuf[end] != '\n')
            end++;
        n = end - off + (end < total ? 1 : 0);
        if (!vh_line_names_vpn(kbuf + off, end - off)) {
            if (wr != off)
                memmove(kbuf + wr, kbuf + off, n);
            wr += n;
        } else {
            changed = true;
        }
        off = end + 1;
    }

    if (!changed) {
        kfree(kbuf);
        return total;
    }
    if (copy_to_user(ubuf, kbuf, wr)) {
        kfree(kbuf);
        return total;
    }
    kfree(kbuf);
    return wr;
}

// ── ioctl masking ───────────────────────────────────────────────────────────

static bool vh_fd_is_socket(unsigned int fd)
{
    struct file *f;
    bool is_sock;

    f = fget(fd);
    if (!f)
        return false;
    is_sock = S_ISSOCK(file_inode(f)->i_mode);
    fput(f);
    return is_sock;
}

// Pre-block: SIOCGIF* queries that NAME a specific interface return ENODEV
// when that name is a tunnel, so the iface "does not exist" from the app's
// point of view (also skips the real syscall -- no timing side channel).
bool xnsu_vpn_hide_ioctl_pre(unsigned int cmd, void __user *arg)
{
    struct ifreq ifr;

    switch (cmd) {
    case SIOCGIFFLAGS:
    case SIOCGIFADDR:
    case SIOCGIFDSTADDR:
    case SIOCGIFBRDADDR:
    case SIOCGIFNETMASK:
    case SIOCGIFMETRIC:
    case SIOCGIFMTU:
    case SIOCGIFHWADDR:
    case SIOCGIFTXQLEN:
    case SIOCGIFMAP:
        break;
    default:
        return false;
    }
    if (!arg)
        return false;
    if (copy_from_user(&ifr, arg, sizeof(ifr)))
        return false;
    return vh_name_is_vpn(ifr.ifr_name);
}

// Post-filter the two output-shaped queries:
//   SIOCGIFNAME (index -> name): report ENODEV when the resolved name is VPN-ish.
//   SIOCGIFCONF (enumerate): compact the ifreq array in place, dropping VPN
//   entries and shrinking ifc_len accordingly.
long xnsu_vpn_hide_ioctl_post(unsigned int cmd, void __user *arg, long ret)
{
    struct ifreq ifr;

    if (ret < 0 || !arg)
        return ret;

    if (cmd == SIOCGIFNAME) {
        if (copy_from_user(&ifr, arg, sizeof(ifr)))
            return ret;
        if (vh_name_is_vpn(ifr.ifr_name))
            return -ENODEV;
        return ret;
    }

    if (cmd == SIOCGIFCONF) {
        struct ifconf ifc;
        char *kreq;
        long count, rd, wr;

        if (copy_from_user(&ifc, arg, sizeof(ifc)))
            return ret;
        if (!ifc.ifc_req || ifc.ifc_len <= 0 || ifc.ifc_len > XNSU_VH_BUF_MAX)
            return ret;
        kreq = kmalloc(ifc.ifc_len, GFP_KERNEL);
        if (!kreq)
            return ret;
        if (copy_from_user(kreq, ifc.ifc_req, ifc.ifc_len)) {
            kfree(kreq);
            return ret;
        }

        count = ifc.ifc_len / (long)sizeof(struct ifreq);
        wr = 0;
        for (rd = 0; rd < count; rd++) {
            struct ifreq *cur = (struct ifreq *)(kreq + rd * sizeof(struct ifreq));

            if (vh_name_is_vpn(cur->ifr_name))
                continue;
            if (wr != rd)
                memcpy(kreq + wr * sizeof(struct ifreq), cur, sizeof(struct ifreq));
            wr++;
        }
        if (wr != count) {
            int newlen = (int)(wr * sizeof(struct ifreq));

            if (!copy_to_user(ifc.ifc_req, kreq, newlen)) {
                ifc.ifc_len = newlen;
                // Best effort: if the header write fails the app sees the
                // original (larger) length and reads garbage tail entries --
                // treat that as unrecoverable and keep the success return,
                // since the real kernel call already succeeded.
                (void)copy_to_user(arg, &ifc, sizeof(ifc));
            }
        }
        kfree(kreq);
        return ret;
    }

    return ret;
}

// ── SO_BINDTODEVICE / loopback connect (ports layer) ────────────────────────

// Deny SO_BINDTODEVICE naming a VPN interface for target apps: unprivileged
// probing of tunnel devices via a bound socket is a known VPN-exit-IP leak
// (Google issue 516559265, "won't fix"). Returns -EPERM to short-circuit,
// 0 to run the real setsockopt.
int xnsu_vpn_hide_filter_setsockopt(unsigned int fd, int level, int optname,
                                    void __user *optval, int optlen)
{
    char name[IFNAMSIZ];

    (void)fd;
    if (level != SOL_SOCKET || optname != SO_BINDTODEVICE)
        return 0;
    if (!optval || optlen <= 0 || optlen > (int)sizeof(name))
        return 0;
    if (copy_from_user(name, optval, optlen))
        return 0;
    name[sizeof(name) - 1] = '\0';
    if (vh_name_is_vpn(name))
        return -EPERM;
    return 0;
}

// IPv6 loopback and v4-mapped loopback checks, hand-rolled: the trimmed DDK
// headers do not expose the ipv6_addr_* helpers.
static bool vh_in6_is_loopback(const struct in6_addr *a)
{
    return (a->s6_addr32[0] | a->s6_addr32[1] | a->s6_addr32[2]) == 0 &&
           a->s6_addr32[3] == htonl(1);
}

static bool vh_in6_is_v4mapped_loopback(const struct in6_addr *a)
{
    return a->s6_addr32[0] == 0 && a->s6_addr32[1] == 0 &&
           a->s6_addr32[2] == htonl(0x0000ffff) &&
           a->s6_addr32[3] == htonl(INADDR_LOOPBACK);
}

// connect() to 127.0.0.1 / ::1 fails with ECONNREFUSED for target apps while
// the ports layer is on, so locally running VPN / proxy daemons cannot be
// found by port probing. IPv4-mapped-v6 loopback is covered too.
int xnsu_vpn_ports_filter_connect(void __user *addr_user, int addrlen)
{
    union {
        struct sockaddr sa;
        struct sockaddr_in in4;
        struct sockaddr_in6 in6;
    } addr;

    if (!addr_user || addrlen < (int)sizeof(struct sockaddr_in) ||
        addrlen > (int)sizeof(addr))
        return 0;
    if (copy_from_user(&addr, addr_user, addrlen))
        return 0;

    if (addr.sa.sa_family == AF_INET) {
        if (addr.in4.sin_addr.s_addr == htonl(INADDR_LOOPBACK))
            return -ECONNREFUSED;
    } else if (addr.sa.sa_family == AF_INET6) {
        if (vh_in6_is_loopback(&addr.in6.sin6_addr))
            return -ECONNREFUSED;
        if (vh_in6_is_v4mapped_loopback(&addr.in6.sin6_addr))
            return -ECONNREFUSED;
    }
    return 0;
}

static int vpn_hide_feature_get(u64 *value)
{
    *value = static_key_enabled(&xnsu_vpn_hide) ? 1 : 0;
    return 0;
}

static int vpn_hide_feature_set(u64 value)
{
    if (value) {
        static_key_enable(&xnsu_vpn_hide.key);
    } else {
        static_key_disable(&xnsu_vpn_hide.key);
    }
    pr_info("vpn_hide: set to %llu\n", value);
    // Mark/unmark target apps to match the new state.
    xnsu_mark_running_process();
    return 0;
}

static const struct xnsu_feature_handler vpn_hide_handler = {
    .feature_id = XNSU_FEATURE_VPN_HIDE,
    .name = "vpn_hide",
    .get_handler = vpn_hide_feature_get,
    .set_handler = vpn_hide_feature_set,
};

static int vpn_ports_feature_get(u64 *value)
{
    *value = static_key_enabled(&xnsu_vpn_ports) ? 1 : 0;
    return 0;
}

static int vpn_ports_feature_set(u64 value)
{
    if (value) {
        static_key_enable(&xnsu_vpn_ports.key);
    } else {
        static_key_disable(&xnsu_vpn_ports.key);
    }
    pr_info("vpn_ports: set to %llu\n", value);
    // Target marks are shared with vpn_hide; refresh for the new state.
    xnsu_mark_running_process();
    return 0;
}

static const struct xnsu_feature_handler vpn_ports_handler = {
    .feature_id = XNSU_FEATURE_VPN_PORTS,
    .name = "vpn_ports",
    .get_handler = vpn_ports_feature_get,
    .set_handler = vpn_ports_feature_set,
};

void __init xnsu_vpn_hide_init(void)
{
    if (xnsu_register_feature_handler(&vpn_hide_handler)) {
        pr_err("Failed to register vpn_hide feature handler\n");
    }
    if (xnsu_register_feature_handler(&vpn_ports_handler)) {
        pr_err("Failed to register vpn_ports feature handler\n");
    }
}

void __exit xnsu_vpn_hide_exit(void)
{
    xnsu_unregister_feature_handler(XNSU_FEATURE_VPN_HIDE);
    xnsu_unregister_feature_handler(XNSU_FEATURE_VPN_PORTS);
}
