"""
对 miuix 0.9.1 -> 0.9.3 做签名级 diff，聚焦项目真正用到的高风险组件。

为什么需要：verify_miuix_upgrade.py 只能确认「符号还在」，
但 0.9.x 的多处破坏性变更是**参数签名**层面的（插入位置参数、
改名、默认值变化），符号名不变。用 class 常量池里的方法描述符
（形如 (Landroidx/...;Landroidx/...;)V）做前后对比，可以直接看到差异。

范围只覆盖项目实际使用的组件，避免对 111 个符号全量展开。
"""

import io
import os
import re
import sys
import zipfile

CMP = os.environ.get('MIUIX_CMP_DIR', 'D:/xinovasu/.workbuddy/miuix-cmp')

# 项目用到的高风险组件 -> (Kt 文件名, 关注的类前缀)
# 关注原因见 docs/MIUIX_UPGRADE_PLAN.md：0.9.3 改过 NavigationRail(
# mode -> state)、SnackbarColors(新增 actionContainerColor)、
# TextButton(位置参数前插入 textStyle)。
TARGETS = {
    'NavigationRail': ('NavigationRailKt', 'top/yukonga/miuix/kmp/basic'),
    'NavigationBar': ('NavigationBarKt', 'top/yukonga/miuix/kmp/basic'),
    'Snackbar': ('SnackbarKt', 'top/yukonga/miuix/kmp/basic'),
    'TextButton': ('TextButtonKt', 'top/yukonga/miuix/kmp/basic'),
    'PullToRefresh': ('PullToRefreshKt', 'top/yukonga/miuix/kmp/basic'),
    'TopAppBar': ('TopAppBarKt', 'top/yukonga/miuix/kmp/basic'),
    'TabRow': ('TabRowKt', 'top/yukonga/miuix/kmp/basic'),
}


def load_class(version, cls_simple, pkg_prefix):
    """Return raw bytes of a class from the given version's AARs."""
    for mod in ['miuix-ui-android', 'miuix-preference-android', 'miuix-blur-android']:
        aar = os.path.join(CMP, version, mod + '.aar')
        if not os.path.exists(aar):
            continue
        try:
            z = zipfile.ZipFile(aar)
            jar = zipfile.ZipFile(io.BytesIO(z.read('classes.jar')))
        except Exception:
            continue
        for name in jar.namelist():
            if name.endswith('/%s.class' % cls_simple) and pkg_prefix in name:
                return jar.read(name)
    return None


def methods(data):
    """Extract (name, descriptor) pairs from a class file's constant pool."""
    if not data:
        return set()
    found = set()
    # method refs look like: 0x0a name_index desc_index
    text = data.decode('latin1')
    # crude but effective: find all UTF8 entries that look like descriptors
    for s in re.findall(r'[\x20-\x7e]{3,}', data.decode('latin1')):
        pass
    # Better: walk the constant pool properly.
    i = 10  # magic(4) minor(2) major(2) cp_count(2)
    n = int.from_bytes(data[8:10], 'big')
    pool = {}
    idx = 1
    while idx < n and i < len(data):
        tag = data[i]
        i += 1
        if tag == 1:  # UTF8
            ln = int.from_bytes(data[i:i + 2], 'big')
            pool[idx] = data[i + 2:i + 2 + ln].decode('utf-8', 'replace')
            i += 2 + ln
        elif tag in (7, 8, 16, 19, 20):
            i += 2
        elif tag == 15:
            i += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            i += 4
        elif tag in (5, 6):
            i += 8
            idx += 1
        else:
            break
        idx += 1
    # second pass: Methodref/Fieldref point at (name_index, desc_index)
    i = 10
    idx = 1
    while idx < n and i < len(data):
        tag = data[i]
        i += 1
        if tag == 1:
            ln = int.from_bytes(data[i:i + 2], 'big')
            i += 2 + ln
        elif tag in (7, 8, 16, 19, 20):
            name_i = int.from_bytes(data[i:i + 2], 'big')
            desc_i = int.from_bytes(data[i + 2:i + 4], 'big')
            nm = pool.get(name_i, '')
            ds = pool.get(desc_i, '')
            if ds.startswith('('):
                found.add((nm, ds))
            i += 2
        elif tag == 15:
            i += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            i += 4
        elif tag in (5, 6):
            i += 8
            idx += 1
        else:
            break
        idx += 1
    return found


def main():
    changed_any = False
    for label, (cls, pkg) in TARGETS.items():
        d91 = load_class('0.9.1', cls, pkg)
        d93 = load_class('0.9.3', cls, pkg)
        if d91 is None and d93 is None:
            print('  SKIP  %-18s （两版均未找到 %s）' % (label, cls))
            continue
        m91 = methods(d91)
        m93 = methods(d93)
        only91 = m91 - m93
        only93 = m93 - m91
        if not only91 and not only93:
            print('  SAME  %-18s %d 个签名无变化' % (label, len(m91 & m93)))
        else:
            changed_any = True
            print('  DIFF  %-18s' % label)
            for nm, ds in sorted(only91):
                print('      - %s%s' % (nm, ds))
            for nm, ds in sorted(only93):
                print('      + %s%s' % (nm, ds))
    print()
    if changed_any:
        print('RESULT: 存在签名差异，需逐项确认是否为位置参数移位/改名')
        return 1
    print('RESULT: 目标组件的公开签名在 0.9.1 -> 0.9.3 间无变化')
    return 0


if __name__ == '__main__':
    sys.exit(main())
