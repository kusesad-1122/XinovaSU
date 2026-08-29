#pragma once
// Minimal LSPlant stub for XinovaSU Zygisk VPN hide.
// Replace this file with the real LSPlant (https://github.com/LSPosed/LSPlant)
// to enable ART method hooking for ConnectivityManager/NetworkCapabilities.
// This stub allows the module to compile without LSPlant; Java hooks will be no-ops.

#include <android/log.h>

namespace lsplant {

inline bool Hook(void* target, void* hook, void** backup) {
    __android_log_print(ANDROID_LOG_WARN, "XinovaSU-VpnHide",
                        "LSPlant stub: Hook(%p) called but LSPlant not vendored", target);
    if (backup) *backup = nullptr;
    (void)hook;
    return false;
}

inline bool UnHook(void* target) {
    (void)target;
    return false;
}

} // namespace lsplant
