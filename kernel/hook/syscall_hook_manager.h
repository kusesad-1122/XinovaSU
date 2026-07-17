#ifndef __XNSU_H_HOOK_MANAGER
#define __XNSU_H_HOOK_MANAGER

#include <asm/ptrace.h>

// Hook manager initialization and cleanup
void xnsu_syscall_hook_manager_init(void);
void xnsu_syscall_hook_manager_exit(void);

#endif
