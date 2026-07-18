// XinovaSU "hide VPN detection" -- Zygisk framework-layer module.
//
// Companion piece to the kernel-layer feature. The kernel already hides VPN
// interfaces from getdents(/sys/class/net) and netlink for selected apps; this
// module adds in-process coverage for vectors the kernel cannot reach from the
// app's syscalls -- primarily the ConnectivityManager / VpnService Java APIs
// whose answers come from system_server.
//
// Gating: the module acts only when (a) the framework sub-switch marker exists
// and (b) the running app's appid is in the shared vpn-hide target list. Because
// the app process is unprivileged and cannot read /data/adb, the decision is
// made by the root companion (see companion_handler) and queried over a socket.
//
// Current scope (this build): module load + companion gating + a native
// getifaddrs PLT hook (defense-in-depth; a no-op when the kernel netlink filter
// already stripped the interface). The ConnectivityManager / VpnService ART
// hook needs an ART method-hook engine and is a follow-up (see hook_java()).

#include "zygisk.hpp"

#include <android/log.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <ifaddrs.h>
#include <sys/socket.h>
#include <unistd.h>

#define LOG_TAG "XinovaSU-VpnHide"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

// Read by the ROOT companion only (the app process cannot read /data/adb).
static constexpr const char *kVpnHideConf = "/data/adb/ksu/vpn_hide.conf";
static constexpr const char *kFwMarker = "/data/adb/ksu/.vpn_hide_fw";

static const char *const kVpnPrefixes[] = {"tun", "tap", "ppp", "wg", "ipsec", "utun"};

static bool name_is_vpn(const char *name) {
    if (!name)
        return false;
    for (const char *p : kVpnPrefixes) {
        if (strncmp(name, p, strlen(p)) == 0)
            return true;
    }
    return false;
}

// ---------------------------------------------------------------------------
// getifaddrs PLT hook (native, defense-in-depth)
// ---------------------------------------------------------------------------

using getifaddrs_t = int (*)(struct ifaddrs **);
static getifaddrs_t orig_getifaddrs = nullptr;

static int my_getifaddrs(struct ifaddrs **ifap) {
    if (!orig_getifaddrs)
        return -1;
    int ret = orig_getifaddrs(ifap);
    if (ret != 0 || ifap == nullptr)
        return ret;

    // Drop VPN interfaces from the list. bionic allocates each node separately
    // (the ifaddrs is the first member of its storage), so an unlinked node is
    // freed to avoid a leak.
    struct ifaddrs *prev = nullptr;
    struct ifaddrs *cur = *ifap;
    while (cur != nullptr) {
        struct ifaddrs *next = cur->ifa_next;
        if (name_is_vpn(cur->ifa_name)) {
            if (prev)
                prev->ifa_next = next;
            else
                *ifap = next;
            free(cur);
        } else {
            prev = cur;
        }
        cur = next;
    }
    return ret;
}

// Placeholder for the framework-API hooks (ConnectivityManager.getNetwork
// Capabilities / getAllNetworks, VpnService.prepare). These are Java methods,
// so hooking them needs an ART method-hook engine (LSPlant-class) -- a
// deliberate follow-up. Kept as a named seam so the wiring is obvious.
static void hook_java(JNIEnv * /*env*/) {
    // TODO(follow-up): integrate an ART hook engine and neutralise
    // NetworkCapabilities.hasTransport(TRANSPORT_VPN) for target apps.
}

// ---------------------------------------------------------------------------
// Zygisk module
// ---------------------------------------------------------------------------

class VpnHideModule : public zygisk::ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        // Decide as early as possible; the Api stops working after
        // postAppSpecialize returns. Query the root companion with our uid.
        should_hide = query_companion(args ? args->uid : -1);
    }

    void postAppSpecialize(const AppSpecializeArgs * /*args*/) override {
        if (!should_hide) {
            // Not a target: make sure this module is unloaded from the process.
            api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }
        install_native_hooks();
        hook_java(env);
        LOGD("framework-layer VPN hiding active for this process");
    }

private:
    Api *api = nullptr;
    JNIEnv *env = nullptr;
    bool should_hide = false;

    bool query_companion(int uid) {
        if (uid < 0)
            return false;
        int fd = api->connectCompanion();
        if (fd < 0)
            return false;
        uint8_t hide = 0;
        bool ok = write(fd, &uid, sizeof(uid)) == sizeof(uid) && read(fd, &hide, sizeof(hide)) == sizeof(hide);
        close(fd);
        return ok && hide != 0;
    }

    void install_native_hooks() {
        // Match any mapped libc.so and redirect its getifaddrs import.
        api->pltHookRegister("libc\\.so$", "getifaddrs", reinterpret_cast<void *>(my_getifaddrs),
                             reinterpret_cast<void **>(&orig_getifaddrs));
        if (!api->pltHookCommit())
            LOGE("pltHookCommit failed");
    }
};

// ---------------------------------------------------------------------------
// Root companion: answers "is this uid a framework-hide target?"
// ---------------------------------------------------------------------------

static bool companion_should_hide(int uid) {
    if (uid < 0)
        return false;
    // Sub-switch must be enabled.
    if (access(kFwMarker, F_OK) != 0)
        return false;

    int fd = open(kVpnHideConf, O_RDONLY | O_CLOEXEC);
    if (fd < 0)
        return false;
    char buf[16384];
    ssize_t n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0)
        return false;
    buf[n] = '\0';

    // Format: line 1 = "1"/"0" (main switch), remaining lines = target uids.
    int appid = uid % 100000;
    char *save = nullptr;
    char *line = strtok_r(buf, "\n", &save);
    if (line == nullptr || line[0] != '1')
        return false; // main switch off -> nothing to do
    while ((line = strtok_r(nullptr, "\n", &save)) != nullptr) {
        if (line[0] == '\0')
            continue;
        if (atoi(line) % 100000 == appid)
            return true;
    }
    return false;
}

static void companion_handler(int client) {
    int uid = -1;
    if (read(client, &uid, sizeof(uid)) != sizeof(uid))
        return;
    uint8_t hide = companion_should_hide(uid) ? 1 : 0;
    write(client, &hide, sizeof(hide));
}

REGISTER_ZYGISK_MODULE(VpnHideModule)
REGISTER_ZYGISK_COMPANION(companion_handler)
