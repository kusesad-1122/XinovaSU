#ifndef __XNSU_H_SYSCALL_EVENT_BRIDGE
#define __XNSU_H_SYSCALL_EVENT_BRIDGE

#include <asm/ptrace.h>

long xnsu_hook_newfstatat(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_faccessat(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_openat(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_getdents64(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_recvmsg(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_recvfrom(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_execve(int orig_nr, const struct pt_regs *regs);
long xnsu_hook_setresuid(int orig_nr, const struct pt_regs *regs);

void xnsu_stop_xnsusd_execve_hook(void);

#endif // __XNSU_H_SYSCALL_EVENT_BRIDGE
