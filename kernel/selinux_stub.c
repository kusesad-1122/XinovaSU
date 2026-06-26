// SPDX-License-Identifier: GPL-2.0-or-later
/*
 * XinovaSU SELinux stub — GKI unexported symbol workaround.
 * Temporary no-op replacements until runtime resolution is implemented.
 */
#include <linux/types.h>

/* sepolicy.c stubs */
bool xnsu_type(const char *type, const char *attr) { return true; }
bool xnsu_attribute(const char *attr) { return true; }
bool xnsu_permissive(const char *type) { return true; }
bool xnsu_enforce(const char *type) { return true; }
bool xnsu_typeattribute(const char *type, const char *attr) { return true; }
bool xnsu_exists(const char *type) { return false; }
bool xnsu_allow(const char *s, const char *t, const char *c, const char *p) { return true; }
bool xnsu_deny(const char *s, const char *t, const char *c, const char *p) { return true; }
bool xnsu_auditallow(const char *s, const char *t, const char *c, const char *p) { return true; }
bool xnsu_dontaudit(const char *s, const char *t, const char *c, const char *p) { return true; }
bool xnsu_allowxperm(const char *s, const char *t, const char *c, const char *r) { return true; }
bool xnsu_auditallowxperm(const char *s, const char *t, const char *c, const char *r) { return true; }
bool xnsu_dontauditxperm(const char *s, const char *t, const char *c, const char *r) { return true; }
bool xnsu_type_transition(const char *s, const char *t, const char *c, const char *d, const char *o) { return true; }
bool xnsu_type_change(const char *s, const char *t, const char *c, const char *d) { return true; }
bool xnsu_type_member(const char *s, const char *t, const char *c, const char *d) { return true; }
bool xnsu_genfscon(const char *fs, const char *path, const char *ctx) { return true; }
void xnsu_destroy_sepolicy(void *p) { }
void *xnsu_dup_sepolicy(void *p) { return NULL; }
int  xnsu_sepolicy_symbols_init(void) { return 0; }

/* selinux_hide.c stubs */
void xnsu_selinux_hide_init(void) { }
void xnsu_selinux_hide_exit(void) { }
void xnsu_selinux_hide_drop_backup_if_unused(void) { }
void xnsu_selinux_hide_handle_second_stage(void) { }
void xnsu_selinux_hide_handle_post_fs_data(void) { }
