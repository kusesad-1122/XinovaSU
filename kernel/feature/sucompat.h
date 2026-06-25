#ifndef __XNSU_H_SUCOMPAT
#define __XNSU_H_SUCOMPAT
#include <asm/ptrace.h>
#include <linux/types.h>

extern bool xnsu_su_compat_enabled;

void xnsu_sucompat_init(void);
void xnsu_sucompat_exit(void);

// Handler functions exported for hook_manager
int xnsu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode, int *__unused_flags);
int xnsu_handle_stat(int *dfd, const char __user **filename_user, int *flags);
long xnsu_handle_execve_sucompat(const char __user **filename_user, int orig_nr, const struct pt_regs *regs);

#endif