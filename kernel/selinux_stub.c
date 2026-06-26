// SPDX-License-Identifier: GPL-2.0-or-later
/*
 * XinovaSU SELinux stub — temporary replacement while
 * GKI symbol resolution is pending.
 * All functions return safe no-op values.
 *
 * Covers all symbols previously defined in:
 *   - kernel/selinux/sepolicy.c
 *   - kernel/selinux/selinux.c
 *   - kernel/selinux/rules.c
 */
#include <linux/types.h>
#include <linux/errno.h>
#include <linux/cred.h>
#include <linux/uaccess.h>

#include "selinux/sepolicy.h"

/* ───── sepolicy.c stubs ───── */

bool xnsu_type(struct policydb *db, const char *name, const char *attr) { (void)db; return true; }
bool xnsu_attribute(struct policydb *db, const char *name) { (void)db; return true; }
bool xnsu_permissive(struct policydb *db, const char *type) { (void)db; return true; }
bool xnsu_enforce(struct policydb *db, const char *type) { (void)db; return true; }
bool xnsu_typeattribute(struct policydb *db, const char *type, const char *attr) { (void)db; return true; }
bool xnsu_exists(struct policydb *db, const char *type) { (void)db; return false; }
bool xnsu_allow(struct policydb *db, const char *s, const char *t, const char *c, const char *p) { (void)db; return true; }
bool xnsu_deny(struct policydb *db, const char *s, const char *t, const char *c, const char *p) { (void)db; return true; }
bool xnsu_auditallow(struct policydb *db, const char *s, const char *t, const char *c, const char *p) { (void)db; return true; }
bool xnsu_dontaudit(struct policydb *db, const char *s, const char *t, const char *c, const char *p) { (void)db; return true; }
bool xnsu_allowxperm(struct policydb *db, const char *s, const char *t, const char *c, const char *r) { (void)db; return true; }
bool xnsu_auditallowxperm(struct policydb *db, const char *s, const char *t, const char *c, const char *r) { (void)db; return true; }
bool xnsu_dontauditxperm(struct policydb *db, const char *s, const char *t, const char *c, const char *r) { (void)db; return true; }
bool xnsu_type_transition(struct policydb *db, const char *s, const char *t, const char *c, const char *d, const char *o) { (void)db; return true; }
bool xnsu_type_change(struct policydb *db, const char *s, const char *t, const char *c, const char *d) { (void)db; return true; }
bool xnsu_type_member(struct policydb *db, const char *s, const char *t, const char *c, const char *d) { (void)db; return true; }
bool xnsu_genfscon(struct policydb *db, const char *fs, const char *path, const char *ctx) { (void)db; return true; }
void xnsu_destroy_sepolicy(struct selinux_policy *pol) { (void)pol; }
struct selinux_policy *xnsu_dup_sepolicy(struct selinux_policy *old_pol) { (void)old_pol; return NULL; }
int  xnsu_sepolicy_symbols_init(void) { return 0; }

/* ───── selinux.c stubs ───── */

void setup_selinux(const char *domain, struct cred *cred) { (void)domain; (void)cred; }
void setup_xnsu_cred(void) { }
void setenforce(bool enforce) { (void)enforce; }
bool getenforce(void) { return false; }
void cache_sid(void) { }
bool is_task_xnsu_domain(const struct cred *cred) { (void)cred; return false; }
bool is_xnsu_domain(void) { return false; }
bool is_zygote(const struct cred *cred) { (void)cred; return false; }
bool is_init(const struct cred *cred) { (void)cred; return false; }
void escape_to_root_for_adb_root(void) { }
u32 xnsu_file_sid = 0;

/* ───── rules.c stubs ───── */

void apply_xinovasu_rules(void) { }
int handle_sepolicy(void __user *user_data, u64 data_len) { (void)user_data; (void)data_len; return 0; }

/* ───── selinux_hide.c stubs ───── */

void xnsu_selinux_hide_init(void) { }
void xnsu_selinux_hide_exit(void) { }
void xnsu_selinux_hide_drop_backup_if_unused(void) { }
void xnsu_selinux_hide_handle_second_stage(void) { }
void xnsu_selinux_hide_handle_post_fs_data(void) { }

/* ───── globals ───── */

struct selinux_policy *backup_sepolicy;
