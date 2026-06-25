#ifndef __XNSU_H_KLOG
#define __XNSU_H_KLOG

#include <linux/printk.h>

#ifdef pr_fmt
#undef pr_fmt
#define pr_fmt(fmt) "XinovaSU: " fmt
#endif

#endif
