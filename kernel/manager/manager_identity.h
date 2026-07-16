#ifndef __XNSU_H_MANAGER_IDENTITY
#define __XNSU_H_MANAGER_IDENTITY

#include <linux/cred.h>
#include <linux/types.h>

#define XNSU_INVALID_APPID -1
#define XNSU_PER_USER_RANGE 100000

#ifdef CONFIG_XNSU_DISABLE_MANAGER
static inline bool xnsu_is_manager_appid_valid()
{
    return true;
}

static inline bool is_manager()
{
    return current_uid().val == 0;
}

static inline bool is_uid_manager(uid_t uid)
{
    return uid == 0;
}

static inline uid_t xnsu_get_manager_appid()
{
    return 0;
}

static inline void xnsu_set_manager_appid(uid_t appid)
{
    (void)appid;
}

static inline void xnsu_invalidate_manager_uid()
{
}
#else
extern uid_t xnsu_manager_appid; // DO NOT DIRECT USE

static inline bool xnsu_is_manager_appid_valid()
{
    return xnsu_manager_appid != XNSU_INVALID_APPID;
}

static inline bool is_manager()
{
    return unlikely(xnsu_manager_appid == current_uid().val % XNSU_PER_USER_RANGE);
}

static inline bool is_uid_manager(uid_t uid)
{
    return unlikely(xnsu_manager_appid == uid % XNSU_PER_USER_RANGE);
}

static inline uid_t xnsu_get_manager_appid()
{
    return xnsu_manager_appid;
}

static inline void xnsu_set_manager_appid(uid_t appid)
{
    xnsu_manager_appid = appid;
}

static inline void xnsu_invalidate_manager_uid()
{
    xnsu_manager_appid = XNSU_INVALID_APPID;
}
#endif

#endif // __XNSU_H_MANAGER_IDENTITY
