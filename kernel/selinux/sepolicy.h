#ifndef __XNSU_H_SEPOLICY
#define __XNSU_H_SEPOLICY

#include <linux/types.h>

#include "ss/policydb.h"

struct selinux_policy *xnsu_dup_sepolicy(struct selinux_policy *old_pol);

void xnsu_destroy_sepolicy(struct selinux_policy *orig);

// Operation on types
bool xnsu_type(struct policydb *db, const char *name, const char *attr);
bool xnsu_attribute(struct policydb *db, const char *name);
bool xnsu_permissive(struct policydb *db, const char *type);
bool xnsu_enforce(struct policydb *db, const char *type);
bool xnsu_typeattribute(struct policydb *db, const char *type, const char *attr);
bool xnsu_exists(struct policydb *db, const char *type);

// Return 1 when the type value is from the initial policy, 0 when it is not, and -1 when metadata is unavailable
int xnsu_type_is_original(struct policydb *db, u32 policy_generation, u32 type_value);
bool xnsu_set_original_types(struct policydb *db, struct policydb *orig_db, u32 policy_generation);
bool xnsu_advance_original_types(struct policydb *db, struct policydb *old_db, u32 old_policy_generation,
                                 u32 policy_generation);

// Access vector rules
bool xnsu_allow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool xnsu_deny(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool xnsu_auditallow(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);
bool xnsu_dontaudit(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *perm);

// Extended permissions access vector rules
bool xnsu_allowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);
bool xnsu_auditallowxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);
bool xnsu_dontauditxperm(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *range);

// Type rules
bool xnsu_type_transition(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def,
                          const char *obj);
bool xnsu_type_change(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def);
bool xnsu_type_member(struct policydb *db, const char *src, const char *tgt, const char *cls, const char *def);

// File system labeling
bool xnsu_genfscon(struct policydb *db, const char *fs_name, const char *path, const char *ctx);

#endif
