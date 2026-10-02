"""
检查 Android 资源的 i18n 一致性。

为什么查：项目有 20+ 个 values-xx 翻译目录，编译不会因为「某个语言少了
几个 key」而失败（那属于内容缺失，不是代码错误），但运行时用户会看到
界面元素直接消失或显示 key 本身。这类问题只能靠静态比对发现。

规则：
  1. values/strings.xml 里的每个 string name，至少要出现在 values/strings.xml
     之外的一个语言目录里（否则这个翻译从来没做过）
  2. 各语言目录之间缺失的 key 数量统计 —— 差异过大说明翻译维护不动了
  3. plurals 单独统计（复数形式在中文/日文里通常不需要，容易被误报）
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

RES = sys.argv[1] if len(sys.argv) > 1 else 'manager/app/src/main/res'

BASE = os.path.join(RES, 'values')


def parse_strings(path):
    """Return (names, plurals_names, untranslatable)."""
    if not os.path.exists(path):
        return set(), set(), set()
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exc:
        print('  PARSE_ERROR %s: %s' % (path, exc))
        return set(), set(), set()
    names, plurals, untranslatable = set(), set(), set()
    for child in root:
        tag = child.tag
        name = child.get('name')
        if not name:
            continue
        if tag == 'string':
            names.add(name)
            # translatable="false" 的不需要翻译
            if child.get('translatable') == 'false':
                untranslatable.add(name)
        elif tag == 'plurals':
            plurals.add(name)
    return names, plurals, untranslatable


def main():
    base_strings, base_plurals, untranslatable = parse_strings(
        os.path.join(BASE, 'strings.xml')
    )
    translatable_base = base_strings - untranslatable

    print('基准 values/strings.xml')
    print('  string 总数: %d（其中 translatable=false: %d，需翻译: %d）'
          % (len(base_strings), len(untranslatable), len(translatable_base)))
    print('  plurals 总数: %d' % len(base_plurals))
    print()

    # 收集所有语言目录
    lang_dirs = []
    for d in sorted(os.listdir(RES)):
        if not d.startswith('values-'):
            continue
        p = os.path.join(RES, d, 'strings.xml')
        if os.path.exists(p):
            lang_dirs.append((d, p))

    if not lang_dirs:
        print('RESULT: 没有找到任何翻译目录')
        return 0

    print('翻译目录: %d 个' % len(lang_dirs))
    print()
    print('%-16s %-8s %-8s %-8s %s' % ('locale', 'strings', 'plurals', 'missing', '覆盖率'))
    print('-' * 62)

    # 只统计「有实际翻译」的目录：values-xx 里的资源名带 -b 的是从母语继承的补充，
    # 不算独立翻译
    translated = []
    for d, p in lang_dirs:
        s, pl, _ = parse_strings(p)
        # 过滤掉 BCP47 变体（values-b+zh+Hans）里仅作占位的
        if len(s) == 0:
            continue
        translated.append((d, s, pl))

    problems = []
    for d, s, pl in translated:
        missing = translatable_base - s
        cov = 1.0 - len(missing) / max(len(translatable_base), 1)
        print('%-16s %-8d %-8d %-8d %.1f%%'
              % (d, len(s), len(pl), len(missing), cov * 100))
        if cov < 0.3:
            problems.append('%s 覆盖率仅 %.0f%%' % (d, cov * 100))

    # 没有任何语言覆盖的 string（这些在所有语言里都缺失）
    all_covered = set()
    for d, s, pl in translated:
        all_covered |= s
    never_translated = translatable_base - all_covered
    print()
    print('从未出现在任何翻译目录的 string: %d 个' % len(never_translated))
    if never_translated:
        # 列出前 20 个作为参考
        for n in sorted(never_translated)[:20]:
            print('    %s' % n)
        if len(never_translated) > 20:
            print('    ... 还有 %d 个' % (len(never_translated) - 20))

    print()
    if problems:
        print('RESULT: %d 个语言覆盖率过低（<30%%）:' % len(problems))
        for p in problems:
            print('  - %s' % p)
        return 1
    print('RESULT: i18n 结构检查通过')
    if never_translated:
        print('（注: %d 个 string 尚无任何翻译 —— 属内容工作，不阻塞 CI）'
              % len(never_translated))
    return 0


if __name__ == '__main__':
    sys.exit(main())
