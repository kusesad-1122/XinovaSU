#include <linux/cred.h>
#include <linux/net.h>
#include <linux/printk.h>
#include <linux/socket.h>
#include <linux/spinlock.h>
#include <linux/static_key.h>
#include <linux/types.h>

#include "feature/net_isolate.h"
#include "hook/lsm_hook.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep

#define XNSU_NI_MAX_UIDS 4096
#define XNSU_NI_APPID(uid) ((uid) % 100000)

// Master switch: gated by the XNSU_FEATURE_NET_ISOLATE feature toggle so the
// hot connect() path is near-free while the feature is off.
static DEFINE_STATIC_KEY_FALSE(xnsu_net_isolate);
static DEFINE_SPINLOCK(ni_lock);
static u32 ni_appids[XNSU_NI_MAX_UIDS];
static int ni_count;

static bool ni_is_blocked(u32 appid)
{
    int i;
    bool blocked = false;
    unsigned long flags;

    spin_lock_irqsave(&ni_lock, flags);
    for (i = 0; i < ni_count; i++) {
        if (ni_appids[i] == appid) {
            blocked = true;
            break;
        }
    }
    spin_unlock_irqrestore(&ni_lock, flags);
    return blocked;
}

static int my_socket_connect(struct socket *sock, struct sockaddr *address, int addrlen);
struct xnsu_lsm_hook net_isolate_connect_hook =
    XNSU_LSM_HOOK_INIT(socket_connect, "selinux_socket_connect", my_socket_connect, 0);

typedef int (*socket_connect_fn)(struct socket *sock, struct sockaddr *address, int addrlen);
static int __nocfi my_socket_connect(struct socket *sock, struct sockaddr *address, int addrlen)
{
    if (static_branch_unlikely(&xnsu_net_isolate) && address &&
        (address->sa_family == AF_INET || address->sa_family == AF_INET6) &&
        ni_is_blocked(XNSU_NI_APPID(current_uid().val))) {
        return -EPERM;
    }
    return ((socket_connect_fn)net_isolate_connect_hook.original)(sock, address, addrlen);
}

int xnsu_net_isolate_add(u32 uid)
{
    u32 appid = XNSU_NI_APPID(uid);
    int i;
    int ret = 0;
    unsigned long flags;

    spin_lock_irqsave(&ni_lock, flags);
    for (i = 0; i < ni_count; i++) {
        if (ni_appids[i] == appid) {
            goto out; // already present
        }
    }
    if (ni_count >= XNSU_NI_MAX_UIDS) {
        ret = -ENOSPC;
        goto out;
    }
    ni_appids[ni_count++] = appid;
out:
    spin_unlock_irqrestore(&ni_lock, flags);
    return ret;
}

int xnsu_net_isolate_remove(u32 uid)
{
    u32 appid = XNSU_NI_APPID(uid);
    int i;
    unsigned long flags;

    spin_lock_irqsave(&ni_lock, flags);
    for (i = 0; i < ni_count; i++) {
        if (ni_appids[i] == appid) {
            ni_appids[i] = ni_appids[--ni_count];
            break;
        }
    }
    spin_unlock_irqrestore(&ni_lock, flags);
    return 0;
}

void xnsu_net_isolate_clear(void)
{
    unsigned long flags;

    spin_lock_irqsave(&ni_lock, flags);
    ni_count = 0;
    spin_unlock_irqrestore(&ni_lock, flags);
}

static int net_isolate_feature_get(u64 *value)
{
    *value = static_key_enabled(&xnsu_net_isolate) ? 1 : 0;
    return 0;
}

static int net_isolate_feature_set(u64 value)
{
    if (value) {
        static_key_enable(&xnsu_net_isolate.key);
    } else {
        static_key_disable(&xnsu_net_isolate.key);
    }
    pr_info("net_isolate: set to %llu\n", value);
    return 0;
}

static const struct xnsu_feature_handler net_isolate_handler = {
    .feature_id = XNSU_FEATURE_NET_ISOLATE,
    .name = "net_isolate",
    .get_handler = net_isolate_feature_get,
    .set_handler = net_isolate_feature_set,
};

void __init xnsu_net_isolate_init(void)
{
    if (xnsu_register_feature_handler(&net_isolate_handler)) {
        pr_err("Failed to register net_isolate feature handler\n");
    }
    if (xnsu_lsm_hook(&net_isolate_connect_hook)) {
        pr_err("net_isolate: failed to hook socket_connect\n");
    }
}

void __exit xnsu_net_isolate_exit(void)
{
    xnsu_lsm_unhook(&net_isolate_connect_hook);
    xnsu_unregister_feature_handler(XNSU_FEATURE_NET_ISOLATE);
}
