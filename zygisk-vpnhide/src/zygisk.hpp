// Zygisk API header (API version 4).
//
// This is the standard, ABI-stable Zygisk module interface implemented by
// ZygiskNext / Magisk-Zygisk. It is vendored here so the XinovaSU VPN-hide
// module can be built without an external SDK. Do not change the layout of the
// structs / vtable below -- it must match what the Zygisk loader expects.

#pragma once

#include <cstdint>
#include <jni.h>

#define ZYGISK_API_VERSION 4

namespace zygisk {

struct Api;
struct AppSpecializeArgs;
struct ServerSpecializeArgs;

class ModuleBase {
public:
    virtual void onLoad([[maybe_unused]] Api *api, [[maybe_unused]] JNIEnv *env) {}
    virtual void preAppSpecialize([[maybe_unused]] AppSpecializeArgs *args) {}
    virtual void postAppSpecialize([[maybe_unused]] const AppSpecializeArgs *args) {}
    virtual void preServerSpecialize([[maybe_unused]] ServerSpecializeArgs *args) {}
    virtual void postServerSpecialize([[maybe_unused]] const ServerSpecializeArgs *args) {}
};

struct AppSpecializeArgs {
    jint &uid;
    jint &gid;
    jintArray &gids;
    jint &runtime_flags;
    jobjectArray &rlimits;
    jint &mount_external;
    jstring &se_info;
    jstring &nice_name;
    jstring &instruction_set;
    jstring &app_data_dir;

    jintArray *&fds_to_ignore;
    jboolean *&is_child_zygote;
    jboolean *&is_top_app;
    jobjectArray *&pkg_data_info_list;
    jobjectArray *&whitelisted_data_info_list;
    jboolean *&mount_data_dirs;
    jboolean *&mount_storage_dirs;

    AppSpecializeArgs() = delete;
};

struct ServerSpecializeArgs {
    jint &uid;
    jint &gid;
    jintArray &gids;
    jint &runtime_flags;
    jlong &permitted_capabilities;
    jlong &effective_capabilities;

    ServerSpecializeArgs() = delete;
};

namespace internal {
struct api_table;
template <class T>
void entry_impl(api_table *, JNIEnv *);
} // namespace internal

// These bit flags are used in `setOption()` in Api.
enum Option : int {
    // Force Magisk's denylist unmount routines to run on this process.
    FORCE_DENYLIST_UNMOUNT = 0,
    // When this process is being forked, don't record its PID for later.
    DLCLOSE_MODULE_LIBRARY = 1,
};

// Bit masks of the return value of `getFlags()`.
enum StateFlag : uint32_t {
    PROCESS_GRANTED_ROOT = (1u << 0),
    PROCESS_ON_DENYLIST = (1u << 1),
};

// All API functions will stop working after `postAppSpecialize` returns.
struct Api {
    // Connect to a companion request handler.
    int connectCompanion();
    // Get the file descriptor of the root folder of the current module.
    int getModuleDir();
    // Set various options for your module.
    void setOption(Option opt);
    // Get information about the current process.
    uint32_t getFlags();
    // Hook JNI native methods for a class.
    void hookJniNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *methods, int numMethods);
    // Register a PLT hook for a function (regex on library name / symbol).
    void pltHookRegister(const char *regex, const char *symbol, void *newFunc, void **oldFunc);
    // Commit all the pending PLT hooks.
    bool pltHookCommit();

private:
    internal::api_table *tbl;
    template <class T>
    friend void internal::entry_impl(internal::api_table *, JNIEnv *);
};

} // namespace zygisk

// The type of the module entry function.
extern "C" [[gnu::visibility("default")]] [[maybe_unused]] void zygisk_module_entry(zygisk::internal::api_table *,
                                                                                    JNIEnv *);
extern "C" [[gnu::visibility("default")]] [[maybe_unused]] void zygisk_companion_entry(int);

namespace zygisk {
namespace internal {

struct module_abi {
    long api_version;
    ModuleBase *impl;

    void (*preAppSpecialize)(ModuleBase *, AppSpecializeArgs *);
    void (*postAppSpecialize)(ModuleBase *, const AppSpecializeArgs *);
    void (*preServerSpecialize)(ModuleBase *, ServerSpecializeArgs *);
    void (*postServerSpecialize)(ModuleBase *, const ServerSpecializeArgs *);
};

struct api_table {
    // These first 2 entries are permanent, means we don't change them anymore.
    void *impl;
    bool (*registerModule)(api_table *, module_abi *);

    void (*hookJniNativeMethods)(JNIEnv *, const char *, JNINativeMethod *, int);
    void (*pltHookRegister)(const char *, const char *, void *, void **);
    bool (*exemptFd)(int);
    bool (*pltHookCommit)();
    int (*connectCompanion)(void * /* impl */);
    void (*setOption)(void * /* impl */, Option);
    int (*getModuleDir)(void * /* impl */);
    uint32_t (*getFlags)(void * /* impl */);
};

template <class T>
void entry_impl(api_table *table, JNIEnv *env) {
    ModuleBase *module = new T();
    if (!table->registerModule)
        return;
    static module_abi abi = {
        .api_version = ZYGISK_API_VERSION,
        .impl = module,
        .preAppSpecialize = [](ModuleBase *m, AppSpecializeArgs *args) { m->preAppSpecialize(args); },
        .postAppSpecialize = [](ModuleBase *m, const AppSpecializeArgs *args) { m->postAppSpecialize(args); },
        .preServerSpecialize = [](ModuleBase *m, ServerSpecializeArgs *args) { m->preServerSpecialize(args); },
        .postServerSpecialize = [](ModuleBase *m, const ServerSpecializeArgs *args) { m->postServerSpecialize(args); },
    };
    if (!table->registerModule(table, &abi))
        return;
    auto api = new Api();
    api->tbl = table;
    module->onLoad(api, env);
}

} // namespace internal

inline int Api::connectCompanion() {
    return tbl->connectCompanion ? tbl->connectCompanion(tbl->impl) : -1;
}
inline int Api::getModuleDir() {
    return tbl->getModuleDir ? tbl->getModuleDir(tbl->impl) : -1;
}
inline void Api::setOption(Option opt) {
    if (tbl->setOption)
        tbl->setOption(tbl->impl, opt);
}
inline uint32_t Api::getFlags() {
    return tbl->getFlags ? tbl->getFlags(tbl->impl) : 0;
}
inline void Api::hookJniNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *methods, int numMethods) {
    if (tbl->hookJniNativeMethods)
        tbl->hookJniNativeMethods(env, className, methods, numMethods);
}
inline void Api::pltHookRegister(const char *regex, const char *symbol, void *newFunc, void **oldFunc) {
    if (tbl->pltHookRegister)
        tbl->pltHookRegister(regex, symbol, newFunc, oldFunc);
}
inline bool Api::pltHookCommit() {
    return tbl->pltHookCommit && tbl->pltHookCommit();
}

} // namespace zygisk

#define REGISTER_ZYGISK_MODULE(clazz)                                                                                  \
    extern "C" [[gnu::visibility("default")]] void zygisk_module_entry(zygisk::internal::api_table *table,             \
                                                                       JNIEnv *env) {                                  \
        zygisk::internal::entry_impl<clazz>(table, env);                                                               \
    }

#define REGISTER_ZYGISK_COMPANION(func)                                                                                \
    extern "C" [[gnu::visibility("default")]] void zygisk_companion_entry(int client) {                                \
        func(client);                                                                                                  \
    }
