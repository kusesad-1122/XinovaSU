#ifndef __XNSU_H_KERNEL_SPOOF
#define __XNSU_H_KERNEL_SPOOF

#include <linux/types.h>

/*
 * Apply or reset kernel uname (utsname) spoofing.
 *
 * op == 0: restore the original release/version captured on first use.
 * op == 1: overwrite release and/or version. A NULL or empty string leaves
 *          that field unchanged.
 */
int xnsu_kernel_spoof_apply(u8 op, const char *release, const char *version);

void xnsu_kernel_spoof_init(void);

void xnsu_kernel_spoof_exit(void);

#endif // __XNSU_H_KERNEL_SPOOF
