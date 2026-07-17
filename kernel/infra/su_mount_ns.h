#ifndef __XNSU_SU_MOUNT_NS_H
#define __XNSU_SU_MOUNT_NS_H

#include <linux/types.h>

#define XNSU_NS_INHERITED 0
#define XNSU_NS_GLOBAL 1
#define XNSU_NS_INDIVIDUAL 2

struct xnsu_mns_tw {
    struct callback_head cb;
    int32_t ns_mode;
};

void setup_mount_ns(int32_t ns_mode);

#endif
