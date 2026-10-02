# 验证矩阵：自动化已覆盖 vs 只能真机

> 日期：2026-10-02 · 对应本轮 8 次提交
> 原则：把能在无设备环境下发现的问题全部前移，剩下的才是真机的价值所在。

## 一、环境事实（决定了验证手段的选择）

本机（Windows）：

| 项 | 状态 | 影响 |
|---|---|---|
| JDK | **无**（`Program Files/Java`、`Eclipse Adoptium` 均不存在） | 无法本地编译/跑 Gradle |
| Android SDK | **无**（`ANDROID_HOME` 空，无 sdkmanager） | 无法本地渲染 |
| adb | 已安装，但 `adb devices` **为空** | 无法装机截图 |
| GitHub Actions | 有 JDK 21 + Android SDK | **唯一可用的自动化环境** |

因此所有编译/渲染/静态分析都搬到了 CI。

## 二、已自动化覆盖（无需真机）

### 1. 编译与打包 — `Build Manager APK`

| 项 | 状态 |
|---|---|
| manager 编译（debug + release） | ✅ CI #38 起持续绿 |
| LKM 编译（7 个 KMI：android12-5.10 … android16-6.12） | ✅ 全绿 |
| 静态 xnsuinit + PT_INTERP 硬断言 | ✅（防刷机变砖） |
| xnsusd → libxnsusd.so | ✅ |
| APK 内 `libxnsusd.so` 冒烟检查 | ✅ |

### 2. UI 视觉 — `UI Snapshots`（Paparazzi / LayoutLib）

**实际产出 58 张 PNG**（不是 12 张 —— 早期版本的循环 snapshot 会同名覆盖，
只留每组最后一张；已修，详见下方「覆盖度修正」）。

| 维度 | 产出 | 备注 |
|---|---|---|
| 主题 | **32/32 张** | id 全部核对过，含大小写敏感的 `Sakura` / `SakuraVN` / `Cyber` |
| 卡片角色 | **5 张** | Hero / Monitor / Function / Standard / Compact |
| 形变过渡关键帧 | **6 张** | p=0 / 20 / 38 / 50 / 75 / 100，含 StyleSwapShare=0.34 两侧 |
| 深色 / 浅色 | 2 张 | |
| 玻璃态 vs 实底 | 2 张 | |
| RTL LTR / RTL | 2 张 | `CardDecorationGeometry` 的 logicalX 镜像分支 |
| 大字体 1.0 / 1.5 | 2 张 | fontScale≥1.3 会关闭二级装饰 |
| 手机 / 平板 / 折叠屏 | 3 张 | 450×1000 / 1000×625 / 833×1000 |
| 光扫强度曲线 | 1 张 | 24 根柱，肉眼确认平滑升降、无跳变 |
| 早期人工比对（3 套） | 3 张 | |
| 配方映射完整性 | 断言 | 加 recipe 忘了加映射会立刻红 |
| 造型切换点 | 断言 | eased<0.34 为旧配方，超过为新 |

> **覆盖度修正记录**：方法名写着 `allThemesRender` 不等于验证了 32 套主题 ——
> Paparazzi 按「类名_方法名」命名产物，同一测试方法内多次 `snapshot()` 会互相覆盖。
> 修复前 artifact 里只有 12 张，实际每组只验证了最后一张。
> **看方法名不算验证，看产物数量才算。**

### 3. 单元测试

现有 9 个 + 新增 3 个，共 12 个测试类（`testDebugUnitTest`）：
装饰几何、密度策略、时间轴、形变归约、目录一致性、快照、配方映射完整性。

### 4. Android Lint（首次接入）

8 条崩溃类规则设为 error，当前代码**全部干净**：

```
MissingPermission / UnspecifiedImmutableFlag / UnspecifiedRegisterReceiverFlag
WrongThread / Recycle / StaticFieldLeak / ObsoleteSdkInt / InlinedApi
```

`warningsAsErrors = true`，报告作为 artifact 上传。

### 5. 静态脚本（10 个，`.workbuddy/`）

| 脚本 | 查什么 | 本轮结论 |
|---|---|---|
| `verify_theme_morph.py` | 形变纯函数 31 项断言 | 全过 |
| `verify_miuix_upgrade.py` | 两版 miuix AAR 符号比对 | 112 符号全保留 |
| `verify_miuix_signatures.py` | 组件参数签名 diff | 见文档 |
| `verify_expressive_api.py` | material3 alpha19 二进制符号 | 10/10 存在 |
| `verify_so_contract.py` | 原生库命名三处一致性 | 已一致 |
| `verify_textfield_params.py` | TextField 参数迁移 | 12 调用点全清 |
| `verify_enum_coverage.py` | 装饰枚举覆盖率 | **224/224，无死代码** |
| `verify_i18n.py` | 46 个语言目录覆盖率 | 简中 95%、繁中 TW 37% |
| `verify_test_imports.py` | 测试文件 import 完整性 | 本机无 JDK 的替代兜底 |
| `verify_apk_contents.py` | APK 结构/ABI/资源 | 需 APK 产物 |

## 三、本轮自动发现并修复的真实 bug

| # | 问题 | 影响 |
|---|---|---|
| 1 | `values-bs` 的 `module_install_prompt_with_name` 漏 `%1$s` | **安装确认框里模块名整个消失**（46 个语言全量扫描确认只此一处） |
| 2 | `HanziToPinyin.java:536` 用默认 Locale 小写化 | tr-TR 下 `"I"→"ı"`，拼音首字母排序错乱 |
| 3 | `MimeUtil.java:52` 同上 | `.MP3` / `.WASM` 扩展名识别失败 |

## 四、只能真机验证的（已确认无替代方案）

### A. AGSL / RuntimeShader 动态背景
LayoutLib 不渲染 `RuntimeShader`，截图里没有流光背景。
**要测**：主题切换时背景动画是否播放、`OS3BgFrag` 在各 GPU 驱动上的表现、
Android 12（API 31）以下是否正确降级（本项目 minSdk 31，而 shader 需 33）。

### B. 真实 blur 玻璃态
MiuiX 的 `textureBlur` 依赖 `RenderEffect`，LayoutLib 不支持。
**要测**：开启"玻璃卡片"后的真实模糊效果、滚动时 backdrop 是否跟随、
不同设备上的性能。

### C. 动画流畅度
逐帧渲染只验证"每帧画面对不对"，**不验证时间驱动是否掉帧**。
**要测**：
- 主题形变 760ms 是否跟手（`Animatable` + `tween`）
- `EnergyTimeline` 的 5 段（压缩/充能/注入/共振/稳定）节奏是否自然
- 列表滚动时装饰层是否掉帧
- 低端机（低内存）降级策略是否生效

### D. 交互
**要测**：下拉刷新、滑动删除、拖拽排序（`ReorderableColumn`）、
点击态反馈、`Predictive Back`（返回手势）。

### E. R8 混淆后的运行时行为
静态检查只能看到 `proguard-rules.pro` 为空这个表象。
**要测**：release APK 装上后是否一切正常（AIDL binder、MiuiX 反射、
kotlinx.serialization 的 35 个 `@Serializable` 类）。

### F. 其他
- 主题切换时装饰是否"串味"（截图是逐主题独立渲染的，没测连续切换）
- Home / SuperUser 等**真实页面**（快照只渲染了孤立的卡片组件）
- 首次安装 / 升级安装的迁移路径
- 多用户 / 不同 Android 版本

## 五、建议的真机测试顺序

按"信息量 / 耗时"排序，前两步能拦掉大部分问题：

1. **release APK 装上先能开**（20 分钟）
   覆盖 E（混淆）+ 基本启动。崩溃的话后面都免谈。

2. **主题切换 + 玻璃卡片**（30 分钟）
   覆盖 A/B/C 的主体。切 5-6 套风格差异大的主题
   （Sakura / Cyber / Obsidian / Ember / Jade / SakuraVN），
   每套看：背景动画、装饰边框、切换过渡是否流畅。

3. **大字体 + RTL + 平板**（20 分钟）
   系统设置里调字体到最大、切 RTL 语种。截图已验证静态画面，
   这里验证真实布局是否被裁切。

4. **真实页面浏览**（30 分钟）
   Home / SuperUser / Module / Settings 逐页翻，看装饰层是否错位。

5. **LKM 相关**（需要刷机，本次不涉及）
   7 个 KMI 的 `.ko` 已编译通过，但加载/卸载只有真机能测。

## 六、还没做但可以做的自动化

如果想让自动化覆盖更多，尚未启用的：

- **detekt / ktlint**：Kotlin 复杂度与风格，可发现过长函数
- **Paparazzi 真实页面快照**：`HomePager` / `SuperUserPager` 等整页渲染，
  而不只是孤立卡片。需要先解决 ViewModel 依赖（可用 `Paparazzi` 的
  `RenderExtension` 注桩）
- **verifyPaparazziDebug 视觉回归**：把关键截图入库做基线，
  卡住"编译过但界面坏"。需先解决 LayoutLib 版本漂移导致的基线抖动
- **依赖 CVE 扫描**（`dependency-check`）
- **APK 体积回归**：每次构建记录体积，超阈值告警
