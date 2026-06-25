#ifndef __XNSU_H_MANAGER_OBSERVER
#define __XNSU_H_MANAGER_OBSERVER

#ifdef CONFIG_XNSU_DISABLE_MANAGER
static inline int xnsu_observer_init(void)
{
    return 0;
}

static inline void xnsu_observer_exit(void)
{
}
#else
int xnsu_observer_init(void);
void xnsu_observer_exit(void);
#endif

#endif // __XNSU_H_MANAGER_OBSERVER
