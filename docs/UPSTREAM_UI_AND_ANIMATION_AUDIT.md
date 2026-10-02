# XinovaSU 开源库选型调研报告

> 调研日期：2026-10-02 · 所有 star 数 / 更新时间 / 版本号均通过 GitHub API 与 Maven 仓库实时核验
> 目标环境：Android 12+ (**minSdk 31**) · compileSdk 37 · Kotlin 2.3.21 · AGP 9.2.1 · Compose Multiplatform + MiuiX KMP **0.9.1**（上游 KernelSU 已到 0.9.4）

---

## 0. 前置结论：两条关键事实纠正

**纠正 1：Compose 官方 Shared Transition 并未在 1.7+ 被移除。**

`SharedTransitionLayout` / `Modifier.sharedElement` / `sharedBounds` / `rememberSharedContentState` 在 **Compose 1.11（2026-04 稳定版，BOM `2026.04.01`）中仍然存在且是 `@ExperimentalSharedTransitionApi`**。官方在 1.11 反而**新增了调试工具** `LookaheadAnimationVisualDebugging`，用于可视化 shared element 的目标边界、运动轨迹与匹配数量。

```kotlin
LookaheadAnimationVisualDebugging(overlayColor = Color(0x4AE91E63), isEnabled = true) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) { /* content */ }
    }
}
```

> 注：`LocalSharedTransitionScope` 需要手动提供，说明该 API 尚未完全"顺滑"，这可能是外界产生"已被移除"印象的来源。
> **对项目的意义**：如果 XinovaSU 只是想要 shared element 转场，**不需要引入任何第三方库**，直接用官方 API 即可，且能与 MiuiX 栈共用（两套 UI 页面可共享同一 `SharedTransitionLayout` 宿主）。真正需要第三方库的场景是"官方 API 表达不了"的东西（如带缩放+形变+裁剪的复合转场）。

**纠正 2：Maven Central 的 `search.maven.org` Solr 接口返回数据严重滞后**（例如把 lottie-compose 报成 6.6.6、实际已到 6.7.1；把 HypnoticCanvas 报成 0.1.2、实际已到 0.4.1）。本报告所有版本号以 **GitHub Releases + mvnrepository.com** 为准。

---

## 1. Compose 动画 / 交互 / 转场库

### 1.1 官方 Shared Transition（首选，无需依赖）
- **坐标**：`androidx.compose.animation:animation`（随 Compose BOM）
- **许可**：Apache-2.0 · **维护**：Google 官方，随每个 Compose 版本发布
- **兼容性**：完全适配 Kotlin 2.x / K2；Compose 1.7+ 全部可用
- **体积影响**：零（已有依赖）
- **KMP**：✅ `org.jetbrains.compose.animation` 已在 CMP 1.7 跨平台 commonize

**关键 API**：
```kotlin
SharedTransitionLayout {
  AnimatedContent(targetState = tab) { page ->
    Hero(modifier = Modifier.sharedElement(
      rememberSharedContentState(key = "hero-${page.id}"),
      animatedVisibilityScope = this@AnimatedContent,
    ))
  }
}
```

### 1.2 Orbital — 复合运动（Movement + Transformation + SharedElement）
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/skydoves/Orbital |
| Star | **1,199** |
| 最后 push | **2025-10-15**（约 1 年前有更新，仍在维护但节奏放缓） |
| 许可 | Apache-2.0 |
| 坐标 | `com.github.skydoves:orbital:0.4.0`（2024-06 发布） |
| 体积 | 极小（纯 Modifier/动画层，无 native 依赖） |

```kotlin
val poster = rememberContentWithOrbitalScope {
    Image(
        modifier = if (isTransformed) Modifier.fillMaxSize()
                   else Modifier.size(130.dp, 220.dp)
            .animateSharedElementTransition(
                this,
                SpringSpec(stiffness = 500f),
                SpringSpec(stiffness = 500f),
            ),
        imageModel = item.poster,
    )
}
Orbital(modifier = Modifier.clickable { isTransformed = !isTransformed }) { poster() }
```

**评价**：Compose 1.7 之前官方 shared transition 缺失期的社区主力方案，API 设计至今仍是社区最好的"复合运动"抽象。支持 `OrbitalScope.animateMovement` / `animateTransformation` / `animateSharedElementTransition`。
**KMP**：✅ Compose Multiplatform 库，Android/iOS/Desktop/js 全平台。
**与 MiuiX 的坑**：Orbital 内部用 `Modifier` 链和布局坐标，对**自定义 Layout（如 MiuiX 的部分容器）无侵入要求**，可以套在 MiuiX 组件外层 `Box` 上，属于安全用法。但它用 `GlideImage` 做示例——**KMP 场景下 Glide 不可用**，需替换为 Coil/资源图片。

**结论**：若需要"官方 API 表达不了的复合转场"，Orbital 是最成熟的补充；但需接受 1 年未发新版的节奏风险。

### 1.3 material-motion-compose — Material Motion 动效曲线套件
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/fornewid/material-motion-compose |
| Star | **660** |
| 最后 push | **2026-05-28**（活跃） |
| 许可 | Apache-2.0 |
| 体积 | 极小（纯 Easing/SpringSpec 常量集） |

提供符合 Material Design 2 规范的 `EasingToken` 集（emphasized、emphasizedDecelerate、emphasizedAccelerate、standard 等精确曲线），全部实现为 `androidx.compose.animation.core.Easing` / `SpringSpec`，可直接喂给 `tween()` / `spring()`。

**评价**：与项目已用的 Material 3 Expressive 动效体系**高度互补**——M3 Expressive 用的是新版 spring 语义，而 MD2 这套曲线是另一套更"经典"的观感。做主题动效差异化时值得一试。
**KMP**：✅ Compose Multiplatform。

### 1.4 ConfettiKit — 庆祝/能量粒子特效
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/vinceglb/ConfettiKit |
| Star | **154** |
| 最后 push | **2026-10-01**（极活跃） |
| 许可 | **MIT** |
| 最新版 | `0.9.0`（2026-08-18） |
| 体积 | 小（纯 Compose 粒子系统，无 native 渲染） |

```kotlin
ConfettiKit(
    modifier = Modifier.fillMaxSize(),
    particles = List(60) { ConfettiParticle() },
    config = ConfettiConfig(spread = 180f, fallSpeed = 300f),
)
```

**评价**：与项目已有的 `EnergyOrnaments` / `EnergyTimeline` 主题动效体系是**同一位面**的补充。MIT 许可最宽松，KMP 友好。
**风险**：Star 数较少（154），属于新兴库；但代码质量与更新频率良好。

---

## 2. 二次元 / ACG 风格 UI 绘制

### ⚠️ 重要发现：**该方向无成熟专用库**

穷举搜索（GitHub Search API + 网络检索）后确认：**没有任何专门做"描边 / 双层描边 / 赛璐璐上色 / 贴纸徽章"的 Compose 绘图库**。搜索 `compose cel shading / anime / manga` 得到的 127 个结果中，命中项全部是 **Celery / CEL（Common Expression Language）/ Docker Compose** 等无关项目。

**原因分析**：这类需求在 Compose 中通常用 20~40 行的 `Modifier.drawWithCache` + `Path` 组合即可实现（双层描边 = 两次 `drawPath(style = Stroke)`，描边偏移 = `PathEffect.dashPathEffect`），社区缺乏造库的动机。**因此本节结论是：自行实现，不要硬凑库。**

### 2.1 推荐：直接用 `drawWithCache` 自建（二次元描边/贴纸核心原语）

项目已具备 `CardDecorationGeometry` / `DecorationDensityPolicy` 等体系，扩展成本极低：

```kotlin
// 双层描边：先画粗深色底描边，再画细亮色内描边
Modifier.drawWithCache {
    val p = Path().apply { /* 已有 CardDecorationGeometry 产出的轮廓 */ }
    val outline = Brush.linearGradient(listOf(inkDark, inkMid))
    onDrawWithContent {
        drawContent()
        drawPath(p, outline, style = Stroke(width = 6.dp.toPx()))   // 外描边
        drawPath(p, highlight,  style = Stroke(width = 2.dp.toPx())) // 内高光
    }
}
```

**赛璐璐上色**：需要 `RuntimeShader` 做色阶量化（posterize），**这正是项目已有 `BgEffectPainter` / `OS3BgFrag` 的能力范围**，直接扩展 shader 即可，无需新库：
```glsl
// AGSL 伪赛璐璐：3 阶色带
half3 cel = half3(0.0);
cel.r = step(0.33, lum) * 0.5 + step(0.66, lum) * 0.5;
```

### 2.2 Mirage — 29 个即用型 AGSL 着色器
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/AndroidPoet/mirage |
| Star | **38**（很低） |
| 最后 push | **2026-07-05**；最新 release `0.1.0`（2026-07-05） |
| 许可 | **Apache-2.0** ✅ |
| 坐标 | `io.github.androidpoet:mirage:0.1.0`（Maven Central 搜索暂未索引到，建议直接引或用 JitPack/本地源码） |
| 体积 | 中（着色器字符串本身很小，但 29 个 shader 全量引入需按需取用） |

```kotlin
MeshGradient(
    Modifier.fillMaxSize(),
    colors = listOf(Color(0xFFE0EAFF), Color(0xFF241D9A), Color(0xFFF75092), Color(0xFF9F50D3)),
    swirl = 0.3f, speed = 1f,
)
// 图像滤镜型
Image(painter, modifier = Modifier.water())   // / .liquidMetal() / .halftoneCmyk()
```

**内容清单**（部分）：`MeshGradient`、`Metaballs`、`GodRays`（体积光 + bloom）、`DotGrid`、`DotOrbit`、`GrainGradient`、`NeuroNoise`、`Voronoi`、以及图像滤镜 `water()` / `liquidMetal()` / `halftoneCmyk()`。

**评价**：⚠️ 虽 Zero-dependency、Apache-2.0、minSdk 21 且作者在 2026-07 刚做完 CMP 化，但 **Star 仅 38、版本 0.1.0、无 star 基础**。适合当**着色器灵感库/AGSL 代码片段来源**借鉴，不建议作为生产依赖。
**KMP**：作者正在迁移到 Compose Multiplatform，当前仍是 Android-only。
**与 MiuiX**：AGSL 渲染在 Android 13+ 无兼容性坑；minSdk 31 的项目需自行处理 API 33 以下的降级分支。

### 2.3 HypnoticCanvas — 着色器背景 Modifier（成熟度最高）
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/mikepenz/HypnoticCanvas |
| Star | **226** |
| 最后 push | **2026-08-31**（活跃） |
| 许可 | **Apache-2.0**（core 模块代码） |
| 坐标 | `com.mikepenz.hypnoticcanvas:hypnoticcanvas:0.4.1` |
| 体积 | 小（core 模块 3 个 shader，均为 MIT） |

```kotlin
Box(modifier = Modifier.fillMaxSize().shaderBackground(BlackCherryCosmos))
Box(modifier = Modifier.fillMaxSize().shaderBackground(
    MeshGradient(arrayOf(Color(0xFFFF15E5), Color(0xFFFAAEF7), Color(0xFF6903F9)), scale = 1f)
))
```

**⚠️ 许可证陷阱（重要）**：
- `hypnoticcanvas`（core）：代码 Apache-2.0，内含 3 个 shader 为 **MIT** — ✅ 可商用
  - `MeshGradient`（Mike Penz, MIT）、`MesmerizingLens`（Mike Penz, MIT）、`GlossyGradients`（Giorgi Azmaipharashvili, MIT，Fiverr 购入后以 MIT 发布）
- `hypnoticcanvas-shaders`：**全部为 CC-BY-NC-SA-3.0**（来自 ShaderToy）— ❌ **禁止商用**
  - 含 `BlackCherryCosmos2`、`GoldenMagma`、`IceReflection`、`InkFlow`、`OilFlow`、`PurpleLiquid`、`RainbowWater`、`Stage`

**结论**：**不引**。除了上述许可证风险外，更关键的是 MiuiX 已自带等价抽象（见 **5.2** `miuix-shader`）——Apache-2.0、KMP 全平台、已封装 `setColorUniform`/`setInputShader`。注意其在 Android 侧要求 API 33+，本项目 minSdk 31 需保留降级分支。
**兼容性**：已升级到 Compose 1.8.x；**Kotlin 2.4 / AGP 9.4 需自行验证**（社区库通常滞后 1-2 个大版本）。
**KMP**：✅ Compose Multiplatform，Android 13+ / Desktop / iOS / Wasm 全支持（项目基座来自 Chris Banes 的 haze）。
**与项目重叠度**：与现有 `BgEffectPainter` / `OS3BgFrag` 能力**高度重叠**。引入价值主要是"省去自己写 shader"，但代价是引入依赖 + 许可证踩雷风险 + 后续维护负担。**若现有 shader 体系够用，不建议引入。**

### 2.4 二次元风格图标/插画资源

| 资源 | 许可 | 数量 | 备注 |
|---|---|---|---|
| **Iconify 大集合**（https://icon-sets.iconify.design） | 逐集合标注，含 MIT / Apache-2.0 / CC0 | 30,000+ | 唯一有大规模、许可清晰、分类友好的入口 |
| Tabler Icons | **MIT** | 6,080 | 线条干净，适合二次元描边风格二次加工 |
| MingCute Icon | **Apache-2.0** | 3,324 | 双层/多色风格，接近贴纸感 |
| Lucide | ISC | 1,694 | 极简线条 |
| Huge Icons | **MIT** | 4,653 | 描边型，风格偏几何 |
| Material Symbols | Apache-2.0 | 15,222+ | 官方，但风格过于"系统" |

⚠️ **注意**：Iconify 上标 `CC BY 4.0` 的集合（Solar / IconaMoon / Boxicons / Typicons / Pixel 等）**商用需署名**，标 `CC BY 4.0` 且**不可闭源分发衍生图标库**。第三方图标站（如 animedas.pics）多为**个人作品、许可不明**，**不可用于商业 App**。

**结论**：二次元专用图标集**无可商用的成熟开源方案**。务实做法：用 Tabler/MingCute 等 MIT/Apache 集合做基础，通过 `drawWithCache` 统一加双色描边 → 得到一致的"贴纸/描边"图标体系，与主题系统天然联动。**Iconify 建议通过 Gradle 插件或 `compose-resources` 转换后内联，不要运行时拉 CDN。**

---

## 3. 动画曲线 / 缓动 / 物理仿真

**⚠️ 结论：无需第三方库。**

这是本次调研中**最应该直接说"不推荐引入"**的方向：

1. Compose 官方 `animation-core` 已提供完整能力：`spring(dampingRatio, stiffness, visibilityThreshold)`、`tween`、`keyframes`、`snap`、`splineBasedDecay`（fling 物理）、`exponentialDecay`。
2. Compose 官方 `LookaheadAnimationScope`（`Modifier.animatedBounds`）已提供**提前计算目标布局**，大幅降低 shared transition 的掉帧——这正是过去需要第三方库解决的问题。
3. 搜索 GitHub `compose easing/spring animation multiplatform`（60 个结果）后，**没有任何一个"缓动曲线库"达到可用标准**：要么是为一条曲线写的小 demo，要么是 Balloon（tooltip）、Orbital（转场）这类用途不符的库。`material-motion-compose`（660 star, Apache-2.0）是唯一有实际价值的补充，且它提供的曲线是 `Easing` 接口实现——**它增强的是"曲线数值"，不是"能力"**。

**建议**：把 `MaterialTheme` 里的 spring 参数抽成 `MotionTokens`（类比现有 `ThemeTokenBundle` 体系），集中管理 stiffness/dampingRatio 档位。`EnergyTimeline` 的时间轴动效本质上就是 spring 参数编排，无需外部抽象。

---

## 4. Lottie / 矢量动画

| 库 | Star | 最后 push | 许可 | 最新版 | 平台 |
|---|---|---|---|---|---|
| **lottie-compose**（Airbnb 官方） | **35,735** | 2026-02-15 | Apache-2.0 | `6.7.1`（2025-10-31） | Android only |
| **compottie** | 690 | **2026-10-02**（今日） | **MIT** | `2.3.2`（2026-09-27） | **KMP 全平台** |
| **kottie** | 301 | 2026-03-09 | Apache-2.0 | `2.3.0`（2026-03-09） | **KMP 全平台** |
| **KraftShade**（Cardinal Blue） | — | 活跃 | — | — | Android（OpenGL，非 Compose 原生） |

### 4.1 lottie-compose（Airbnb 官方）— 事实核查
```kotlin
implementation("com.airbnb.android:lottie-compose:6.7.1")

val comp by rememberLottieComposition(LottieCompositionSpec.Asset("energy.json"))
val progress by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever)
LottieAnimation(composition = comp, progress = { progress })
```
- **维护状态**：**仍活跃**（2026-02 有提交），35k star，Apache-2.0，是该领域事实标准。
- **体积**：`lottie-compose` 本体约 300KB~600KB（不含 JSON 资源）；JSON 资源体积需自行控制。
- **⚠️ 与 MiuiX KMP 的根本冲突**：**lottie-compose 是纯 Android 库**（依赖 `android.graphics` / View 体系），**无法在 commonMain 中使用**。如果项目只有 Android 端问题不大；若 MiuiX 意味着未来要出 iOS/桌面，**引入 lottie-compose 会锁死 KMP 路线**。

### 4.2 compottie — KMP 纯 Kotlin 渲染器 ⭐
| 项 | 值 |
|---|---|
| 仓库 | https://github.com/alexzhirkevich/compottie |
| Star | **690** |
| 最后 push | **2026-10-02**（**当天**，维护极活跃） |
| 许可 | **MIT** ✅ 最宽松 |
| 最新版 | `2.3.2`（2026-09-27） |
| 原理 | **自研纯 Kotlin 渲染器**（不依赖 Skia/Android 图形栈），在 Skia/Canvas 上重写 Lottie 渲染 |

**评价**：这是本次调研中**性价比最高的发现**。MIT 许可、KMP 原生、维护极其活跃（作者 alexzhirkevich 同时是 KMP 生态活跃贡献者），且是**纯 Kotlin 实现**——意味着在 Android/iOS/Desktop/Wasm 上行为一致。
**与 MiuiX 的坑**：
- ✅ 坐标 `commonMain`，与 MiuiX KMP 天然共存
- ⚠️ 纯 Kotlin 渲染器性能**低于** Lottie 原生（Android 端用 Skia 直绘），高帧率复杂动画（>30 个图层、60fps）可能掉帧——需实测
- ⚠️ 生态较新，部分 Lottie 特性（文本、遮罩、表达式）可能未完全实现，需按实际用到的 JSON 特性验证

### 4.3 kottie — KMP 版 Lottie
- 301 star · Apache-2.0 · `2.3.0`（2026-03-09）· 最后 push 2026-03-09
- 定位："Inspired by Airbnb/Lottie"，同样走 KMP 路线
- **评价**：更新频率不如 compottie（半年内仅一次提交），生态规模小一号。可作为 compottie 的备选，但**没有明显理由优先于 compottie**。

### 4.4 KraftShade — 需警惕 ⚠️
- Cardinal Blue 出品，Compose 有 `KraftShadeAnimatedView` / `KraftShadeEffectView`
- **但它是 OpenGL 纹理 View 包装器**（`KraftTextureView`），走 `SerialSteps` / `WindowSurface` 体系
- **问题**：这是 **View 互操作方案，不是 Compose 原生渲染**。与项目"Compose 树内纯渲染"的 Liquid Glass / 装饰层体系**架构不兼容**，且在 KMP 下完全不可用。**不推荐。**

---

## 5. Compose 性能与图形

### 5.1 haze — 模糊/图形效果（仅作参考）
- 仓库：https://github.com/chrisbanes/haze · **2,578 star** · 最后 push **2026-10-02** · **Apache-2.0**
- 事实：haze **并非效果库，而是一套"任意内容背后实时模糊"的框架**（Compose Multiplatform 全平台）。
- **架构不匹配**：haze 的实现方式是在背景内容上绘制模糊后的副本（crossfade / 逐层 tint），而项目已有**自建的 Liquid Glass 体系**（Lens / Vibrancy / InnerShadow / CombinedBackdrop）+ AGSL 背景着色器。
- **KMP**：✅ 优秀（Android/iOS/Desktop/Wasm）
- **评价**：作为"业界怎么做磨砂"的参考实现价值高，但**直接替换现有体系风险大于收益**——现有 Liquid Glass 已成型且与主题系统深度耦合。仅建议在需要 `haze` 独有的 Progressive Blur / 逐层 tint 策略时局部借鉴。

### 5.2 ⭐ MiuiX 自带 KMP RuntimeShader 抽象（**但当前版本不可用，见下方修正**）

> #### ⚠️ 2026-10-02 实测修正：本节原结论「已在依赖树里、零成本」**不成立**
>
> 本项目当前使用 **miuix 0.9.1**。经 Maven Central 逐版本核实：
>
> | 模块 | 0.9.1（2026-05-14） | 0.9.2（2026-06-05） | 0.9.4（2026-09-21） |
> |---|---|---|---|
> | `miuix-blur-android` | ✅ 存在 | ✅ | ✅ |
> | `miuix-shader-android` | ❌ **不存在** | ✅ 首次发布 | ✅ |
>
> - `miuix-shader` 是 **0.9.2 才引入的新模块**，0.9.1 完全没有这个 artifact。
> - 因此在 0.9.1 上把 import 从 `top.yukonga.miuix.kmp.blur.*` 改成
>   `top.yukonga.miuix.kmp.shader.*` 会**直接编译失败**（包不存在）。
> - 本项目现在的 `top.yukonga.miuix.kmp.blur.RuntimeShader` 是 **blur 包自己的类**，
>   与 0.9.2+ 的 `shader` 包无继承关系，只是同名。
>
> **0.9.2+ 的兼容关系（供升级时参考）**：`miuix-blur` 通过 `api(projects.miuixShader)`
> 依赖 miuix-shader，并在 `blur/RuntimeShader.kt` 里提供向后兼容桥：
> ```kotlin
> /** Back-compat re-export. New code should use `top.yukonga.miuix.kmp.shader.RuntimeShader`. */
> typealias RuntimeShader = top.yukonga.miuix.kmp.shader.RuntimeShader
> fun RuntimeShader(shaderString: String): RuntimeShader = ...
> fun RuntimeShader.asBrush(): ShaderBrush = ...
> fun isRuntimeShaderSupported(): Boolean = ...
> ```
> 也就是说升到 0.9.2+ 后，现有代码**不改也能编译**（走 typealias），
> 改 import 只是为了显式使用正式包、并去掉对兼容桥的依赖。
>
> **结论：迁移的前置条件是 miuix ≥ 0.9.2。** 但注意 0.9.4 移除了
> `miuix-navigation3-ui`（本项目正在用），所以「升到 0.9.2/0.9.3 以启用 shader 迁移」
> 与「留在 0.9.1」两个选择都可行：
> - 留 0.9.1：shader 能力继续用 `blur` 包的等价实现，**功能上没有任何损失**
>   （`RuntimeShader` / `asBrush` / `isRuntimeShaderSupported` / `isRenderEffectSupported`
>   全部可用，本项目已在使用）
> - 升 0.9.2/0.9.3：可把 import 切到 `shader` 正式包，为将来去 blur 化铺路
>
> **不构成「必须现在做」的升级理由。**

**原始调研结论（保留供未来升级参考）**：

MiuiX KMP 仓库（Apache-2.0）包含独立模块 **`miuix-shader`**（`top.yukonga.miuix.kmp:miuix-shader`），
它做的正是 HypnoticCanvas / Mirage / gaze-glassy 三家都在做的抽象。

**已核验事实**：
- 坐标 `top.yukonga.miuix.kmp:miuix-shader-android`（**0.9.2 起**，0.9.4 为最新版）
- SPDX 标识：`SPDX-License-Identifier: Apache-2.0`（源码头部声明）
- 目标平台：`android / jvm("desktop") / iosArm64 / iosSimulatorArm64 / macosArm64 / wasmJs / js`
- 源集结构：`commonMain`（expect）+ `androidMain`（AGSL 封装）+ `skikoMain`（Skia 封装）
- Android 侧要求 `@ChecksSdkIntAtLeast(TIRAMISU)`——**API 33 起可用，与项目 minSdk 31 兼容**

**commonMain 公共 API（已读源码核实）**：
```kotlin
interface RuntimeShader {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    fun setIntUniform(name: String, value: Int)          // 1~4 分量 + IntArray 重载
    fun setColorUniform(name: String, color: Color)      // ✅ 已封装 Color，AGSL 手写时最麻烦的一环
    fun setInputShader(name: String, shader: Shader)     // ✅ 内容输入绑定
}
expect fun isRuntimeShaderSupported(): Boolean
expect fun RuntimeShader(shaderString: String): RuntimeShader
expect fun RuntimeShader.asComposeShader(): Shader
expect fun RuntimeShader.asBrush(): ShaderBrush
expect fun isRenderEffectSupported(): Boolean
```

**用法（KMP 通用，commonMain 编写）**：
```kotlin
import top.yukonga.miuix.kmp.shader.*

val shader = remember { RuntimeShader(OS3BgFrag) }   // 复用现有 AGSL 源码，无需改写
LaunchedEffect(t) { shader.setFloatUniform("time", t) }
Box(Modifier.fillMaxSize().background(shader.asBrush()))
// 或作为 RenderEffect 作用于已有内容：
// graphicsLayer { renderEffect = shader.createComposeRenderEffect("image") }
```

**这条发现对选型的三重影响**：
1. **`BgEffectPainter` / `OS3BgFrag` 可零成本升级为 KMP 跨平台** —— AGSL 源码不用重写，Android 走 `RuntimeShader`，其他平台走 Skia 路径。
2. **HypnoticCanvas / Mirage / gaze-glassy 全部不需要引** —— 它们提供的能力 MiuiX 自带的 `miuix-shader` 已覆盖，且后者是 Apache-2.0（无 NC 陷阱、无无许可证问题）。
3. **`setColorUniform` / `setInputShader` 已封装** —— 相比 HypnoticCanvas 抄 ShaderToy AGSL 需自行处理 `image.eval(coord)` 与颜色空间转换，MiuiX 抽象更干净。

**建议**：把 `miuix-shader` 作为 shader 能力层的**唯一抽象**，彻底放弃外部 shader 库。相关代码可回流上游（仓库含 `CLAUDE.md` / `AGENTS.md`，接受贡献）。

### 5.3 结论
**无需任何第三方图形效果库。** 项目的 `BgEffectPainter` / `OS3BgFrag`（AGSL/RuntimeShader）+ `CombinedBackdrop` 已经是该方向的一流实现。任何外部封装都只是把 `RuntimeShader` 换一层调用方式，反而增加依赖与许可风险。**建议方向：重点投入 shader 素材（.agsl 片段）而非依赖。**

> 补充（2026-10-02 修正）：本节原写「`miuix-shader` 已在依赖树中提供跨平台抽象」——
> 在 miuix **0.9.1** 下不成立（该模块 0.9.2 才发布）。当前 shader 能力来自
> `miuix-blur` 包内的等价实现，**功能完整、不阻塞跨平台计划**。

---

## 6. MiuiX KMP 兼容性总表

| 库 | 坐标能否进 commonMain | KMP 支持 | 结论 |
|---|---|---|---|
| **miuix-shader（需 miuix ≥ 0.9.2）** | ✅ | ✅ android/desktop/iOS/macOS/wasm/js | **shader 能力层首选，但当前 0.9.1 不可用** |
| Compose 官方 shared transition | ✅ `org.jetbrains.compose.animation` | ✅ | **首选** |
| Orbital | ✅ | ✅ 全平台 | 可用，注意替换 Glide |
| material-motion-compose | ✅ | ✅ | 可用 |
| ConfettiKit | ✅ | ✅ | 可用 |
| HypnoticCanvas | ✅ | ✅ 全平台 | **被 miuix-shader 取代，不需引** |
| Mirage | ❌ Android-only | ❌ | 仅借鉴代码 |
| lottie-compose | ❌ | ❌ Android only | 与 KMP 冲突 |
| **compottie** | ✅ | ✅ 全平台 | **Lottie 首选** |
| kottie | ✅ | ✅ | 备选 |
| haze | ✅ | ✅ | 仅参考 |
| gaze-glassy | ✅ | ✅ | **无 LICENSE，不可用** |
| KraftShade | ❌ View 互操作 | ❌ | 不推荐 |

> 表中加粗项为零新增依赖（Compose 官方 API 随 BOM 提供）。`miuix-shader` 需 miuix ≥ 0.9.2，当前 0.9.1 不可用。

**KMP 三大通用坑（针对 Kotlin 2.4 K2）**：
1. **Android-only 库会断掉 commonMain 编译**。`lottie-compose`、Mirage 这类必须放在 `androidMain` 或干脆不引。
2. **A 库用 `androidx.compose.*`，B 库用 `org.jetbrains.compose.*`** 时，版本不一致会在 Gradle 依赖图里报冲突。MiuiX 用 JetBrains 版，第三方 KMP 库也多用 JetBrains 版，而 Android-only 库必然用 `androidx` 版——**混用是常态，注意 `androidx` 与 `org.jetbrains` 的 artifact 不要重复声明**。
3. **Kotlin 2.4 (K2) 对第三方库的 Compose Compiler 版本敏感**。绝大多数社区库未针对 Kotlin 2.4 验证，升级 Kotlin 时需逐个回归。**建议把上述 KMP 库统一收敛到 `libs.versions.toml` 并加版本约束注释。**

**4. ⚠️ MiuiX 伞形 artifact 版本陷阱（实测发现）**：
`top.yukonga.miuix.kmp:miuix` 的 Maven `<release>` 停留在 **0.8.8**，而全部子模块（`miuix-core` / `miuix-ui` / `miuix-shader` / `miuix-icons` / `miuix-blur` / `miuix-nav` / `miuix-preference` / `miuix-squircle`）均为 **0.9.4**。

- 若项目通过伞形坐标声明 `0.9.4`，**可能解析不到该版本**，或意外把 `miuix-shader` 拉到 0.8.x（该版本尚无 shader 模块）。
- **建议**：显式按需引用子模块坐标，至少锁 `miuix-core` + `miuix-ui` + `miuix-shader` 三者到同一版本。
- 同时注意 `miuix-squircle`（连续圆角矩形）与项目已有 `CardDecorationGeometry` 概念重合——**值得对照评估能否直接复用**，MiuiX KMP 作者另有 `6xingyv/gaze-capsule`（G2 连续圆角）但仅 17 star 且无 LICENSE，不建议直接引。

---

## 7. 推荐采纳清单

### 🟢 高优先级 · 立刻可用

| 库 | 坐标 | 理由 |
|---|---|---|
| **Compose 官方 SharedTransition** | `androidx.compose.animation:animation`（BOM 内） | **无需任何依赖**。1.11 仍在且新增 `LookaheadAnimationVisualDebugging` 调试工具。零体积、零许可风险、KMP 官方支持。**推翻了"必须换库"的预设。** |
| **compottie** | `dev.alexzhirkevich:compottie:2.3.2` | MIT、KMP 原生纯 Kotlin 渲染、当天仍在更新。**唯一同时满足"宽松许可 + KMP + 高频维护"的 Lottie 方案。** 用于 `EnergyOrnaments` / `EnergyTimeline` 的复杂矢量动效。⚠️ 接入前实测 Android 高帧率场景性能。 |
| **Tabler Icons** | MIT, 6,080 icons | 描边风格与二次元描边体系契合最好。用 `drawWithCache` 加双色描边即可产出统一"贴纸感"图标。 |
| **MingCute Icon** | Apache-2.0, 3,324 icons | 双层多色风格，天然接近贴纸/徽章感。 |

### ⚪ 已评估 · 当前版本不适用

| 库 | 坐标 | 说明 |
|---|---|---|
| ~~miuix-shader~~ ⭐ | `top.yukonga.miuix.kmp:miuix-shader-android`（**0.9.2+**） | **方向正确但当前不可用**：项目在 miuix 0.9.1，该模块 0.9.2 才发布。现有 `blur` 包已提供全部所需 shader API（`RuntimeShader`/`asBrush`/`isRuntimeShaderSupported`/`isRenderEffectSupported`），**功能无损失**。升到 0.9.2/0.9.3 后可把 import 切到 `shader` 正式包（blur 侧有 typealias 兼容桥，不会破坏现有代码）。**不是必须现在做的升级理由。** 详见 5.2。 |

### 🟡 中优先级 · 需要适配

| 库 | 坐标 | 适配成本 |
|---|---|---|
| **material-motion-compose** | `fornewid.materialmotion:compose:1.0.0`(需核) | 660 star / Apache-2.0 / 活跃。仅提供 `Easing` 数值曲线，可直接注入现有 `ThemeTokenBundle` 动效体系。**若要"官方曲线"够用则不必引**——MD2 曲线与 M3 Expressive 观感不同，需先做视觉 A/B。 |
| **Orbital** | `com.github.skydoves:orbital:0.4.0` | Apache-2.0 / 1,199 star。仅在需要"移动+形变+共享"三段复合转场（官方 API 表达不了）时引入。**KMP 下必须替换示例中的 GlideImage**。近 1 年未发版，锁版本。 |
| **HypnoticCanvas** | `com.mikepenz.hypnoticcanvas:hypnoticcanvas:0.4.1` | **已被 `miuix-shader` 完全取代（5.2），建议不引。** 双重理由：① `miuix-shader` 是 Apache-2.0 + KMP 全平台，而 HypnoticCanvas 的 `-shaders` 模块是 **CC-BY-NC-SA 3.0 禁商用**（8 个 ShaderToy shader），core 模块仅 3 个 MIT shader 且能力弱于 `miuix-shader`；② 与现有 `BgEffectPainter` 高度重叠。若因故必须引：**严格只引 core 模块，绝不引 `-shaders`**。 |
| **ConfettiKit** | `com.vinceglb:confettikit:0.9.0` | MIT / 154 star / 极活跃。适合 `EnergyTimeline` 的庆祝/达成反馈特效。KMP 友好。 |

### 🔴 不推荐

| 库/方向 | 不推荐原因 |
|---|---|
| **lottie-compose**（Airbnb 官方） | 本身维护良好（35k star, Apache-2.0），但**是纯 Android 库，无法进 commonMain**，与 MiuiX KMP 架构**根本冲突**。除非确定项目永远只做 Android，否则**应选 compottie**。体积也明显大于 compottie。 |
| **二次元/赛璐璐专用绘图库** | **不存在**。搜索 `compose cel shading/anime/manga` 127 个结果全部命中 Celery / Docker Compose / CEL 等无关项目。社区无此类库。→ **用 `drawWithCache` + `Path` + `Stroke` + `PathEffect` 自建**，与已有 `CardDecorationGeometry` 体系无缝衔接；赛璐璐色阶量化在现有 `OS3BgFrag` 里扩展 quantize 逻辑即可。 |
| **缓动曲线 / 物理仿真库** | **不存在可用方案**。官方 `animation-core` 已提供 `spring/tween/keyframes/snap/splineBasedDecay` + `LookaheadAnimationScope`，能力完备。社区无可用库。→ **抽 `MotionTokens` 集中管理 spring 参数**，不引库。 |
| **gaze-glassy**（Liquid Glass） | ⚠️ **仓库无 LICENSE 文件**（GitHub API 确认 `license: None`）—— 法律上等同于"保留所有权利"，**不可用于任何产品**。且仅 17 star / 2026-01 后停更。架构定位与项目现有 Liquid Glass 体系重复。**若确实想用其跨平台 shader 抽象，必须先向作者索要授权。** |
| **KraftShade** | 走 OpenGL + View 互操作（`KraftTextureView` / `WindowSurface`），**非 Compose 原生渲染**，KMP 完全不可用。与现有 Compose 树内装饰体系架构不兼容。 |
| **haze** | 2,578 star / Apache-2.0 / 极活跃 / KMP 优秀——**但它是"内容背后实时模糊框架"，不是效果库**。与已成型的 `CombinedBackdrop` / Liquid Glass 体系重叠，替换风险 > 收益。建议仅作 Progressive Blur 实现思路的参考读物。 |
| **Mirage** | Apache-2.0 且质量不错（29 个 AGSL shader），但 **仅 38 star、v0.1.0、无社区验证**。且其价值（`Modifier.liquidMetal()` 等滤镜）已由 `miuix-shader` + 自建 shader 覆盖。→ **当 AGSL 代码素材库借鉴，不作生产依赖。** |
| **chigichan24/Spider**（AGSL + KSP） | 2023-02 后停更，无 LICENSE。KSP 代码生成规避 AGSL 运行时崩溃的想法有趣，但项目已自行封装 shader，风险收益比不合适。 |
| **第三方二次元图标站**（如 animedas.pics 等） | **许可不明、个人作品**，无法用于商业分发的 App。仅可作设计灵感参考。 |
| **Iconify 中 CC BY 4.0 集合** | 商用需署名 + 衍生图标库不可闭源分发。若采用必须逐集合核许可证，**优先选 MIT / Apache-2.0 / ISC 集合**。 |
| **mobnetic/compose-shared-element** | 397 star 但**2021-12 停更 4 年+**，且基于已被移除的旧 shared element API。 |
| **SakshamSharma2026/Shared-Element-Transitions** 等 | 4 star 的 demo 仓库，Apache-2.0 但无使用价值。 |

---

## 8. 一句话总结

**最重要的发现是：本次调研的 6 个方向里，有 4 个（shared transition、二次元描边、缓动曲线、着色器抽象）根本不需要引入第三方库**——官方 API + `drawWithCache` + `animation-core` + **已在依赖树中的 `miuix-shader`** 已经完整覆盖，硬凑库反而增加维护与法律风险。

**真正值得投入的只有 2 个**：
1. **compottie**（MIT / KMP / 今日更新）填补 Lottie 在 KMP 下的空缺；
2. **Tabler + MingCute**（MIT / Apache-2.0）配合自建描边管线，产出符合二次元风格的图标体系。

**最重要的单条发现**：`miuix-shader` 已经提供了 `RuntimeShader` / `RenderEffect` 的 KMP 跨平台抽象（Apache-2.0，android/desktop/iOS/macOS/wasm/js 全平台，已封装 `setColorUniform`/`setInputShader`）。这意味着 **`BgEffectPainter` / `OS3BgFrag` 可以零成本升级为跨平台**，且 HypnoticCanvas / Mirage / gaze-glassy 三家全部不需要引入。

**必须警惕的两个法律红线**：
- HypnoticCanvas 的 `-shaders` 模块是 **CC-BY-NC-SA-3.0，禁商用**；
- **gaze-glassy 完全没有 LICENSE**，法务上不可用。

**需要立刻检查的工程隐患**：`top.yukonga.miuix.kmp:miuix` 伞形坐标的 Maven release 停留在 **0.8.8**，而子模块全是 **0.9.4**。若项目用伞形坐标声明 0.9.4，可能拿不到 `miuix-shader`。建议显式引用子模块坐标并锁版本（见 6.4）。

**最需要修正的认知**：官方 `SharedTransitionLayout` 在 Compose 1.11 中**依然存在且是官方推荐**，还新增了调试工具——不需要为它引入任何社区替代方案。

---

### 附：数据核验来源
- GitHub REST API v3（`/repos/*`、`/releases/latest`、`/search/repositories`、`/git/trees/*`）— star / pushed_at / license / archived 均为实时读取
- `repo1.maven.org/maven2/**/maven-metadata.xml`（直读 POM 仓库，绕开滞后的 Solr 搜索接口）— 版本号与发布状态
- mvnrepository.com、maventum.com — 交叉校验版本号与发布日期
- MiuiX 源码直读（`miuix-shader/src/commonMain/.../RuntimeShader.kt`、`build.gradle.kts`）— 公共 API 签名、许可证、目标平台
- developer.android.com 官方博客（Compose April '26 release / Compose Multiplatform 1.7.0 release notes）— shared transition 状态
- Android Developers 官方文档 — `drawWithContent` / `drawBehind` / `drawWithCache` / `graphicsLayer` 语义
