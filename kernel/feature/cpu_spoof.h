#ifndef __XNSU_H_CPU_SPOOF
#define __XNSU_H_CPU_SPOOF

#include <linux/types.h>

/*
 * CPU spoof — kernel-level redirection of hardware-identity files.
 *
 * Rules are <source path> -> <decoy path> pairs installed by ksud through
 * XNSU_IOCTL_SET_CPU_SPOOF. While the feature is enabled, getname_flags()
 * resolves the decoy for any reader, so /proc/cpuinfo, /proc/cmdline and
 * /proc/bootconfig can report spoofed hardware without touching the real
 * files or depending on the reader's privileges.
 *
 * The decoy files themselves are written by userspace and MUST remain
 * world-readable (0644): the open happens under the *caller's* credentials.
 */

/* Maximum length (incl. NUL) accepted for a source/decoy path. */
#define XNSU_CS_PATH_MAX 256

/* Maximum number of simultaneously installed rules. */
#define XNSU_CS_MAX_RULES 16

int xnsu_cpu_spoof_add_rule(const char *src, const char *dst);

int xnsu_cpu_spoof_remove_rule(const char *src);

void xnsu_cpu_spoof_clear_rules(void);

void xnsu_cpu_spoof_init(void);

void xnsu_cpu_spoof_exit(void);

#endif // __XNSU_H_CPU_SPOOF
