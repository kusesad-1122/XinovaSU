"""
给 build-manager.yml 插入 versionCode 门槛断言。

分三处插入，每次都断言 YAML 仍可解析 —— 之前手工 Edit 反复把
`run: |` 块的缩进改坏（8 vs 10 空格），YAML 直接 parse 失败。
用脚本 + 每次验证，避免再犯。

插入点：
  1. lkm job / Build xinovasu.ko —— 捕获 Kbuild 的 $(info) 输出，断言 ≥32377，
     落盘 version.txt
  2. lkm job —— 上传 version.txt 为独立 artifact
  3. manager job —— 下载它，并在编译前断言 APK 与 LKM 版本一致且都过门槛
"""

import sys

import yaml

P = '.github/workflows/build-manager.yml'
MIN = 32377


def load():
    with open(P, encoding='utf-8') as f:
        return yaml.safe_load(f)


def save(d):
    with open(P, 'w', encoding='utf-8', newline='\n') as f:
        yaml.safe_dump(d, f, allow_unicode=True, sort_keys=False, width=200,
                       default_flow_style=False)
    # 落盘后立刻验证
    load()
    print('  ✓ YAML 仍可解析')


def find_run_block(text, step_name):
    """Return (start, end) line indices of the `run: |` content under a step."""
    lines = text.split('\n')
    for i, l in enumerate(lines):
        if l.strip() == '- name: ' + step_name:
            for j in range(i, i + 6):
                if lines[j].strip() == 'run: |':
                    indent = len(lines[j]) - len(lines[j].lstrip())
                    k = j + 1
                    while k < len(lines):
                        cur = lines[k]
                        if cur.strip() and (len(cur) - len(cur.lstrip())) <= indent:
                            break
                        k += 1
                    return lines, i, j, k
    raise SystemExit('step not found: ' + step_name)


def main():
    text = open(P, encoding='utf-8').read()

    # ── 1) lkm: 捕获并断言版本 ────────────────────────────────────────
    print('[1/3] lkm job — 捕获 Kbuild 版本并断言门槛')
    lines, s, r, e = find_run_block(text, 'Build xinovasu.ko')
    base = len(lines[r]) - len(lines[r].lstrip())  # 'run: |' 的缩进
    pad = ' ' * (base + 2)
    block = [
        '',
        pad + '# 捕获 Kbuild 实际算出的 XNSU_VERSION —— 唯一可靠来源。',
        pad + '# 它是编译期常量、以二进制立即数存在于 .text，事后从 .ko 里',
        pad + '# grep 搜不到（实测 32377 / 32637 / 30031 全都搜不到）。',
        pad + '# manager job 依赖此值做断言，所以由本 job 产出并上传。',
        pad + 'KO_VER=$(grep -oE "XinovaSU version: [0-9]+" /tmp/kbuild.log | grep -oE "[0-9]+" | head -1 || true)',
        pad + 'if [ -z "$KO_VER" ]; then',
        pad + '  echo "::error::无法从 make 输出解析出 XNSU_VERSION"',
        pad + '  grep -iE "xnsu|version" /tmp/kbuild.log | head -10 || true',
        pad + '  exit 1',
        pad + 'fi',
        pad + 'echo "Kbuild 算出的 XNSU_VERSION = $KO_VER"',
        pad + 'if [ "$KO_VER" -lt ' + str(MIN) + ' ]; then',
        pad + '  echo "::error::LKM 版本 $KO_VER 低于 ' + str(MIN) + ' 门槛"',
        pad + '  echo "::error::刷入后模块与 su 正常，但管理器显示未安装"',
        pad + '  exit 1',
        pad + 'fi',
        pad + 'mkdir -p /github/workspace/out/version',
        pad + 'echo "XinovaSU version: $KO_VER" > /github/workspace/out/version/version.txt',
        '',
    ]
    lines = lines[:e] + block + lines[e:]
    # 让 make 输出落盘供 grep（替换原来的裸 make 行）
    for i, l in enumerate(lines):
        if l.strip().startswith('make -C "$KDIR"') and 'kbuild.log' not in l:
            lines[i] = l + ' 2>&1 | tee /tmp/kbuild.log'
            break
    open(P, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
    save(load())

    # ── 2) lkm: 上传 version.txt ──────────────────────────────────────
    print('[2/3] lkm job — 上传 version.txt')
    text = open(P, encoding='utf-8').read()
    import re
    m = re.search(r'^([ ]*)- name: Upload LKM$', text, re.M)
    if not m:
        raise SystemExit('Upload LKM step not found')
    ind = m.group(1)
    idx = m.start()
    ins = (ind + '- name: Upload LKM version\n'
           + ind + '  uses: actions/upload-artifact@v5\n'
           + ind + '  with:\n'
           + ind + '    name: lkm-version\n'
           + ind + '    path: /github/workspace/out/version/version.txt\n'
           + ind + '    if-no-files-found: error\n'
           + '\n')
    text = text[:idx] + ins + text[idx:]
    open(P, 'w', encoding='utf-8', newline='\n').write(text)
    save(load())

    # ── 3) manager: 下载 + 断言一致 ───────────────────────────────────
    print('[3/3] manager job — 下载并断言 APK/LKM 版本一致')
    text = open(P, encoding='utf-8').read()
    m2 = re.search(r'^([ ]*)- name: Stage LKM into ksud embedded assets$', text, re.M)
    if not m2:
        raise SystemExit('Stage LKM step not found')
    ind2 = m2.group(1)
    marker = ind2 + '- name: Stage LKM into ksud embedded assets\n'
    ins = f'''{ind2}- name: Download LKM version
{ind2}  uses: actions/download-artifact@v5
{ind2}  with:
{ind2}    name: lkm-version
{ind2}    path: version

{ind2}- name: Assert versionCode consistency
{ind2}  run: |
{ind2}    set -euo pipefail
{ind2}    MIN=32377
{ind2}    test -f version/version.txt || {{ echo "::error::缺少 version/version.txt"; exit 1; }}
{ind2}    KO_VER=$(grep -oE "XinovaSU version: [0-9]+" version/version.txt | grep -oE "[0-9]+" | head -1)
{ind2}    VC=$(grep -oE "VERSION_CODE_BASE = [0-9]+" manager/build.gradle.kts | grep -oE "[0-9]+")
{ind2}    COMMITS=$(git rev-list --count HEAD)
{ind2}    APK_VC=$((VC + COMMITS))
{ind2}    echo "  LKM version     = $KO_VER   (Kbuild 实际算出)"
{ind2}    echo "  APK versionCode = $VC + $COMMITS = $APK_VC"
{ind2}    [ "$APK_VC" -ge "$MIN" ] || {{ echo "::error::APK versionCode $APK_VC < $MIN"; exit 1; }}
{ind2}    [ "$KO_VER" -ge "$MIN" ] || {{ echo "::error::LKM version $KO_VER < $MIN"; exit 1; }}
{ind2}    [ "$APK_VC" -eq "$KO_VER" ] || {{
{ind2}      echo "::error::APK($APK_VC) 与 LKM($KO_VER) 不一致，界面会显示驱动版本不匹配"
{ind2}      exit 1
{ind2}    }}
{ind2}    echo "  OK: 两者一致且均 >= $MIN（余量 $((APK_VC - MIN))）"

'''
    text = text.replace(marker, ins + marker, 1)
    open(P, 'w', encoding='utf-8', newline='\n').write(text)
    save(load())

    d = load()
    print()
    print('lkm steps    :', [s.get('name') for s in d['jobs']['lkm']['steps']])
    print('manager steps:', [s.get('name') for s in d['jobs']['manager']['steps']])


if __name__ == '__main__':
    sys.exit(main())
