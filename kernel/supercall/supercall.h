#ifndef __XNSU_H_SUPERCALL
#define __XNSU_H_SUPERCALL

#include <linux/types.h>
#include <linux/uaccess.h>

// IOCTL handler types
typedef int (*xnsu_ioctl_handler_t)(void __user *arg);
typedef bool (*xnsu_perm_check_t)(void);

// IOCTL command mapping
struct xnsu_ioctl_cmd_map {
    unsigned int cmd;
    const char *name;
    xnsu_ioctl_handler_t handler;
    xnsu_perm_check_t perm_check; // Permission check function
};

// Install KSU fd to current process
int xnsu_install_fd(void);

void xnsu_supercalls_init(void);
void xnsu_supercalls_exit(void);
#endif // __XNSU_H_SUPERCALL
