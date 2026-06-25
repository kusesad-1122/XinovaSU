#ifndef __XNSU_H_SULOG
#define __XNSU_H_SULOG

#include <linux/types.h>

bool xnsu_sulog_is_enabled(void);
void xnsu_sulog_init(void);
void xnsu_sulog_exit(void);

#endif
