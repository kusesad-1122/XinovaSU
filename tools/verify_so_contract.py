"""
校验原生库命名契约在三处保持一致。

背景：仓库历史上三处说法不一，导致本地 `just bm` 能编译通过，
但产出的 APK 里没有 manager 真正要找的 .so，装到设备上才暴露：

  1. userspace/ksud/Cargo.toml 的 package name  -> 决定 cargo 产出的文件名
  2. justfile 里 cp 到 jniLibs 的目标名          -> 决定 APK 里的文件名
  3. manager Kotlin 侧 nativeLibraryDir 的查找名 -> 决定运行时找哪个文件

三处必须指向同一个 .so 名。本脚本直接读三个源文件做交叉校验。
"""

import re
import sys

ROOT = 'D:/xinovasu/'

CARGO_TOML = ROOT + 'userspace/ksud/Cargo.toml'
JUSTFILE = ROOT + 'justfile'
KSU_CLI = ROOT + 'manager/app/src/main/java/com/xinsu/moe/ui/util/KsuCli.kt'
GITIGNORE = ROOT + 'manager/app/src/main/jniLibs/.gitignore'

failures = []


def check(name, cond, detail=''):
    if cond:
        print('  PASS  %s' % name)
    else:
        print('  FAIL  %s  %s' % (name, detail))
        failures.append(name)


print('[1] 读取各处声明')
cargo = open(CARGO_TOML, encoding='utf-8').read()
just = open(JUSTFILE, encoding='utf-8').read()
cli = open(KSU_CLI, encoding='utf-8').read()
gi = open(GITIGNORE, encoding='utf-8').read()

pkg = re.search(r'^name\s*=\s*"([^"]+)"', cargo, re.M)
pkg_name = pkg.group(1) if pkg else None
print('  Cargo package name : %s' % pkg_name)
check('Cargo.toml 有 package name', pkg_name is not None)

# Kotlin 侧要找的 .so
so = re.search(r'"(lib[^"]*\.so)"', cli)
want_so = so.group(1) if so else None
print('  Kotlin 查找的 .so   : %s' % want_so)
check('KsuCli.kt 能解析出 .so 名', want_so is not None)

# justfile 里 cp 的目标
tgt = re.search(r'jniLibs/arm64-v8a/(\S+\.so)', just)
copy_target = tgt.group(1) if tgt else None
print('  justfile cp 目标   : %s' % copy_target)
check('justfile 能解析出 cp 目标', copy_target is not None)

# justfile 里 cargo build 的 package
bp = re.search(r'cross build (?:--package\s+(\S+)\s+)?--target', just)
build_pkg = bp.group(1) if bp and bp.group(1) else None
print('  justfile build 包  : %s' % (build_pkg or '(未指定)'))

print()
print('[2] 一致性交叉校验')
if pkg_name and want_so:
    expected_bin = pkg_name                      # xnsusd
    expected_so = 'lib%s.so' % pkg_name           # libxnsusd.so
    check('package name 推出的 .so 名 == Kotlin 查找的',
          expected_so == want_so,
          '推出 %s，实际 %s' % (expected_so, want_so))

if copy_target:
    check('justfile cp 目标 == Kotlin 查找的',
          copy_target == want_so,
          'cp %s，Kotlin 找 %s' % (copy_target, want_so))

if build_pkg:
    check('justfile build 的 package == Cargo package name',
          build_pkg == pkg_name,
          'build %s，Cargo 是 %s' % (build_pkg, pkg_name))
else:
    check('justfile 显式指定 --package', False,
          '未指定 --package，会构建 workspace default-members（可能编错目标）')

if want_so:
    check('.gitignore 已忽略 %s' % want_so, want_so in gi)

print()
print('[3] 反向检查：不应残留的旧名')
# 只看命令行，不看注释 —— justfile 的注释里会解释历史遗留（提到旧名是合理的），
# 真正要确保的是没有再 cp 旧名。
cmd_lines = [ln for ln in just.splitlines() if not ln.lstrip().startswith('#')]
cmd_text = '\n'.join(cmd_lines)
for stale, why in [('libksud.so', '与 Kotlin 查找名不一致的历史遗留')]:
    check('justfile 命令行不再产出 %s' % stale, stale not in cmd_text, why)

print()
if failures:
    print('RESULT: %d 项不一致 -> %s' % (len(failures), failures))
    sys.exit(1)
print('RESULT: 命名契约三处一致')
