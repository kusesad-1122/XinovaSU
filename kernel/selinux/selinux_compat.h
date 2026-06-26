#ifndef __XNSU_SELINUX_COMPAT_H
#define __XNSU_SELINUX_COMPAT_H

/*
 * SELinux internal function pointers — resolved at runtime via
 * find_kernel_symbol_exact().  Shared by sepolicy.c and selinux_hide.c and selinux_hide.c
 * to eliminate direct calls that would trigger GKI CRC checks.
 *
 * Declared extern here; defined (non-static) and resolved in sepolicy.c
 * via xnsu_sepolicy_symbols_init().
 */

#include <linux/types.h>
#include <linux/stddef.h>

/* Forward declarations for pointer types */
struct avtab;
struct avtab_key;
struct avtab_node;
struct avtab_datum;
struct av_decision;
struct sidtab;
struct sidtab_entry;
struct ebitmap;
struct common_audit_data;
struct selinux_state;
struct policydb;
struct context;
struct extended_perms;

/* sepolicy / avtab */
extern struct avtab_node *(*p_avtab_search_node)(struct avtab *, const struct avtab_key *);
extern int (*p_avtab_alloc)(struct avtab *, u32);
extern void (*p_avtab_destroy)(struct avtab *);
extern struct avtab_node *(*p_avtab_search_node_next)(struct avtab_node *, int);
extern int (*p_avtab_insert_nonunique)(struct avtab *, const struct avtab_key *, const struct avtab_datum *);

/* PID / process */
extern struct task_struct *(*p_change_pid)(struct task_struct *, enum pid_type, struct pid *);

/* AVC */
extern int (*p_avc_has_perm)(u32, u32, u16, u32, struct common_audit_data *);

/* sidtab */
extern void (*p_sidtab_destroy)(struct sidtab *);
extern int (*p_sidtab_context_to_sid)(struct sidtab *, struct context *, u32 *);
extern struct sidtab_entry *(*p_sidtab_search_entry)(struct sidtab *, const u32);
extern int (*p_sidtab_sid2str_get)(struct sidtab *, struct sidtab_entry *, u32 *, char **);
extern void (*p_sidtab_sid2str_put)(struct sidtab *, struct sidtab_entry *, char *);

/* ebitmap */
extern void (*p_ebitmap_destroy)(struct ebitmap *);
extern int (*p_ebitmap_cmp)(const struct ebitmap *, const struct ebitmap *);
extern int (*p_ebitmap_contains)(const struct ebitmap *, const struct ebitmap *, u32);

/* MLS */
extern int (*p_mls_context_to_sid)(struct policydb *, char, char *, struct context *, struct sidtab *, u32);
extern int (*p_mls_compute_context_len)(const struct policydb *, const struct context *);
extern int (*p_mls_sid_to_context)(const struct policydb *, const struct context *, char **, char *);

/* policydb */
extern int (*p_policydb_context_isvalid)(const struct policydb *, const struct context *);

/* conditional */
extern void (*p_cond_compute_av)(struct avtab *, struct avtab_key *, struct av_decision *, struct extended_perms *);

/* Global flag: set when ALL symbols resolved in xnsu_sepolicy_symbols_init() */
extern bool xnsu_sepolicy_ops_available;

#endif /* __XNSU_SELINUX_COMPAT_H */
