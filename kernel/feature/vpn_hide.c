#include <linux/cred.h>
#include <linux/dcache.h>
#include <linux/err.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/if_addr.h>
#include <linux/if_link.h>
#include <linux/kernel.h>
#include <linux/netlink.h>
#include <linux/printk.h>
#include <linux/rtnetlink.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/socket.h>
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

#define XNSU_VH_MAX_UIDS 4096
#define XNSU_VH_APPID(uid) ((uid) % 100000)
#define XNSU_VH_PATH_LEN 256
#define XNSU_VH_DENTS_MAX (256 * 1024)
// Upper bound on a netlink dump we will rewrite; larger replies pass through.
#define XNSU_VH_NL_MAX (256 * 1024)

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
static DEFINE_SPINLOCK(vh_lock);
static u32 vh_appids[XNSU_VH_MAX_UIDS];
static int vh_count;

int xnsu_vpn_hide_add_uid(u32 uid)
{
    u32 appid = XNSU_VH_APPID(uid);
    int i;
    int ret = 0;
    unsigned long flags;

    spin_lock_irqsave(&vh_lock, flags);
    for (i = 0; i < vh_count; i++) {
        if (vh_appids[i] == appid) {
            goto out; // already present
        }
    }
    if (vh_count >= XNSU_VH_MAX_UIDS) {
        ret = -ENOSPC;
        goto out;
    }
    vh_appids[vh_count++] = appid;
out:
    spin_unlock_irqrestore(&vh_lock, flags);
    // Re-evaluate marks so the newly targeted app is marked.
    xnsu_mark_running_process();
    return ret;
}

int xnsu_vpn_hide_remove_uid(u32 uid)
{
    u32 appid = XNSU_VH_APPID(uid);
    int i;
    unsigned long flags;

    spin_lock_irqsave(&vh_lock, flags);
    for (i = 0; i < vh_count; i++) {
        if (vh_appids[i] == appid) {
            vh_appids[i] = vh_appids[--vh_count];
            break;
        }
    }
    spin_unlock_irqrestore(&vh_lock, flags);
    xnsu_mark_running_process();
    return 0;
}

void xnsu_vpn_hide_clear_uids(void)
{
    unsigned long flags;

    spin_lock_irqsave(&vh_lock, flags);
    vh_count = 0;
    spin_unlock_irqrestore(&vh_lock, flags);
    xnsu_mark_running_process();
}

bool xnsu_vpn_hide_is_target(uid_t uid)
{
    u32 appid = XNSU_VH_APPID(uid);
    int i;
    bool found = false;
    unsigned long flags;

    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }

    spin_lock_irqsave(&vh_lock, flags);
    for (i = 0; i < vh_count; i++) {
        if (vh_appids[i] == appid) {
            found = true;
            break;
        }
    }
    spin_unlock_irqrestore(&vh_lock, flags);
    return found;
}

bool xnsu_vpn_hide_should_filter_dents(void)
{
    if (!static_branch_unlikely(&xnsu_vpn_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    return xnsu_vpn_hide_is_target(current_uid().val);
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
    return xnsu_vpn_hide_is_target(current_uid().val);
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

    if (!ubuf || total <= 0 || total > XNSU_VH_NL_MAX) {
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

void __init xnsu_vpn_hide_init(void)
{
    if (xnsu_register_feature_handler(&vpn_hide_handler)) {
        pr_err("Failed to register vpn_hide feature handler\n");
    }
}

void __exit xnsu_vpn_hide_exit(void)
{
    xnsu_unregister_feature_handler(XNSU_FEATURE_VPN_HIDE);
}
