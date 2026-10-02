# miuix 升级评估：0.9.1 → 0.9.4

> 评估日期：2026-10-02 · 结论：**移除一个空引用依赖后即可升级，无代码改动**

## 一、先更正一个此前的错误结论

此前记录写的是：

> miuix 0.9.4 移除了 `miuix-navigation3-ui`（本项目正在用），
> 直接升 0.9.4 会编译失败，需迁移到 `miuix-nav`。

**这个说法是错的。** 实际核查（全仓库 grep + 依赖溯源）结论相反：

| 事实 | 证据 |
|---|---|
| 全仓库对 `top.yukonga.miuix.kmp.navigation3.*` 的 import 数为 **0** | `grep -rn "miuix.kmp.navigation3" --include=*.kt` 无结果 |
| `NavDisplay` / `LocalNavAnimatedContentScope` 来自 **androidx.navigation3.ui** | import 为 `androidx.navigation3.ui.NavDisplay`，且 `libs.versions.toml` 里没有 `androidx-navigation3-ui` 坐标 → 由 compose-bom 提供 |
| `NavKey` / `entryProvider` / `rememberSaveableStateHolderNavEntryDecorator` 来自 **androidx.navigation3.runtime** | 显式声明 `androidx.navigation3:navigation3-runtime:1.1.1` |
| 导航的 push/pop/replace/setResult/observeResult 由**本项目自实现** | `ui/navigation3/Navigator.kt`（146 行），只依赖 `NavKey` 一个类型 |

即导航层是「**AndroidX navigation3 + 自建 Navigator**」，
与 MiuiX 的导航模块没有任何关系。`miuix-navigation3-ui` 是一条**空引用声明**
（历史遗留），已移除（提交 `3b7ee7b`）。

## 二、移除后，0.9.4 的阻塞点是否清除

0.9.4 release notes 列出的破坏性变更，逐条对本项目核验：

| 0.9.4 变更 | 对本项目影响 | 依据 |
|---|---|---|
| 移除 `miuix-navigation3-ui` | **无** | 零 import，已移除声明 |
| minSdk 23 → 24 | **无** | 本项目 minSdk 31 |
| `RadioButtonPreference` 签名变化 | **无** | 全仓库未使用该组件 |
| `NavigationRail` 的 `mode` 枚举 → `state` | **无** | `NavigationRailMiuix.kt:36` 只传 `modifier` / `color`，未用 `mode`；也未引用 `NavigationRailDisplayMode` |
| `SnackbarColors` 新增 `actionContainerColor` | **无** | 未直接构造 `SnackbarColors(...)` |
| `TextButton` 位置参数前插入 `textStyle` | **无** | 3 处调用（`ChooseKmiDialogMaterial.kt:47`、`ChooseKmiDialogMiuix.kt:76/85`）全部用命名参数（`onClick =` / `enabled =`） |
| `PullToRefreshState` 语义细分 | **低风险** | 18 处使用，但 `rememberPullToRefreshState()` 入口保留，破坏性的是 `progress` 的公开字段细分（`pullProgress` / `fullDragProgress` / `visualProgress`） |

**结论：主要阻塞点已全部清除。**

## 三、仍需注意的两点

1. **Kotlin 版本耦合**：0.9.4 对应 Compose Multiplatform 1.12.0-rc01 / Kotlin 2.4.10，
   而本项目是 Kotlin 2.3.21 / AGP 9.2.1。升级 miuix 时大概率要连带抬 Kotlin、
   AGP、compose-bom（当前 2026.05.00）。**这才是主要成本**，而非 API  breakage。
2. **KMP 一致性**：0.9.x 各子模块必须锁同一版本。项目已正确使用子模块坐标
   （`miuix-ui-android` 等）而非伞形 `miuix`（后者的 maven `<release>` 仍停在 0.8.8），
   保持这个做法即可。

## 四、建议的升级路径

不建议一步跳到 0.9.4，分两步以便定位问题：

### 第一步：0.9.1 → 0.9.2（低风险，先验证 shader 迁移）

0.9.2 的意义在于 `miuix-shader` 模块首次发布（0.9.1 没有）。
升到 0.9.2 后可把 shader 相关 import 从向后兼容的
`top.yukonga.miuix.kmp.blur.*` 切到正式的 `top.yukonga.miuix.kmp.shader.*`：

```
ui/component/liquid/Lens.kt                        isRuntimeShaderSupported
ui/component/miuix/effect/BgEffectBackground.kt   isRuntimeShaderSupported
ui/component/miuix/effect/BgEffectPainter.kt       RuntimeShader, asBrush
ui/screen/about/AboutMiuix.kt                      isRuntimeShaderSupported
ui/MainActivity.kt                                 isRenderEffectSupported
ui/util/BlurExt.kt                                 isRenderEffectSupported
```

共 7 处。**不改也能编译**（`miuix-blur` 用 `api(projects.miuixShader)` 依赖
miuix-shader，并提供 typealias 兼容桥），改是为了去掉对兼容桥的依赖。

### 第二步：0.9.2 → 0.9.4（需连带抬 Kotlin/AGP）

先确认第一批 miuix 升上去后 Gradle 报的 Kotlin 版本要求，再决定抬到哪一版。
0.9.4 官方标注 Kotlin 2.4.10 / Compose MP 1.12.0-rc01。

## 五、不建议做的事

- **不要为了升级去迁到 `miuix-nav`**。本项目导航层与 MiuiX 导航无关，
  换成 `miuix-nav` 意味着重写 `Navigator.kt`（146 行）+ 全部 26 处 `push`、
  19 处 `pop`、17 处 `entry<>`，收益仅是转场动画，而 androidx.navigation3
  自身的 transition/decorator 机制同样能实现。
- **不要引外部 shader 库**。`miuix-shader` 是 Apache-2.0、KMP 全平台、
  API 33+ 可用，且 `setColorUniform` / `setInputShader` 已封装。
  详见 `docs/UPSTREAM_UI_AND_ANIMATION_AUDIT.md`。

## 六、验证方式

每次升级后跑 GitHub Actions（`Build Manager APK`）。注意 CI 的
`paths` 过滤包含 `manager/**`，改 `libs.versions.toml` 会触发。

`.workbuddy/verify_so_contract.py` 与 `.workbuddy/verify_expressive_api.py`
可在本机先跑一遍静态检查，但**编译验证只能靠 CI**（本机无 JDK / Android SDK）。
