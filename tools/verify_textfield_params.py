"""
扫描所有 miuix TextField 调用点，检查是否用到 0.9.3 被合并掉的参数。

miuix 0.9.3 把 TextField 的 backgroundColor / labelColor / borderColor
三个参数合并成了 `colors: TextFieldColors`（见 TextFieldDefaults.textFieldColors）。
这是 CI #37 报出的两类错误之一。

只扫 TextField( 的调用块，提取具名参数，与被移除的参数集合求交集。
注意不能简单全局 grep backgroundColor —— Material 组件和
SearchStatus.TopAppBarAnim 也有同名参数，只在 TextField 块内才算受影响。
"""

import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
REMOVED = {'backgroundColor', 'labelColor', 'borderColor'}


def call_blocks(src):
    """Yield (start, end, direct_param_region) for each balanced TextField(...) call.

    `direct_param_region` is the part of the call before any nested lambda /
    nested call that legitimately owns the same parameter name. Specifically,
    once we see `colors = TextFieldDefaults.textFieldColors(`, everything from
    there on belongs to that nested factory, not to TextField itself.

    The negative lookbehind also matters: without it,
    `TextFieldDefaults.textFieldColors(` would itself match.
    """
    for m in re.finditer(r'(?<![\w.])TextField\(', src):
        i = m.end()
        depth = 1
        while i < len(src) and depth > 0:
            if src[i] == '(':
                depth += 1
            elif src[i] == ')':
                depth -= 1
            i += 1
        block = src[m.start():i]
        # Cut the block at the point where the migrated `colors =` argument
        # begins; only the text before it is TextField's own parameter list.
        cut = block.find('colors')
        region = block if cut == -1 else block[:cut]
        yield m.start(), i, region


def main():
    total = 0
    affected = []
    for root, _, files in os.walk(ROOT):
        for f in files:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(root, f)
            src = open(p, encoding='utf-8').read()
            for s, e, region in call_blocks(src):
                total += 1
                params = set(re.findall(r'(\w+)\s*=', region))
                bad = params & REMOVED
                if bad:
                    line = src[:s].count('\n') + 1
                    affected.append((p, line, sorted(bad)))

    print('TextField 调用点总数: %d' % total)
    print('用到已移除参数的: %d' % len(affected))
    for p, line, bad in affected:
        print('  %s:%d  ->  %s' % (p, line, bad))
    print()
    if affected:
        print('RESULT: 仍有未迁移的调用点')
        return 1
    print('RESULT: 所有 TextField 调用点均未使用 0.9.3 移除的参数')
    return 0


if __name__ == '__main__':
    sys.exit(main())
