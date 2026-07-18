#ifndef __XNSU_H_PATH_HIDE
#define __XNSU_H_PATH_HIDE

#include <linux/types.h>

/*
 * Per-app path hiding. Selected app uids ("targets") get a marked flag so their
 * openat/newfstatat/faccessat syscalls reach the hooks; a matching hidden path
 * (exact or directory prefix) then returns -ENOENT to that app.
 *
 * v1 is uid-mode only (hide for the selected app uids). The master switch is
 * the XNSU_FEATURE_PATH_HIDE feature toggle. filter_system is stored but
 * reserved -- extending hiding to system/root processes requires marking them,
 * which is deferred for brick safety.
 */

int xnsu_path_hide_add_path(const char *path);
int xnsu_path_hide_remove_path(const char *path);
void xnsu_path_hide_clear_paths(void);

int xnsu_path_hide_add_uid(u32 uid);
int xnsu_path_hide_remove_uid(u32 uid);
void xnsu_path_hide_clear_uids(void);

void xnsu_path_hide_set_filter_system(bool on);

// True if uid's appid is a hiding target (used to keep the tracepoint mark on
// the target app so its file-access syscalls reach the hooks).
bool xnsu_path_hide_is_target(uid_t uid);

// True if the current task's access to *filename_user should be hidden
// (-ENOENT). Called from the openat/newfstatat/faccessat hook bodies. Resolves
// dir-fd-relative and sdcard-aliased paths before matching.
bool xnsu_path_hide_should_hide(int *dfd, const char __user **filename_user);

// True if the current task is a hiding target with the feature on and at least
// one hidden path configured. Cheap gate for the getdents64 hook.
bool xnsu_path_hide_should_filter_dents(void);

// Filter a getdents64 result buffer in place, removing directory entries whose
// absolute path matches a hidden path. `total` is the byte count the real
// syscall returned; returns the (<=) byte count after filtering. Returns
// `total` unchanged on any error, so it never corrupts the dirent stream.
long xnsu_path_hide_filter_getdents64(unsigned int fd, void __user *dirp, long total);

void xnsu_path_hide_init(void);
void xnsu_path_hide_exit(void);

#endif // __XNSU_H_PATH_HIDE
