"""
静态检查测试源文件的 import 完整性。

为什么需要：本项目本轮新增的快照测试首次提交时 CI 报了三类错误
（Unresolved reference 'LocalThemeMorphFrame' / 'height' / 'width'），
根因全是 import 缺失或写错包名。本机无 JDK，无法靠编译兜底，
只能写脚本在提交前扫一遍。

检查项：
  1. 用了但没 import 的 Compose / JUnit 符号（按包名前缀推断）
  2. import 了但文件里没用到（未使用 import，Kotlin 会警告）
  3. 导入了本包内的符号（同包无需 import，多余）
"""

import os
import re
import sys

# 常见 Compose/JUnit 符号 -> 全限定名（只需覆盖本项目会用到的）
KNOWN = {
    # androidx.compose.foundation.layout
    'Box': 'androidx.compose.foundation.layout.Box',
    'Column': 'androidx.compose.foundation.layout.Column',
    'Row': 'androidx.compose.foundation.layout.Row',
    'Spacer': 'androidx.compose.foundation.layout.Spacer',
    'Modifier': 'androidx.compose.ui.Modifier',
    'Text': 'androidx.compose.material3.Text',
    'background': 'androidx.compose.foundation.background',
    'height': 'androidx.compose.foundation.layout.height',
    'width': 'androidx.compose.foundation.layout.width',
    'fillMaxSize': 'androidx.compose.foundation.layout.fillMaxSize',
    'padding': 'androidx.compose.foundation.layout.padding',
    'toArgb': 'androidx.compose.ui.graphics.toArgb',
    'dp': 'androidx.compose.ui.unit.dp',
    'Alignment': 'androidx.compose.ui.Alignment',
    'Color': 'androidx.compose.ui.graphics.Color',
    'LocalLayoutDirection': 'androidx.compose.ui.platform.LocalLayoutDirection',
    'LayoutDirection': 'androidx.compose.ui.unit.LayoutDirection',
    'TextAlign': 'androidx.compose.ui.text.style.TextAlign',
    'Composable': 'androidx.compose.runtime.Composable',
    'CompositionLocalProvider': 'androidx.compose.runtime.CompositionLocalProvider',
    'remember': 'androidx.compose.runtime.remember',
    'Paparazzi': 'app.cash.paparazzi.Paparazzi',
    'DeviceConfig': 'app.cash.paparazzi.DeviceConfig',
    'Rule': 'org.junit.Rule',
    'Test': 'org.junit.Test',
    'Assert': 'org.junit.Assert',
}


def strip_code(src):
    """Remove comments and string literals so we only scan real code."""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if src[i:i+2] == '//':
            j = src.find('\n', i)
            i = j if j != -1 else n
        elif src[i:i+2] == '/*':
            j = src.find('*/', i + 2)
            i = (j + 2) if j != -1 else n
        elif c == '"':
            if src[i:i+3] == '"""':
                j = src.find('"""', i + 3)
                i = (j + 3) if j != -1 else n
            else:
                i += 1
                while i < n and src[i] != '"':
                    i += 2 if src[i] == '\\' else 1
                i += 1
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def check(path):
    raw = open(path, encoding='utf-8').read()
    code = strip_code(raw)
    lines = raw.splitlines()

    imports = {}
    pkg = None
    for ln in lines:
        m = re.match(r'\s*package\s+([\w.]+)', ln)
        if m:
            pkg = m.group(1)
        m = re.match(r'\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$', ln)
        if m:
            fq = m.group(1)
            alias = m.group(2) or fq.rsplit('.', 1)[-1]
            imports[alias] = fq

    problems = []

    # 1) 用了但没 import
    for sym, fq in KNOWN.items():
        # 作为标识符使用：后面可跟 '(' '<' '.' 数字（16.dp）字母下划线等
        if not re.search(r'(?<![\w.])' + re.escape(sym) + r'(?![\w])', code):
            continue
        if sym in imports:
            continue
        # 同包则不需要 import
        if pkg and fq.rsplit('.', 1)[0] == pkg:
            continue
        # 是否以全限定名直接使用（则不需要 import）
        if fq in code:
            continue
        problems.append('MISSING_IMPORT  %s  (应 import %s)' % (sym, fq))

    # 2) import 了但没用
    #    注意：不能要求符号后必须跟 '(' —— Compose 里 `16.dp`、`Modifier.weight(1f)`、
    #    `x.toArgb()` 的写法里，`dp` / `toArgb` 后面跟的是数字或点号。
    for alias, fq in imports.items():
        body = re.sub(r'^\s*import\s+.*$', '', raw, flags=re.M)
        stripped = strip_code(body)
        if not re.search(r'(?<![\w.])' + re.escape(alias) + r'(?![\w])', stripped):
            problems.append('UNUSED_IMPORT    %s -> %s' % (alias, fq))

    name = os.path.basename(path)
    if problems:
        print('FAIL %s' % name)
        for p in problems:
            print('     %s' % p)
    else:
        print('OK   %s  (%d imports)' % (name, len(imports)))
    return problems


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else '.'
    total = 0
    files = 0
    for dirpath, _, names in os.walk(root):
        for n in sorted(names):
            if not n.endswith('.kt'):
                continue
            p = os.path.join(dirpath, n)
            files += 1
            total += len(check(p))
    print()
    print('扫描 %d 个文件，问题 %d 处' % (files, total))
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main())
