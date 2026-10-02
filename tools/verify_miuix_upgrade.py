"""
比对 miuix 0.9.1 与 0.9.3 的二进制，验证项目实际用到的符号是否仍然存在。

背景：升级依赖前不能只读 changelog —— 上一次就是因为只搜源码 import 就下结论
（"miuix-navigation3-ui 零引用"），被 CI 证伪。typealias 桥接会让被桥接类型
在源码里完全不带提供方包名，只有反查二进制才可靠。

做法：
  1. 解出两版各模块 AAR 的 classes.jar，收集 class 路径 + 常量池里的标识符；
  2. 从项目源码提取所有 top.yukonga.miuix.kmp.* 的 import 符号；
  3. 逐个检查该符号在 0.9.3 里是否仍存在，缺失的即为潜在破坏点。

Kotlin 的符号在 class 文件里有三种落点，缺一不可：
  - 顶层函数/属性 -> <File>Kt.class（如 BasicComponentKt）
  - 类/对象/接口  -> <Name>.class 或 <Name>$...class
  - typealias     -> 被桥接类型的 class 里不留提供方包名（这正是上次的坑）
所以这里同时收集 class 路径与常量池字符串，两者合并判断。
"""

import io
import os
import re
import sys
import zipfile

# AAR 目录。允许用环境变量 MIUIX_CMP_DIR 覆盖，因为 Windows Python
# 不认 MSYS/Git-Bash 的 /tmp 路径（那会导致静默读到 0 个符号，
# 从而给出"没有破坏点"的假结论）。
CMP = os.environ.get('MIUIX_CMP_DIR', 'm3cmp')
SRC = os.environ.get('MIUIX_SRC_DIR', 'D:/xinovasu/manager/app/src/main/java/com/xinsu/moe/')

MODULES = ['miuix-ui-android', 'miuix-blur-android',
           'miuix-preference-android', 'miuix-icons-android',
           # miuix-shader 必须包含：0.9.2 起 blur 把 isRenderEffectSupported
           # 等符号搬到了这里。漏掉它会导致「符号在 0.9.3 仍存在」的假结论
           # （符号确实存在，只是在另一个模块里，而项目 import 的是旧路径）。
           'miuix-shader-android',
           'miuix-navigation3-ui-android']


def collect(version):
    """Collect identifiers from all modules of one version.

    A module that doesn't exist for this version is skipped, not fatal:
    miuix-shader-android first shipped in 0.9.2, so it is legitimately absent
    in 0.9.1. Only a *total* absence of any readable module is an error.
    """
    pool = set()
    vdir = os.path.join(CMP, version)
    if not os.path.isdir(vdir):
        print('  ERROR 目录不存在: %s' % vdir)
        return None
    readable = 0
    for mod in MODULES:
        aar = os.path.join(vdir, mod + '.aar')
        if not os.path.exists(aar):
            print('  skip  %-30s （该版本无此模块）' % mod)
            continue
        try:
            z = zipfile.ZipFile(aar)
            jar = zipfile.ZipFile(io.BytesIO(z.read('classes.jar')))
        except Exception as exc:
            print('  skip  %-30s (%s)' % (mod, type(exc).__name__))
            continue
        readable += 1
        for name in jar.namelist():
            if not name.endswith('.class'):
                continue
            stem = name[:-len('.class')]
            pool.add(stem)
            pool.add(stem.rsplit('/', 1)[-1])
            data = jar.read(name)
            for s in re.findall(rb'[\x20-\x7e]{4,}', data):
                pool.add(s.decode('latin1'))
    if readable == 0:
        return None
    return pool


def project_symbols():
    """All miuix symbols imported by the project, as (full, lastSegment) pairs."""
    syms = set()
    pat = re.compile(r'top\.yukonga\.miuix\.kmp\.([A-Za-z0-9_.]+)')
    for root, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith('.kt'):
                continue
            src = open(os.path.join(root, f), encoding='utf-8').read()
            for m in pat.finditer(src):
                syms.add(m.group(1))
    return syms


def exists(pool, sym):
    """A Kotlin import path may be a class, an object, or a top-level member.
    Check the dotted path and its progressively-trimmed tails, because
    `basic.Text` may live in TextKt or as a member of a file facade."""
    parts = sym.split('.')
    # full dotted path with '/' separators, e.g. androidx/compose/material3/Text
    candidates = []
    for i in range(len(parts), 0, -1):
        candidates.append('/'.join(parts[:i]))
        candidates.append('/'.join(parts[:i]) + 'Kt')
    # also try dropping the last segment (e.g. `MiuixTheme.colorScheme` -> `MiuixTheme`)
    for i in range(len(parts) - 1, 0, -1):
        candidates.append('/'.join(parts[:i]))
        candidates.append('/'.join(parts[:i]) + 'Kt')
    for c in candidates:
        if c in pool or c.rsplit('/', 1)[-1] in pool:
            return True
    # last resort: bare last segment anywhere in the pool
    return parts[-1] in pool


def main():
    print('=== 收集 0.9.1 符号池 ===')
    p91 = collect('0.9.1')
    print('=== 收集 0.9.3 符号池 ===')
    p93 = collect('0.9.3')

    # 硬断言：符号池为空说明路径不对或 AAR 没解出来。
    # 上一版脚本在这种情况下会输出"未发现符号消失"—— 那是假结论，比不给结论更糟。
    for name, pool in (('0.9.1', p91), ('0.9.3', p93)):
        if not pool:
            print('ABORT %s 符号池为空，无法得出任何结论。' % name)
            print('      请检查 MIUIX_CMP_DIR 指向的目录是否含 %s' % '/'.join(MODULES[:1]))
            return 2
        print('  %s 标识符数: %d' % (name, len(pool)))

    syms = project_symbols()
    if not syms:
        print('ABORT 项目符号提取为空，请检查 MIUIX_SRC_DIR: %s' % SRC)
        return 2
    print('=== 项目使用的 miuix 符号: %d 个 ===' % len(syms))

    missing = []
    for s in sorted(syms):
        in91 = exists(p91, s)
        in93 = exists(p93, s)
        if in91 and not in93:
            missing.append(s)
            print('  GONE  %s   (0.9.1 有 -> 0.9.3 无)' % s)

    print()
    if missing:
        print('RESULT: %d 个符号在 0.9.3 中消失（潜在破坏点）:' % len(missing))
        for m in missing:
            print('  - %s' % m)
        return 1
    print('RESULT: 0.9.1 -> 0.9.3 未发现符号消失')
    print('（注意：本检查基于标识符存在性，不覆盖参数签名变化；'
          '真正的验证仍需 CI 编译）')
    return 0


if __name__ == '__main__':
    sys.exit(main())
