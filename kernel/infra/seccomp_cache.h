#ifndef __XNSU_H_KERNEL_COMPAT
#define __XNSU_H_KERNEL_COMPAT

#include <linux/fs.h>
#include <linux/version.h>

extern void xnsu_seccomp_clear_cache(struct seccomp_filter *filter, int nr);
extern void xnsu_seccomp_allow_cache(struct seccomp_filter *filter, int nr);

#endif
