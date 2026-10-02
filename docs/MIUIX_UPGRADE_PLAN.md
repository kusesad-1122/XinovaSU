# miuix 升级评估：0.9.1 → 0.9.4

> 评估日期：2026-10-02 · 状态：**被 CI #35 证伪过一次，以下是修正后的结论**

## 一、此前结论错在哪

第一版评估写的是「`miuix-navigation3-ui` 零 import，可安全移除，移除后 0.9.4 阻塞点全清」。

**这个判断是错的。** 我用 `grep "miuix.kmp.navigation3" --include=*.kt` 搜源码 import，
得到 0 命中，就断定该依赖未被使用。但项目里写的是：

```kotlin
import androidx.navigation3.ui.NavDisplay          // MainActivity.kt:53
import androidx.navigation3.ui.LocalNavAnimatedContentScope  // DeferredContent.kt:8
```

源码里确实不带 miuix 包名 —— 但这些类型是 `miuix-navigation3-ui`
**通过 typealias 桥接转发**的。移除该依赖后 CI #35 立刻失败：

```
e: MainActivity.kt:53:29     Unresolved reference 'ui'
e: MainActivity.kt:230:25    Unresolved reference 'NavDisplay'
e: DeferredContent.kt:8:29   Unresolved reference 'ui'
e: DeferredContent.kt:24:17  Unresolved reference 'LocalNavAnimatedContentScope'
```

已用提交 `b2fee65` revert 回滚。

**方法论教训**：判断「某依赖是否被使用」，只搜源码 import 不够。
typealias / 兼容桥会让被桥接类型在源码里完全不带提供方的包名。
必须查依赖本身的二进制内容：

```bash
curl -sSL -o n.aar https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-navigation3-ui-android/0.9.1/miuix-navigation3-ui-android-0.9.1.aar
# 解出 classes.jar 后搜索
#   androidx/navigation3/ui      True
#   NavDisplay                   True
#   LocalNavAnimatedContentScope True
```

同理，`compose-bom` 并不包含 `navigation3-ui`（BOM 只覆盖 compose 家族），
所以 `libs.versions.toml` 里没有 `androidx-navigation3-ui` 坐标**不能**推出
「没人显式引入」—— 它是被 miuix 传递进来的。

## 二、那 0.9.4 到底能不能升

**能，但需要先解除 miuix-navigation3-ui 的绑定。** 三条路：

### 路线 A：改用 AndroidX 官方 navigation3-ui（推荐）

显式声明 `androidx.navigation3:navigation3-ui`，替换掉 miuix 的桥接层。
源码 import 几乎不用改（本来就是 `androidx.navigation3.ui.*`），
只需：
1. `libs.versions.toml` 加 `androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }`
2. `app/build.gradle.kts` 加 `implementation(libs.androidx.navigation3.ui)`
3. 移除 `miuix-navigation3-ui`
4. 验证 miuix 0.9.4 下 `NavDisplay` 的 transition/decorator 行为与
   0.9.1 的 miuix 桥接层是否一致（这是唯一的不确定点）

代价：一次编译验证。收益：彻底摆脱 miuix 导航绑定，且本来就是 AndroidX 上游。

### 路线 B：先升到 0.9.3，验证无破坏后再评估

0.9.3 尚未移除 `miuix-navigation3-ui`（移除发生在 0.9.4-rc01），
可作为过渡版本。适合想先拿到 0.9.x 其他改进（progressive blur、
BreadcrumbBar、Badge/Tooltip）但暂不动导航栈的情况。

### 路线 C：直接上 0.9.4 + 一并迁到 miuix-nav

成本最高：`Navigator.kt`（146 行）+ 26 处 `push` + 19 处 `pop` + 17 处 `entry<>` 全部重写，
且新 API（`rememberNavBackStack` / `NavDisplay` / `NavController`）与
`androidx.navigation3` 无兼容层。**除非确实需要 MiuiX 风格转场动画，否则不建议。**

## 三、路线 A 的前置核查

| 项 | 状态 |
|---|---|
| 源码 import 是否已是 `androidx.navigation3.ui.*` | ✅ 是（MainActivity.kt:53、DeferredContent.kt:8） |
| `NavKey` / `entryProvider` / `rememberSaveableStateHolderNavEntryDecorator` 来源 | ✅ `androidx.navigation3.runtime:1.1.1`，已显式声明 |
| `rememberViewModelStoreNavEntryDecorator` 来源 | ✅ `androidx.lifecycle:lifecycle-viewmodel-navigation3`，已显式声明 |
| 自建 `Navigator.kt` 是否依赖 miuix | ✅ 只依赖 `androidx.navigation3.runtime.NavKey` |
| `androidx.navigation3:navigation3-ui:1.1.1` 是否存在 | ✅ 已核实（Google Maven，见下） |

**注意**：`androidx.navigation3` 只在 **Google Maven** 上，不要去 Maven Central 查 ——
Central 没有这个组，会全部 404，看起来像"不存在"。已验证：

```bash
curl -sS https://dl.google.com/dl/android/maven2/androidx/navigation3/navigation3-ui/maven-metadata.xml
```

`navigation3-ui` 与 `navigation3-runtime` 的版本序列完全一致
（1.1.0 … 1.1.7，之后 1.2.0 / 1.3.0-alpha01），所以锁 1.1.1 可与现有 runtime 对齐。

因此路线 A 的改动量确定为：libs 加一个坐标 + build.gradle 加一行依赖 +
移除 miuix-navigation3-ui，共 3 处，之后一次 CI 验证。

## 四、0.9.4 其他破坏性变更对本项目的影响

以下结论**仍然有效**（当时逐条核验过，与 navigation3-ui 无关）：

| 0.9.4 变更 | 影响 | 依据 |
|---|---|---|
| minSdk 23 → 24 | 无 | 本项目 minSdk 31 |
| `RadioButtonPreference` 签名变化 | 无 | 全仓库未使用 |
| `NavigationRail` 的 `mode` 枚举 → `state` | 无 | `NavigationRailMiuix.kt:36` 只传 `modifier`/`color`，未用 `mode` |
| `SnackbarColors` 新增 `actionContainerColor` | 无 | 未直接构造 `SnackbarColors(...)` |
| `TextButton` 位置参数前插入 `textStyle` | 无 | 3 处调用全用命名参数 |
| `PullToRefreshState` 语义细分 | 低风险 | 18 处使用，但入口 `rememberPullToRefreshState()` 保留 |

## 五、真实成本在 Kotlin 版本，不在 API

0.9.4 对应 Compose Multiplatform 1.12.0-rc01 / Kotlin 2.4.10，
本项目是 Kotlin 2.3.21 / AGP 9.2.1 / compose-bom 2026.05.00。
升 miuix 大概率要连带抬这三者。**这才是主要工作量。**

## 六、与 shader 迁移的关系

`miuix-shader` 首次发布是 **0.9.2**（0.9.1 无此 artifact），
所以 shader import 迁移（7 处，从 `miuix.kmp.blur.*` 切到 `miuix.kmp.shader.*`）
必须 ≥ 0.9.2。`miuix-blur` 用 `api(projects.miuixShader)` 依赖它并提供 typealias
兼容桥，所以**不改也能编译**，改只是为了去掉对兼容桥的依赖。

若走路线 A，建议顺序：先解掉 navigation3-ui 绑定 → 升 0.9.2 做 shader 迁移
→ 再评估 0.9.4。每步一次 CI 验证。

## 七、验证方式

改 `libs.versions.toml` / `build.gradle.kts` 会触发 CI（`paths` 含 `manager/**`）。
看 `Build Manager APK` 的 manager job 是否走到 `BUILD SUCCESSFUL`。

本机无 JDK / Android SDK，**编译验证只能靠 CI**。
`.workbuddy/` 下的静态校验脚本（`verify_so_contract.py`、
`verify_expressive_api.py`）能在提交前抓一部分问题，但不能替代编译。
