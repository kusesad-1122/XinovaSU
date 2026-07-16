#include <linux/printk.h>
#include <linux/string.h>
#include <linux/types.h>
#include <linux/utsname.h>

#include "feature/kernel_spoof.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep

static bool xnsu_kernel_spoof_enabled;
static bool orig_saved;
static char orig_release[__NEW_UTS_LEN + 1];
static char orig_version[__NEW_UTS_LEN + 1];

static void save_original_once(void)
{
    if (orig_saved) {
        return;
    }
    // utsname() is an inline accessor (current->nsproxy->uts_ns->name). The
    // manager runs in the init uts namespace, so this captures the real
    // init_uts_ns strings without referencing any exported symbol (no uts_sem
    // / init_uts_ns dependency, keeping the LKM loadable across GKI targets).
    strscpy(orig_release, utsname()->release, sizeof(orig_release));
    strscpy(orig_version, utsname()->version, sizeof(orig_version));
    orig_saved = true;
}

int xnsu_kernel_spoof_apply(u8 op, const char *release, const char *version)
{
    struct new_utsname *uts = utsname();

    save_original_once();

    if (op == 0) {
        strscpy(uts->release, orig_release, sizeof(uts->release));
        strscpy(uts->version, orig_version, sizeof(uts->version));
        xnsu_kernel_spoof_enabled = false;
        pr_info("kernel_spoof: restored original uname\n");
        return 0;
    }

    if (release && release[0]) {
        strscpy(uts->release, release, sizeof(uts->release));
    }
    if (version && version[0]) {
        strscpy(uts->version, version, sizeof(uts->version));
    }
    xnsu_kernel_spoof_enabled = true;
    pr_info("kernel_spoof: uname release='%s' version='%s'\n", uts->release, uts->version);
    return 0;
}

static int kernel_spoof_feature_get(u64 *value)
{
    *value = xnsu_kernel_spoof_enabled ? 1 : 0;
    return 0;
}

static int kernel_spoof_feature_set(u64 value)
{
    // The spoofed strings are delivered through XNSU_IOCTL_SET_UTS_SPOOF;
    // toggling the feature off through the generic feature API restores the
    // captured original uname. Enabling here is a no-op.
    if (value == 0 && xnsu_kernel_spoof_enabled) {
        xnsu_kernel_spoof_apply(0, NULL, NULL);
    }
    return 0;
}

static const struct xnsu_feature_handler kernel_spoof_handler = {
    .feature_id = XNSU_FEATURE_KERNEL_SPOOF,
    .name = "kernel_spoof",
    .get_handler = kernel_spoof_feature_get,
    .set_handler = kernel_spoof_feature_set,
};

void __init xnsu_kernel_spoof_init(void)
{
    if (xnsu_register_feature_handler(&kernel_spoof_handler)) {
        pr_err("Failed to register kernel_spoof feature handler\n");
    }
}

void __exit xnsu_kernel_spoof_exit(void)
{
    if (xnsu_kernel_spoof_enabled) {
        xnsu_kernel_spoof_apply(0, NULL, NULL);
    }
    xnsu_unregister_feature_handler(XNSU_FEATURE_KERNEL_SPOOF);
}
