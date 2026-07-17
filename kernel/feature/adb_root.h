#ifndef __XNSU_H_ADB_ROOT
#define __XNSU_H_ADB_ROOT
#include <asm/ptrace.h>

long xnsu_adb_root_handle_execve(struct pt_regs *regs);

void xnsu_adb_root_init(void);

void xnsu_adb_root_exit(void);

#endif
