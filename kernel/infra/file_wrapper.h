#ifndef XNSU_FILE_WRAPPER_H
#define XNSU_FILE_WRAPPER_H

#include <linux/file.h>
#include <linux/fs.h>

int xnsu_install_file_wrapper(int fd);
void xnsu_file_wrapper_init(void);

#endif // XNSU_FILE_WRAPPER_H
