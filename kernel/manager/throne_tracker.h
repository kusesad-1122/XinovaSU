#ifndef __XNSU_H_UID_OBSERVER
#define __XNSU_H_UID_OBSERVER

#include <linux/types.h>
#ifdef CONFIG_XNSU_DISABLE_MANAGER
static inline void xnsu_throne_tracker_init()
{
}

static inline void xnsu_throne_tracker_exit()
{
}

static inline void track_throne(bool prune_only)
{
    (void)prune_only;
}
#else
void xnsu_throne_tracker_init();

void xnsu_throne_tracker_exit();

void track_throne(bool prune_only);
#endif

#endif
