#ifndef __XNSU_H_FEATURE
#define __XNSU_H_FEATURE

#include <linux/types.h>
#include "uapi/feature.h" // IWYU pragma: keep

typedef int (*xnsu_feature_get_t)(u64 *value);
typedef int (*xnsu_feature_set_t)(u64 value);

struct xnsu_feature_handler {
    u32 feature_id;
    const char *name;
    xnsu_feature_get_t get_handler;
    xnsu_feature_set_t set_handler;
};

int xnsu_register_feature_handler(const struct xnsu_feature_handler *handler);

int xnsu_unregister_feature_handler(u32 feature_id);

int xnsu_get_feature(u32 feature_id, u64 *value, bool *supported);

int xnsu_set_feature(u32 feature_id, u64 value);

void xnsu_feature_init(void);

void xnsu_feature_exit(void);

#endif // __XNSU_H_FEATURE
