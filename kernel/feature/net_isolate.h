#ifndef __XNSU_H_NET_ISOLATE
#define __XNSU_H_NET_ISOLATE

#include <linux/types.h>

/*
 * Per-app network isolation. UIDs are normalised to appid space (uid % 100000)
 * so an app is blocked across all Android users. The master switch is the
 * XNSU_FEATURE_NET_ISOLATE feature toggle; the blocklist is managed here.
 */
int xnsu_net_isolate_add(u32 uid);

int xnsu_net_isolate_remove(u32 uid);

void xnsu_net_isolate_clear(void);

void xnsu_net_isolate_init(void);

void xnsu_net_isolate_exit(void);

#endif // __XNSU_H_NET_ISOLATE
