"""
从 APK 验证结构完整性（native 库 / ABI / 资源 / 关键字符串）。

⚠️ 只对 **debug 包可靠**。release 开了 isMinifyEnabled + isShrinkResources，
   R8 会把资源名与字符串全部改写：
     - 资源名变成 res/0j.jpg、res/_N.jpg 这类短名 → 按 `theme_` 前缀匹配必然找不到
     - dex 里不再有 `xnsuinit` 之类的可读字符串
   在 release 上跑会出现两类**假报警**（已实测踩过）：
     1. 误报「主题插画 0 张」—— 实际 21/21 都在，只是名字被混淆。
        复核要用**体积匹配**：拿仓库里源文件的字节数去比 APK 内的文件体积。
     2. 误报「dex 里没有 xnsuinit」—— 它不是独立资源，而是被嵌进
        libxnsusd.so（与 7 个 KMI 的 `_xinovasu.ko` 一起，
        走 ksud 的 `#[folder = "bin/aarch64"]`）。要在 so 里搜。

检查项：
  - dex 是否存在且可解析
  - native 库与 ABI 覆盖（对比 build.gradle.kts 的 abiFilters）
  - 关键 native 库是否在（libxnsusd.so 缺了 root 功能直接不可用）
  - 主题插画是否齐全（debug 用文件名匹配；release 请改用体积匹配）
  - dex 里的关键字符串（同样只对 debug 可靠）

它**不能**覆盖的：运行时反射行为、AIDL binder 是否能连上、
xnsuinit 是否真的完全静态（无 PT_INTERP）—— 后者由 CI 的
llvm-readelf 断言把关，刷入前必须看 CI 是否绿。
"""

import re
import sys
import zipfile


def check(path):
    try:
        z = zipfile.ZipFile(path)
    except FileNotFoundError:
        print('ERROR 找不到 APK: %s' % path)
        return 2
    except zipfile.BadZipFile:
        print('ERROR 不是有效 APK: %s' % path)
        return 2

    names = z.namelist()
    size_mb = sum(i.file_size for i in z.infolist()) / 1024 / 1024

    print('APK: %s' % path)
    print('  条目数: %d' % len(names))
    print('  解压后总体积: %.1f MB' % size_mb)

    problems = []

    # ── 1. dex 必须存在且能解析出类 ──────────────────────────────────
    dex = [n for n in names if re.match(r'classes\d*\.dex$', n)]
    print('  dex 文件: %d 个 %s' % (len(dex), sorted(dex)))
    if not dex:
        problems.append('APK 内没有 dex —— APK 损坏或构建异常')

    # ── 2. native 库 ──────────────────────────────────────────────────
    libs = sorted(n for n in names if n.startswith('lib/') and n.endswith('.so'))
    print('  native 库: %d 个' % len(libs))
    for l in libs:
        print('     %s  (%.0f KB)' % (l, z.getinfo(l).file_size / 1024))

    if not any('libxnsusd.so' in l for l in libs):
        problems.append(
            '缺少 libxnsusd.so —— KsuCli.getKsuDaemonPath() 会找不到守护进程，'
            '整个 root 功能不可用'
        )

    # ABI 覆盖：abiFilters 声明了 arm64-v8a + x86_64
    abis = {l.split('/')[1] for l in libs if l.startswith('lib/')}
    expected_abis = {'arm64-v8a', 'x86_64'}
    missing_abis = expected_abis - abis
    if missing_abis:
        problems.append('ABI 缺失: %s（build.gradle.kts 的 abiFilters 声明了这两个）'
                        % missing_abis)
    print('  ABI: %s' % sorted(abis))

    # ── 3. 主题插画资源 ──────────────────────────────────────────────────
    # release 下 R8 会把资源名混淆成 res/0j.jpg 这类短名，按 `theme_` 前缀匹配
    # 必然得到 0 —— 那是假报警。改用体积匹配：拿仓库里源文件的字节数
    # 去比 APK 内的 jpg 体积。两种模式都做，报告里区分开。
    import glob
    import os
    themes = [n for n in names if n.endswith('.jpg')]
    src_dir = os.path.join(os.path.dirname(os.path.abspath(path)), '..',
                           'manager', 'app', 'src', 'main', 'res', 'drawable-nodpi')
    src = {os.path.basename(p): os.path.getsize(p)
           for p in glob.glob(os.path.join(src_dir, '*.jpg'))}

    print('  JPG 资源: %d 张' % len(themes))
    if src:
        apk_sizes = {z.getinfo(n).file_size for n in themes}
        by_name = sum(1 for n in themes if os.path.basename(n) in src)
        by_size = sum(1 for sz in src.values() if sz in apk_sizes)
        print('  仓库源文件: %d 张' % len(src))
        print('    按文件名匹配: %d  （release 下应为 0 —— 名字被混淆）' % by_name)
        print('    按体积匹配:   %d  （这才是可信数字）' % by_size)
        if by_size < len(src):
            missing = [k for k, sz in src.items() if sz not in apk_sizes]
            problems.append(
                '主题插画按体积只匹配到 %d/%d —— 缺 %s'
                % (by_size, len(src), missing[:3])
            )
    else:
        print('  （未找到仓库源目录，跳过插画校验）')

    # ── 4. 关键内容是否还在 ─────────────────────────────────────────────
    # xnsuinit 不是独立资源，它与 7 个 KMI 的 `_xinovasu.ko` 一起被嵌进
    # libxnsusd.so（ksud 的 `#[folder = "bin/aarch64"]`）。
    # 所以在 dex 里搜 xnsuinit 必然找不到 —— 要去 so 里搜。
    try:
        blob = b''
        for d in [n for n in names if re.match(r'classes\d*\.dex$', n)]:
            blob += z.read(d)
        for probe in [b'IKsuInterface', b'XinovaSU']:
            print('  dex 含 %-16s %s' % (probe.decode(), probe in blob))
            if probe not in blob:
                problems.append('dex 里找不到字符串 %s' % probe.decode())

        so_names = [n for n in libs if n.endswith('libxnsusd.so')]
        if so_names:
            so = z.read(so_names[0])
            for probe in [b'xnsuinit', b'_xinovasu.ko']:
                print('  so  含 %-16s %s' % (probe.decode(), probe in so))
            if b'xnsuinit' not in so:
                problems.append(
                    'libxnsusd.so 里没有 xnsuinit —— ramdisk 的 /init 缺失，'
                    'boot-patch 会失败（这正是 CI 流水线第 5 步要静态嵌入的东西）'
                )
            # KMI 命名是 android<版本>-<内核版本>，如 android14-6.1_xinovasu.ko，
            # 版本号里的点只出现在「内核版本」部分（5.10 / 6.1 / 6.12），
            # 所以分隔符是连字符而不是点 —— 写成 android\d+[\d.]+ 会漏掉全部。
            kmis = sorted(set(re.findall(rb'android\d+-[\d.]+_xinovasu\.ko', so)))
            print('  so 内 KMI 模块: %d 个' % len(kmis))
            for k in kmis:
                print('     %s' % k.decode())
            if len(kmis) == 0:
                problems.append(
                    'libxnsusd.so 里没找到任何 _xinovasu.ko —— LKM 模块可能没嵌进去，'
                    'KMI 列表会全空、boot-patch 找不到可加载的模块'
                )
    except Exception as exc:
        print('  (读取内容失败: %s)' % exc)

    print()
    if problems:
        print('RESULT: %d 个问题' % len(problems))
        for p in problems:
            print('  - %s' % p)
        return 1
    print('RESULT: APK 结构检查通过（不覆盖运行时反射/绑定行为，那需真机）')
    return 0


if __name__ == '__main__':
    sys.exit(check(sys.argv[1] if len(sys.argv) > 1 else 'app-release.apk'))
