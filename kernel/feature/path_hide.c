#include <linux/cred.h>
#include <linux/dcache.h>
#include <linux/err.h>
#include <linux/fcntl.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/fs_struct.h>
#include <linux/kernel.h>
#include <linux/stddef.h>
#include <linux/path.h>
#include <linux/printk.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/spinlock.h>
#include <linux/static_key.h>
#include <linux/string.h>
#include <linux/types.h>
#include <linux/uaccess.h>

#include "feature/path_hide.h"
#include "hook/tp_marker.h"
#include "policy/feature.h"
#include "klog.h" // IWYU pragma: keep

#define XNSU_PH_MAX_PATHS 64
#define XNSU_PH_PATH_LEN 256
#define XNSU_PH_MAX_UIDS 1024
#define XNSU_PH_APPID(uid) ((uid) % 100000)
// Skip filtering absurdly large getdents buffers (avoids a huge transient kmalloc).
#define XNSU_PH_DENTS_MAX (256 * 1024)

// Kernel dirent64 layout. Declared locally to avoid depending on the header being
// includable the same way across all KMIs.
struct xnsu_dirent64 {
    u64 d_ino;
    s64 d_off;
    unsigned short d_reclen;
    unsigned char d_type;
    char d_name[];
};

// Master switch: the XNSU_FEATURE_PATH_HIDE feature toggle. Keeps the marked
// hot syscall paths near-free while the feature is off.
static DEFINE_STATIC_KEY_FALSE(xnsu_path_hide);
static DEFINE_SPINLOCK(ph_lock);
static char ph_paths[XNSU_PH_MAX_PATHS][XNSU_PH_PATH_LEN];
static int ph_path_count;
static u32 ph_uids[XNSU_PH_MAX_UIDS];
static int ph_uid_count;
static bool ph_filter_system; // reserved for a future system-wide mode

// Canonicalize a path for matching: rewrite well-known sdcard aliases to the
// canonical /storage/emulated/0 form, then strip trailing slashes (except the
// root "/"). Both stored paths and queried paths go through this, so either
// spelling of the same location matches. `out` must be XNSU_PH_PATH_LEN bytes.
static void ph_normalize(const char *in, char *out, size_t outlen)
{
    static const struct {
        const char *from;
        const char *to;
    } aliases[] = {
        { "/sdcard", "/storage/emulated/0" },
        { "/storage/self/primary", "/storage/emulated/0" },
        { "/mnt/user/0/primary", "/storage/emulated/0" },
    };
    const char *src = in;
    char tmp[XNSU_PH_PATH_LEN];
    size_t i, len;

    if (!in || !out || outlen == 0) {
        if (out && outlen) {
            out[0] = '\0';
        }
        return;
    }

    for (i = 0; i < ARRAY_SIZE(aliases); i++) {
        size_t flen = strlen(aliases[i].from);
        if (strncmp(in, aliases[i].from, flen) == 0 && (in[flen] == '\0' || in[flen] == '/')) {
            snprintf(tmp, sizeof(tmp), "%s%s", aliases[i].to, in + flen);
            src = tmp;
            break;
        }
    }

    strscpy(out, src, outlen);
    len = strlen(out);
    while (len > 1 && out[len - 1] == '/') {
        out[--len] = '\0';
    }
}

int xnsu_path_hide_add_path(const char *path)
{
    int i;
    int ret = 0;
    unsigned long flags;
    char norm[XNSU_PH_PATH_LEN];

    if (!path || !path[0]) {
        return -EINVAL;
    }

    ph_normalize(path, norm, sizeof(norm));
    if (!norm[0]) {
        return -EINVAL;
    }

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_path_count; i++) {
        if (strncmp(ph_paths[i], norm, XNSU_PH_PATH_LEN) == 0) {
            goto out; // already present
        }
    }
    if (ph_path_count >= XNSU_PH_MAX_PATHS) {
        ret = -ENOSPC;
        goto out;
    }
    strscpy(ph_paths[ph_path_count], norm, XNSU_PH_PATH_LEN);
    ph_path_count++;
out:
    spin_unlock_irqrestore(&ph_lock, flags);
    return ret;
}

int xnsu_path_hide_remove_path(const char *path)
{
    int i;
    unsigned long flags;
    char norm[XNSU_PH_PATH_LEN];

    if (!path) {
        return -EINVAL;
    }
    ph_normalize(path, norm, sizeof(norm));

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_path_count; i++) {
        if (strncmp(ph_paths[i], norm, XNSU_PH_PATH_LEN) == 0) {
            strscpy(ph_paths[i], ph_paths[ph_path_count - 1], XNSU_PH_PATH_LEN);
            ph_path_count--;
            break;
        }
    }
    spin_unlock_irqrestore(&ph_lock, flags);
    return 0;
}

void xnsu_path_hide_clear_paths(void)
{
    unsigned long flags;

    spin_lock_irqsave(&ph_lock, flags);
    ph_path_count = 0;
    spin_unlock_irqrestore(&ph_lock, flags);
}

int xnsu_path_hide_add_uid(u32 uid)
{
    u32 appid = XNSU_PH_APPID(uid);
    int i;
    int ret = 0;
    unsigned long flags;

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_uid_count; i++) {
        if (ph_uids[i] == appid) {
            goto out;
        }
    }
    if (ph_uid_count >= XNSU_PH_MAX_UIDS) {
        ret = -ENOSPC;
        goto out;
    }
    ph_uids[ph_uid_count++] = appid;
out:
    spin_unlock_irqrestore(&ph_lock, flags);
    // Re-evaluate marks so the newly targeted app is marked.
    xnsu_mark_running_process();
    return ret;
}

int xnsu_path_hide_remove_uid(u32 uid)
{
    u32 appid = XNSU_PH_APPID(uid);
    int i;
    unsigned long flags;

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_uid_count; i++) {
        if (ph_uids[i] == appid) {
            ph_uids[i] = ph_uids[--ph_uid_count];
            break;
        }
    }
    spin_unlock_irqrestore(&ph_lock, flags);
    xnsu_mark_running_process();
    return 0;
}

void xnsu_path_hide_clear_uids(void)
{
    unsigned long flags;

    spin_lock_irqsave(&ph_lock, flags);
    ph_uid_count = 0;
    spin_unlock_irqrestore(&ph_lock, flags);
    xnsu_mark_running_process();
}

void xnsu_path_hide_set_filter_system(bool on)
{
    ph_filter_system = on;
}

bool xnsu_path_hide_is_target(uid_t uid)
{
    u32 appid = XNSU_PH_APPID(uid);
    int i;
    bool found = false;
    unsigned long flags;

    if (!static_branch_unlikely(&xnsu_path_hide)) {
        return false;
    }

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_uid_count; i++) {
        if (ph_uids[i] == appid) {
            found = true;
            break;
        }
    }
    spin_unlock_irqrestore(&ph_lock, flags);
    return found;
}

static bool ph_path_match(const char *path)
{
    int i;
    bool match = false;
    unsigned long flags;

    spin_lock_irqsave(&ph_lock, flags);
    for (i = 0; i < ph_path_count; i++) {
        size_t len = strlen(ph_paths[i]);
        if (len == 0) {
            continue;
        }
        // exact match, or a directory prefix (entry followed by '/')
        if (strncmp(path, ph_paths[i], len) == 0 && (path[len] == '\0' || path[len] == '/')) {
            match = true;
            break;
        }
    }
    spin_unlock_irqrestore(&ph_lock, flags);
    return match;
}

// Resolve a syscall's (dfd, raw filename) into a normalized absolute path.
// Absolute filenames are just normalized; relative ones are joined onto the
// directory named by dfd (a real fd via fget, or the cwd for AT_FDCWD). This is
// what lets hiding survive a file manager that navigates with dir-fd-relative
// openat(dirfd, "name") calls. Returns false when it cannot be resolved.
static bool ph_resolve(int dfd, const char *raw, char *out, size_t outlen)
{
    char dirbuf[XNSU_PH_PATH_LEN];
    char joined[XNSU_PH_PATH_LEN];
    char *dpath;
    struct path pwd;
    struct file *f;

    if (raw[0] == '/') {
        ph_normalize(raw, out, outlen);
        return true;
    }

    if (dfd == AT_FDCWD) {
        get_fs_pwd(current->fs, &pwd);
        dpath = d_path(&pwd, dirbuf, sizeof(dirbuf));
        path_put(&pwd);
    } else {
        f = fget(dfd);
        if (!f) {
            return false;
        }
        dpath = d_path(&f->f_path, dirbuf, sizeof(dirbuf));
        fput(f);
    }

    if (IS_ERR(dpath)) {
        return false;
    }

    snprintf(joined, sizeof(joined), "%s/%s", dpath, raw);
    ph_normalize(joined, out, outlen);
    return true;
}

bool xnsu_path_hide_should_hide(int *dfd, const char __user **filename_user)
{
    u32 uid;
    char raw[XNSU_PH_PATH_LEN];
    char abs[XNSU_PH_PATH_LEN];
    const char __user *fn;
    long len;

    if (!static_branch_unlikely(&xnsu_path_hide)) {
        return false;
    }
    // Never hide anything from init.
    if (current->pid == 1) {
        return false;
    }

    uid = current_uid().val;
    if (!xnsu_path_hide_is_target(uid)) {
        return false;
    }

    if (!filename_user || !*filename_user) {
        return false;
    }
    fn = (const char __user *)untagged_addr((unsigned long)*filename_user);
    len = strncpy_from_user(raw, fn, sizeof(raw));
    if (len < 0) {
        return false;
    }
    raw[sizeof(raw) - 1] = '\0';

    if (!ph_resolve(dfd ? *dfd : AT_FDCWD, raw, abs, sizeof(abs))) {
        return false;
    }

    return ph_path_match(abs);
}

bool xnsu_path_hide_should_filter_dents(void)
{
    bool has_paths;
    unsigned long flags;

    if (!static_branch_unlikely(&xnsu_path_hide)) {
        return false;
    }
    if (current->pid == 1) {
        return false;
    }
    if (!xnsu_path_hide_is_target(current_uid().val)) {
        return false;
    }
    spin_lock_irqsave(&ph_lock, flags);
    has_paths = ph_path_count > 0;
    spin_unlock_irqrestore(&ph_lock, flags);
    return has_paths;
}

// Filter one getdents64 result buffer in place, dropping entries whose absolute
// path (this directory's path + "/" + entry name) matches a hidden path. Returns
// the new byte count (<= total). On any error or structural anomaly it returns
// the original total unchanged, so a failure never corrupts the dirent stream.
long xnsu_path_hide_filter_getdents64(unsigned int fd, void __user *dirp, long total)
{
    struct file *f;
    char *dirbuf;
    char *kbuf;
    char *dpath;
    char dirnorm[XNSU_PH_PATH_LEN];
    char full[XNSU_PH_PATH_LEN];
    long off, wr;
    bool bail = false;

    if (total <= 0 || total > XNSU_PH_DENTS_MAX) {
        return total;
    }

    f = fget(fd);
    if (!f) {
        return total;
    }
    dirbuf = kmalloc(XNSU_PH_PATH_LEN, GFP_KERNEL);
    if (!dirbuf) {
        fput(f);
        return total;
    }
    dpath = d_path(&f->f_path, dirbuf, XNSU_PH_PATH_LEN);
    fput(f);
    if (IS_ERR(dpath)) {
        kfree(dirbuf);
        return total;
    }
    ph_normalize(dpath, dirnorm, sizeof(dirnorm));
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
        struct xnsu_dirent64 *d = (struct xnsu_dirent64 *)(kbuf + off);
        unsigned short reclen;
        bool hide = false;

        if (off + (long)offsetof(struct xnsu_dirent64, d_name) > total) {
            bail = true;
            break;
        }
        reclen = d->d_reclen;
        if (reclen < offsetof(struct xnsu_dirent64, d_name) || off + reclen > total) {
            bail = true;
            break;
        }

        if (d->d_name[0] != '\0' && strcmp(d->d_name, ".") != 0 && strcmp(d->d_name, "..") != 0) {
            snprintf(full, sizeof(full), "%s/%s", dirnorm, d->d_name);
            hide = ph_path_match(full);
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

static int path_hide_feature_get(u64 *value)
{
    *value = static_key_enabled(&xnsu_path_hide) ? 1 : 0;
    return 0;
}

static int path_hide_feature_set(u64 value)
{
    if (value) {
        static_key_enable(&xnsu_path_hide.key);
    } else {
        static_key_disable(&xnsu_path_hide.key);
    }
    pr_info("path_hide: set to %llu\n", value);
    // Mark/unmark target apps to match the new state.
    xnsu_mark_running_process();
    return 0;
}

static const struct xnsu_feature_handler path_hide_handler = {
    .feature_id = XNSU_FEATURE_PATH_HIDE,
    .name = "path_hide",
    .get_handler = path_hide_feature_get,
    .set_handler = path_hide_feature_set,
};

void __init xnsu_path_hide_init(void)
{
    if (xnsu_register_feature_handler(&path_hide_handler)) {
        pr_err("Failed to register path_hide feature handler\n");
    }
}

void __exit xnsu_path_hide_exit(void)
{
    xnsu_unregister_feature_handler(XNSU_FEATURE_PATH_HIDE);
}
