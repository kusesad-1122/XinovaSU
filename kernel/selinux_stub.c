// SPDX-License-Identifier: GPL-2.0-or-later
/*
 * XinovaSU SELinux stub — GKI unexported symbol workaround.
 * Temporary no-op replacements until runtime resolution is implemented.
 */
#include "selinux/sepolicy.h"
#include "feature/selinux_hide.h"

/* sepolicy.c stubs — exact signatures from sepolicy.h */
struct selinux_policy *xnsu_dup_sepolicy(struct selinux_policy *old_pol) { return NULL; }
void xnsu_destroy_sepolicy(struct selinux_policy *orig) { }

bool xnsu_type(struct policydb *db, const char *name, const char *attr) { return true; }
bool xnsu_attribute(struct policydb *db, const char *name) { return true; }
bool xnsu_permissive(struct policydb *db, const char *type) { return true; }
bool xnsu_enforce(struct policydb *db, const char *type) { return true; }
bool xnsu_typeattribute(struct policydb *db, const char *type, const char *attr) { return true; }
bool xnsu_exists(struct policydb *db, const char *type) { return false; }

bool xnsu_allow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm) { return true; }
bool xnsu_deny(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm) { return true; }
bool xnsu_auditallow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm) { return true; }
bool xnsu_dontaudit(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm) { return true; }

bool xnsu_allowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range) { return true; }
bool xnsu_auditallowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range) { return true; }
bool xnsu_dontauditxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range) { return true; }

bool xnsu_type_transition(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def, const char *obj) { return true; }
bool xnsu_type_change(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def) { return true; }
bool xnsu_type_member(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def) { return true; }

bool xnsu_genfscon(struct policydb *db, const char *fs_name, const char *path, const char *ctx) { return true; }

int xnsu_sepolicy_symbols_init(void) { return 0; }

/* selinux_hide.c stubs — exact signatures from selinux_hide.h */
void xnsu_selinux_hide_init(void) { }
void xnsu_selinux_hide_exit(void) { }
void xnsu_selinux_hide_drop_backup_if_unused(void) { }
void xnsu_selinux_hide_handle_second_stage(void) { }
void xnsu_selinux_hide_handle_post_fs_data(void) { }
