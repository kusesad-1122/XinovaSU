# 验证脚本

这些脚本是**本机无 JDK / 无 Android SDK** 环境下做静态核对的工具。
它们不能替代编译与真机测试，但能在提交前发现一批靠"编译绿"发现不了的问题。

## 用法

```bash
# 大多数脚本直接跑即可（Python 3.13+，无第三方依赖）
python tools/verify_enum_coverage.py
python tools/verify_i18n.py manager/app/src/main/res
python tools/verify_apk_contents.py dist/XinovaSU-release.apk

# 需要先下载 AAR / APK 的
MIUIX_CMP_DIR=/path/to/aar/dir python tools/verify_miuix_upgrade.py
```

## 清单

| 脚本 | 查什么 | 依赖 |
|---|---|---|
| `verify_theme_morph.py` | 形变过渡纯函数 31 项断言（造型切换点、光扫曲线两端归零、最短弧插值） | 无 |
| `verify_miuix_upgrade.py` | 两个 miuix 版本的 AAR 符号比对：解 `classes.jar` 收集 class 路径 + 常量池，比对项目实际 import 的符号 | 需先下载各版本 AAR |
| `verify_miuix_signatures.py` | 组件公开函数的**参数名列表** diff（从 d2 元数据常量池抓），比读 changelog 可靠 | 同上 |
| `verify_expressive_api.py` | material3 alpha19 的二进制符号核对（确认上游移植用到的 API 真的存在） | 需下载 material3 AAR |
| `verify_so_contract.py` | 原生库命名契约三处一致性（`libxnsusd.so` / `libxinovasu.so` / `libadbroot.so`） | 无 |
| `verify_textfield_params.py` | miuix 0.9.3 把 `TextField` 的三个颜色参数合并成 `colors` 后的迁移完整性 | 无 |
| `verify_enum_coverage.py` | 装饰枚举覆盖率：7 类造型枚举共 224 项，是否有从未被任何主题配方使用的死代码 | 无 |
| `verify_i18n.py` | 46 个语言目录的覆盖率统计与从未翻译的 string 清单 | 无 |
| `verify_test_imports.py` | 测试源文件的 import 缺失 / 多余 / 同包冗余（无 JDK 时替代编译器） | 无 |
| `verify_apk_contents.py` | APK 的 dex / native 库 / ABI / 资源完整性。**只对 debug 包可靠**，release 下 R8 会改写资源名，见脚本 docstring | 需 APK |

## 为什么要有这些

本项目的验证历史里有三次「结论错误」都是靠人工看漏、靠脚本抓出来的：

- 读源码 grep 就断定「`miuix-navigation3-ui` 零引用」—— 错，typealias 桥接让
  被桥接类型在源码里不带提供方包名（`verify_miuix_upgrade.py` 抓出）
- 凭记忆写 Android Lint 的 rule id —— 3 个根本不存在，构建直接失败
  （正确做法见 `docs/LINT_RULES.md`）
- 「32 套主题全覆盖已验证」—— 实际 artifact 只有 12 张，Paparazzi 同名覆盖
  （教训：**看方法名不算验证，看产物数量才算**）

## 局限

- 都不能替代**编译**。Kotlin 类型错误、Compose 编译器检查，只有 CI 能发现。
- 都不能替代**真机**。AGSL 背景、真实 blur、动画流畅度、交互、
  R8 后的运行时反射，见 `docs/VERIFICATION_MATRIX.md` 第四节。
- `verify_apk_contents.py` 的体积匹配法对 release 有效，但对
  字节数相同的不同文件会误判（概率极低）。
