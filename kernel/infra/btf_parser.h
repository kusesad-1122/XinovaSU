#ifndef __XNSU_BTF_PARSER_H
#define __XNSU_BTF_PARSER_H

#include <linux/types.h>

#if defined(MODULE) && defined(CONFIG_DEBUG_INFO_BTF)

struct xnsu_btf_field {
    u32 offset;
    u32 struct_size;
};

/*
 * Resolve the offset of `member` inside `structure` from vmlinux BTF rather
 * than trusting a compile-time struct layout. GKI kernels from android13-5.15
 * onwards ship CONFIG_DEBUG_INFO_BTF; custom kernels without it are expected
 * to build intree and take the selinux_inode() branch in selinux/inode.c.
 */
int xnsu_btf_field(const char *structure, const char *member, u32 kind, u32 size, struct xnsu_btf_field *field);
#endif
#endif
