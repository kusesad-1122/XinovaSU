#ifndef __XNSU_H_SELINUX
#define __XNSU_H_SELINUX

#include <linux/types.h>
#include <linux/version.h>
#include <linux/cred.h>

#define KERNEL_SU_DOMAIN "ksu"
#define KERNEL_SU_FILE "xnsu_file"

#define KERNEL_SU_CONTEXT "u:r:" KERNEL_SU_DOMAIN ":s0"
#define XNSU_FILE_CONTEXT "u:object_r:" KERNEL_SU_FILE ":s0"
#define ZYGOTE_CONTEXT "u:r:zygote:s0"
#define INIT_CONTEXT "u:r:init:s0"

void setup_selinux(const char *, struct cred *);

void setenforce(bool);

bool getenforce();

void cache_sid(void);

bool is_task_xnsu_domain(const struct cred *cred);

bool is_xnsu_domain();

bool is_zygote(const struct cred *cred);

bool is_init(const struct cred *cred);

void apply_xinovasu_rules();

int handle_sepolicy(void __user *user_data, u64 data_len);

void setup_xnsu_cred();

void escape_to_root_for_adb_root();

extern u32 xnsu_file_sid;

#endif
