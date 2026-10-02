# Lint 首次运行结果与处置

> 日期：2026-10-02 · 规则 ID 见 [LINT_RULES.md](LINT_RULES.md)
> 报告原件：CI artifact `lint-report`（`lint-results-debug.txt`）

## 一句话结论

**86 个 error 里没有一条是崩溃级。** 8 条崩溃类规则
（MissingPermission / UnspecifiedImmutableFlag / UnspecifiedRegisterReceiverFlag /
WrongThread / Recycle / StaticFieldLeak / ObsoleteSdkInt / InlinedApi）
在项目当前代码上**全部干净**。

## 已修（2 项真 bug）

| 规则 | 位置 | 性质 |
|---|---|---|
| `DefaultLocale` | `HanziToPinyin.java:536` | **真 i18n bug**。`toPinyinString()` 的返回值是搜索/排序的比较键；用默认 Locale 时 tr-TR 下 `"I".toLowerCase()` 得到 `ı`（点less i），拼音首字母排序错乱。改为 `Locale.ROOT`。 |
| `DefaultLocale` | `MimeUtil.java:52` | 同上。文件扩展名小写化后与字面量比较，`.MP3` / `.WASM` 会识别失败。改为 `Locale.ROOT`。 |
| `StringFormatCount` | `values-bs/strings.xml` | **真 bug**。`module_install_prompt_with_name` 的波斯尼亚语翻译漏了 `%1$s`，而 Kotlin 侧用 `string.format(id, name)` 传了参数 → 安装确认框里**模块名整个消失**。已补上占位符。用脚本全量扫过 46 个语言目录，**只此一处**。 |

## 误报 / 静态分析局限（2 项）

| 规则 | 数量 | 说明 |
|---|---|---|
| `NewApi` | 8 | `BgEffectPainter` 及其调用点需要 API 33，minSdk 是 31。**运行时是安全的**：`BgEffectPainter` 类上有 `@RequiresApi(TIRAMISU)`，调用链上游有 `isRuntimeShaderSupported()` 守卫（`BgEffectBackground.kt:35` 直接 return 降级分支），传给 Modifier 的 `effectBackground` 也是这个判断的结果。Lint 的数据流分析跟不穿这个间接层。 |
| `ObsoleteSdkInt` | 1 | `res/mipmap-anydpi-v26/` 目录在 minSdk 31 下多余（该目录本是为 API 26 以下做兼容）。删掉目录、把内容并回 `mipmap-anydpi/` 即可，但收益仅是目录整洁，改动会碰 7 个密度目录，留给专门的清理任务。 |

## 噪声：建议关掉或降级（82 项）

| 规则 | 数量 | 处置 |
|---|---|---|
| `NewerVersionAvailable` | 15 | 依赖有新版本 —— 纯提示，`disable` |
| `GradleDependency` | 9 | 同上 |
| `AndroidGradlePluginVersion` | 2 | AGP 有新版 —— 同上 |
| `IconLauncherShape` | 11 | 图标设计规范建议（图标不该铺满整个方形）。属设计决策，不是缺陷 |
| `Untranslatable` | 12 | 若干 string 标了 `translatable="false"` 但仍存在于翻译目录（如 `app_name`、`profile`）。是历史遗留，无运行时影响 |
| `IconDuplicates` | 5 | `ic_launcher.png` 与 `ic_launcher_round.png` 内容相同。无害（自适应图标走 `mipmap-anydpi-v26`） |
| `UnusedResources` | 14 | 未引用的资源。可减体积，但需逐个确认不是动态引用（`getIdentifier` 之类），风险大于收益 |
| `TypographyEllipsis` | 1 | 排版细节 |
| `PluralsCandidate` | 1 | 建议改用 plurals。内容优化，非缺陷 |

**这些应加入 `lint { disable += ... }`**，理由与 `MissingTranslation` 相同：
它们要么是提示类（"有新版"），要么是设计决策，要么需要人工判断 ——
让它们红只会训练团队忽略 lint 输出，反而丢掉真正重要的信号。

## 顺带确认的两件事

1. **反射路径不受 R8 影响**。项目有 10+ 处 `Class.forName` / `getMethod`，
   但目标全是系统类（`SystemProperties`、`AppOpsManager`、三星浮窗 Feature），
   不在 APK 内，混淆无关。`proguard-rules.pro` 为空这个隐患暂不成立。

2. **i18n 覆盖率**（`.workbuddy/verify_i18n.py` 实测）：
   46 个语言目录，基准 446 个需翻译的 string。
   简中 `values-zh-rCN` 427/449（95%），
   繁中 `values-zh-rTW` 仅 166/449（37%），
   最低的 `values-my`（缅甸语）只有 4 个（0.9%）。
   属内容工作，不阻塞 CI。

## 待办

- [ ] 把 12 类噪声规则加进 `lint { disable += }`，让 lint 输出只剩真信号
- [ ] `res/mipmap-anydpi-v26/` 目录清理（ObsoleteSdkInt）
- [ ] 繁体中文翻译补全（37% → 目标接近简中）
