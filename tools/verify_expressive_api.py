"""
校验同批移植的 Expressive 组件在 material3 1.5.0-alpha19 下是否真的存在对应 API。

背景：CI 报 `Unresolved reference 'colors'`，根因是 alpha19 里
ToggleButtonDefaults 的工厂方法叫 toggleButtonColors，alpha28 才改名 colors。
既然已经从 AAR 反查确认过一次，就应该把同批移植文件里用到的每个
M3 符号一次性全部核对，而不是靠 CI 一轮轮试错。

做法：解出 alpha19 AAR 的 class 常量池里出现过的标识符，
逐个检查源码里引用的 M3 符号是否在其中。
注意：这只覆盖「类/方法名是否存在」，不覆盖参数名或类型是否完全一致
（后者仍需编译验证），但能可靠抓住这类改名/缺失问题。
"""

import re
import sys
import zipfile

AAR = 'm3aar/m3.aar'
JAR = 'm3aar/classes.jar'

# 移植文件里引用的 M3 符号 -> 需要在 alpha19 中确认存在
# 格式：(文件, 源码中的调用片段, 用于匹配的标识符)
# 注意：Kotlin 的类名在 class 常量池里以 JVM 描述符形式出现
# （如 ToggleButtonColors -> Landroidx/compose/material3/ToggleButtonColors;），
# 顶层函数则落在 <File>Kt 里（如 ToggleButton -> ToggleButtonKt）。
# 所以符号匹配要同时接受「裸名」和「包限定形式」。
CHECKS = [
    ('ExpressiveToggleButton.kt', 'ToggleButtonDefaults.toggleButtonColors', 'toggleButtonColors'),
    ('ExpressiveToggleButton.kt', 'ToggleButton(', 'ToggleButtonKt'),
    ('ExpressiveToggleButton.kt', 'ToggleButtonColors', 'ToggleButtonColors'),
    ('ExpressiveToggleButton.kt', 'ToggleButtonShapes', 'ToggleButtonShapes'),
    ('ExpressiveTabRow.kt', 'PrimaryTabRow', 'PrimaryTabRow'),
    ('ExpressiveMenu.kt', 'DropdownMenuPopup', 'DropdownMenuPopup'),
    ('ExpressiveMenu.kt', 'DropdownMenuGroup', 'DropdownMenuGroup'),
    ('ExpressiveMenu.kt', 'MenuDefaults.groupShape', 'groupShape'),
    ('ExpressiveDialog.kt', 'AlertDialog', 'AlertDialog'),
    ('TopBarBackButton.kt', 'IconButtonDefaults', 'IconButtonDefaults'),
]

# alpha19 的 material3 包路径
M3 = 'androidx/compose/material3'


def symbol_exists(pool, symbol):
    """A Kotlin symbol shows up either bare, as a JVM class descriptor,
    or as a <File>Kt holder. Accept all three forms."""
    if symbol in pool:
        return True
    if ('L%s/%s;' % (M3, symbol)) in pool:
        return True
    if ('%sKt' % symbol) in pool:
        return True
    return False

SRC_DIR = 'D:/xinovasu/manager/app/src/main/java/com/xinsu/moe/ui/component/material/'


def collect_identifiers(jar_path):
    """Collect identifiers from the jar.

    Two complementary sources, because a single one is not enough:
      1. class file *paths* — e.g. 'androidx/compose/material3/ToggleButtonKt.class'
         covers Kotlin top-level functions and nested classes reliably;
      2. raw bytes of every class — covers method names, which live in the
         constant pool and never appear in a path.
    """
    z = zipfile.ZipFile(jar_path)
    pool = set()
    for name in z.namelist():
        if not name.endswith('.class'):
            continue
        # (1) the path itself, both as-is and with separators stripped
        stem = name[:-len('.class')]
        pool.add(stem)
        simple = stem.rsplit('/', 1)[-1]
        pool.add(simple)
        # (2) identifier-ish runs from the constant pool
        data = z.read(name)
        for s in re.findall(rb'[\x20-\x7e]{4,}', data):
            pool.add(s.decode('latin1'))
    return pool


def main():
    try:
        pool = collect_identifiers(JAR)
    except FileNotFoundError:
        print('ERROR: %s not found. Download the AAR first.' % JAR)
        return 2

    print('class 常量池标识符总数: %d\n' % len(pool))

    failures = []
    for filename, call, symbol in CHECKS:
        src_path = SRC_DIR + filename
        try:
            src = open(src_path, encoding='utf-8').read()
        except FileNotFoundError:
            print('  SKIP  %-28s (源文件不存在)' % filename)
            continue

        in_source = call in src
        in_pool = symbol_exists(pool, symbol)

        if not in_source:
            status, note = 'SKIP', '源码中已无此调用'
        elif in_pool:
            status, note = 'PASS', ''
        else:
            status, note = 'FAIL', 'alpha19 中找不到该标识符'
            failures.append((filename, call, symbol))

        print('  %-5s %-28s %-38s %s' % (status, filename, call, note))

    print()
    if failures:
        print('RESULT: %d 个符号在 alpha19 中不存在:' % len(failures))
        for f, c, s in failures:
            print('  %s: %s (期望标识符 %s)' % (f, c, s))
        return 1
    print('RESULT: 全部符号在 alpha19 中存在')
    return 0


if __name__ == '__main__':
    sys.exit(main())
