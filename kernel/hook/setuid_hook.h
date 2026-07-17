#ifndef __XNSU_H_XNSU_CORE
#define __XNSU_H_XNSU_CORE

#include <linux/init.h>
#include <linux/types.h>

void xnsu_setuid_hook_init(void);
void xnsu_setuid_hook_exit(void);

// Handler functions for hook_manager
int xnsu_handle_setresuid(uid_t old_uid, uid_t new_uid);

#endif
