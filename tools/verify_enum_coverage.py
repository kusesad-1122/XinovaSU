"""
统计装饰枚举的覆盖率：哪些造型从未出现在任何主题配方里。

为什么重要：装饰体系有 7 类造型枚举（OrnamentMotif / FrameStyle / PedestalStyle /
AmbientStyle / EnergyPathStyle / ParticleStyle / TitleRailStyle），每类 32-35 项。
如果某一项从未被任何 AUTHORED_RECIPES 使用，它就是**死代码**——
既不会出现在界面上，也永远不会被快照测试覆盖，属于纯粹的维护负担。

同时反过来检查：每个枚举项是否都真的能被渲染（有些项可能引用了不存在的资源名）。

注意：枚举项在源码里是逗号分隔的，可能跨多行，也可能同一行多个，
所以不能用简单的行数统计 —— 必须按逗号切分。
"""

import re
import sys

SPEC = sys.argv[1] if len(sys.argv) > 1 else (
    'manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpec.kt'
)
CATALOG = sys.argv[2] if len(sys.argv) > 2 else (
    'manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalog.kt'
)

# 与绘制层相关的枚举（这些的每个取值都会影响画面）
RENDER_ENUMS = [
    'OrnamentMotif', 'FrameStyle', 'PedestalStyle',
    'AmbientStyle', 'EnergyPathStyle', 'ParticleStyle', 'TitleRailStyle',
]


def parse_enums(path):
    src = open(path, encoding='utf-8').read()
    # 去掉行注释与块注释，避免注释里的花括号干扰
    src = re.sub(r'//[^\n]*', '', src)
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    out = {}
    for m in re.finditer(r'enum class (\w+)\s*\{', src):
        name = m.group(1)
        i = m.end()
        depth = 1
        while i < len(src) and depth > 0:
            if src[i] == '{':
                depth += 1
            elif src[i] == '}':
                depth -= 1
            i += 1
        body = src[m.end():i - 1]
        # 枚举项：顶层逗号分隔的标识符（可能有内部 class 体，如 DriftAxis 无，BadgeAnchor 无）
        items = []
        depth2 = 0
        cur = ''
        for ch in body:
            if ch in '({[':
                depth2 += 1
            elif ch in ')}]':
                depth2 -= 1
            if ch == ',' and depth2 == 0:
                tok = cur.strip()
                if re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', tok):
                    items.append(tok)
                cur = ''
            else:
                cur += ch
        tok = cur.strip()
        if re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', tok):
            items.append(tok)
        out[name] = items
    return out


def parse_used_styles(catalog_path):
    """Collect every style value referenced by AUTHORED_RECIPES."""
    src = open(catalog_path, encoding='utf-8').read()
    src = re.sub(r'//[^\n]*', '', src)
    used = {}
    for enum_name in RENDER_ENUMS:
        vals = re.findall(enum_name + r'\.([A-Za-z0-9_]+)', src)
        used[enum_name] = set(vals)
    return used


def main():
    enums = parse_enums(SPEC)
    used = parse_used_styles(CATALOG)

    print('=== 装饰枚举覆盖情况 ===')
    print()
    total_items = 0
    total_used = 0
    dead = {}

    for name in RENDER_ENUMS:
        items = enums.get(name, [])
        if not items:
            print('  %-18s (未在 %s 中找到)' % (name, SPEC.split('/')[-1]))
            continue
        u = used.get(name, set())
        # 只统计真正被配方引用的（有些枚举项可能只用于内部）
        uncovered = [x for x in items if x not in u]
        covered = len(items) - len(uncovered)
        pct = 100.0 * covered / len(items)
        total_items += len(items)
        total_used += covered
        flag = '' if not uncovered else '   <-- %d 项未被使用' % len(uncovered)
        print('  %-18s %2d/%2d  %5.1f%%%s' % (name, covered, len(items), pct, flag))
        if uncovered:
            dead[name] = uncovered

    print()
    print('  合计 %d/%d 被主题配方使用 (%.1f%%)'
          % (total_used, total_items, 100.0 * total_used / max(total_items, 1)))
    print()

    if dead:
        print('=== 未被任何主题使用的枚举项（疑似死代码）===')
        for name, items in dead.items():
            print('  %s:' % name)
            for it in items:
                print('      %s' % it)
        print()
        print('这些项不影响画面 —— 删掉可减少维护面，或补进配方让它们可见。')
        return 1

    print('RESULT: 所有装饰枚举项都被至少一个主题配方使用')
    return 0


if __name__ == '__main__':
    sys.exit(main())
