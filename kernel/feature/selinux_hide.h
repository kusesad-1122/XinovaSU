#ifndef __XNSU_H_SELINUX_HIDE
#define __XNSU_H_SELINUX_HIDE

void xnsu_selinux_hide_init();
void xnsu_selinux_hide_exit();
void xnsu_selinux_hide_drop_backup_if_unused();
void xnsu_selinux_hide_handle_second_stage();
void xnsu_selinux_hide_handle_post_fs_data();

#endif
