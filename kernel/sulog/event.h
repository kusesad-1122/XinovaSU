#ifndef __XNSU_H_SULOG_EVENT
#define __XNSU_H_SULOG_EVENT

#include <linux/compiler_types.h>
#include <linux/gfp.h>
#include <linux/types.h>
#include "uapi/sulog.h" // IWYU pragma: keep

struct xnsu_event_queue;
struct xnsu_sulog_pending_event;

int xnsu_sulog_events_init(void);
void xnsu_sulog_events_exit(void);

struct xnsu_sulog_pending_event *xnsu_sulog_capture_root_execve(const char __user *filename_user,
                                                                const char __user *const __user *argv_user, gfp_t gfp);
struct xnsu_sulog_pending_event *xnsu_sulog_capture_sucompat(const char __user *filename_user,
                                                             const char __user *const __user *argv_user, gfp_t gfp);
void xnsu_sulog_emit_pending(struct xnsu_sulog_pending_event *pending, int retval, gfp_t gfp);
int xnsu_sulog_emit_grant_root(int retval, __u32 uid, __u32 euid, gfp_t gfp);

struct xnsu_event_queue *xnsu_sulog_get_queue(void);

#endif
