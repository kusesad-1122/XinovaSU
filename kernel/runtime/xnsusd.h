#ifndef __XNSU_H_KSUD
#define __XNSU_H_KSUD

#include <asm/syscall.h>

#define KSUD_PATH "/data/adb/xnsusd"

void xnsu_xnsusd_init();
void xnsu_xnsusd_exit();

void xnsu_execve_hook_xnsusd(const struct pt_regs *regs);
void xnsu_stop_input_hook_runtime(void);

#endif
