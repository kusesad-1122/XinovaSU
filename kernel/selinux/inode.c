#include <linux/errno.h>
#include <linux/err.h>
#include <linux/fs.h>
#include <linux/uaccess.h>

#include "objsec.h"
#include "selinux/selinux.h"
#include "infra/btf_parser.h"
#include "infra/symbol_resolver.h"
#include "klog.h"

#if defined(MODULE) && defined(CONFIG_DEBUG_INFO_BTF)
#include <uapi/linux/btf.h>

static u32 inode_blob_offset __ro_after_init;

/*
 * 6.6.144 inserted lbs_backing_file before lbs_inode in struct
 * lsm_blob_sizes, which invalidates every compile-time selinux_inode()
 * offset and can panic. Resolve lbs_inode's offset from vmlinux BTF and
 * read the live blob displacement at init time.
 *
 * selinux_blob_sizes is an internal kernel symbol and is not guaranteed to
 * be exported to modules, so take its address through the kallsyms resolver
 * instead of referencing it at link time.
 */
int __init xnsu_selinux_inode_init(void)
{
    struct xnsu_btf_field blob_field;
    unsigned long blob_sizes_addr;
    s32 blob_offset;
    int ret;

    ret = xnsu_btf_field("lsm_blob_sizes", "lbs_inode", BTF_KIND_INT, sizeof(int), &blob_field);
    if (ret)
        goto fail;

    blob_sizes_addr = find_kernel_symbol_exact("selinux_blob_sizes");
    if (!blob_sizes_addr) {
        pr_err("selinux_blob_sizes unresolved via kallsyms\n");
        ret = -ENOENT;
        goto fail;
    }

    ret =
        copy_from_kernel_nofault(&blob_offset, (const char *)blob_sizes_addr + blob_field.offset, sizeof(blob_offset));
    if (ret) {
        pr_err("selinux_blob_sizes.lbs_inode unreadable (%d)\n", ret);
        goto fail;
    }
    if (blob_offset < 0 || blob_offset % __alignof__(struct inode_security_struct)) {
        ret = -EINVAL;
        goto fail;
    }
    inode_blob_offset = blob_offset;
    pr_info("offset from vmlinux.btf, lbs_inode@%u, blob=%u\n", blob_field.offset, inode_blob_offset);
    return 0;
fail:
    pr_err("vmlinux BTF (%d) unreadable!\n", ret);
    return ret;
}
#endif

struct inode_security_struct *xnsu_selinux_inode(const struct inode *inode)
{
#if defined(MODULE) && defined(CONFIG_DEBUG_INFO_BTF)
    struct inode_security_struct *security;

    if (!inode->i_security)
        return NULL;
    security = (void *)((char *)inode->i_security + inode_blob_offset);
    if (READ_ONCE(security->inode) != inode) {
        pr_err("Can not resolve inode_security_struct offset, current offset got: %d\n", inode_blob_offset);
        return ERR_PTR(-EINVAL);
    }
    return security;
#else
    /* In-tree builds have the real selinux_inode() with the correct layout. */
    return selinux_inode(inode);
#endif
}
