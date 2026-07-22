# Lavish Theme Card Energy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 XinovaSU 管理器的 32 套内置主题提供各自专属、华丽且可辨识的卡片装饰，并让全部二态 `Switch` 在开启或关闭时播放主题匹配的“能量注入”动画，同时保持 Material 与 Miuix 的既有交互、玻璃材质和业务状态语义。

**Architecture:** 从最新 `origin/main`（必须包含 PR #37 的玻璃卡片）建立隔离工作区。新增渲染器无关的 `ThemeDecorationSpec`/目录、纯 Kotlin 动画状态规划器和性能密度策略；Material `TonalCard` 与 Miuix `MiuixGlassCard` 只负责把当前配色转换成统一装饰颜色并挂载同一个装饰宿主。平台开关仍由 Material3 `Switch` 和 Miuix 0.9.1 `Switch` 承担点击、拖动、触觉和无障碍语义，新增公共绘制层只消费 `checked` 并向最近卡片宿主广播一次局部共振。

**Tech Stack:** Kotlin 2.3.21、Jetpack Compose BOM 2026.05.00、Material3 1.5.0-alpha19、Miuix 0.9.1、Android API 31+、JUnit/kotlin.test、Python 3 源码守卫、GitHub Actions `build-manager.yml` + 独立轻量 `theme-contracts.yml`。

## Global Constraints

- 只改 `manager/`、`scripts/` 和新增 `.github/workflows/theme-contracts.yml`；不得改 `kernel/`、`userspace/`、现有 workflow、签名文件、Secrets 或发布逻辑。
- `kernel/Kbuild` 必须始终保留 `XNSU_EXPECTED_SIZE := 0x037d` 与 `XNSU_EXPECTED_HASH := 4861a86778da1deb60f19391f97ea2c01c1458de20662ff78e303127cdf1731a`。
- 以 `origin/main` 为代码地基，不从旧的 `codex/theme-personalization-phase-a` 继续开发；该 Phase A 已经通过 PR #34 squash 合入，`origin/main` 还有 PR #35～#37 的后续改动。
- 不重写 Material3/Miuix 原生开关行为，不延迟 `onCheckedChange`，不把动画进度当业务状态。
- 装饰层不接收指针事件、不增加可聚焦语义节点；文字、数字、触摸区域优先于装饰完整度。
- 不在本机运行 `assembleRelease`、全量 Android 编译或其他重负载任务。每个本地阶段只运行 Python 守卫和文本级检查；Gradle 单元测试及 release 编译由 PR 的 Build Manager CI 执行。
- 视觉“99%适配”和真机帧率是主观/设备相关验收，除结构和降级契约外标为 `[UNVERIFIED]`，必须通过 32 主题预览矩阵与代表性真机检查关闭。

---

## File Map

### New production files

- `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpec.kt` — 渲染器无关的数据模型、32 个 motif 枚举、卡片角色及结构指纹。
- `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalog.kt` — 32 个显式配方与默认/自定义主题的确定性回退。
- `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducer.kt` — 首次组合、开启、关闭、快速反转、禁用与减少动画的纯 Kotlin规划。
- `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicy.kt` — 按卡片角色、低内存、省电和减少动画计算粒子/环境运动预算。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/DecorationColors.kt` — Material/Miuix 统一装饰色槽。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergyPulseHost.kt` — 最近卡片共振宿主与 CompositionLocal。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/ThemedCardDecoration.kt` — 卡片装饰层叠、裁剪、安全区和局部宿主。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/ArtworkOrnaments.kt` — 21 套立绘主题的静态装饰绘制。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/PaletteOrnaments.kt` — 11 套配色主题的静态装饰绘制。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergyOrnaments.kt` — 32 个 motif 的注能轨迹与粒子绘制。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergySwitchVisual.kt` — 平台无关的开关光轨、状态动画和最近宿主发布。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitch.kt` — 复用原生 Miuix `Switch` 的视觉包装。
- `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitchPreference.kt` — 与 Miuix 0.9.1 `SwitchPreference` 信息层级/语义等价的本地实现。
- `manager/app/src/debug/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationPreviewMatrix.kt` — 32 主题、五卡片角色和两渲染器预览入口。
- `scripts/check_theme_decoration_guards.py` — 原生开关逃逸、卡片宿主逃逸、目录覆盖与锚点保护守卫。

### Modified production files

- `manager/app/src/main/java/com/xinsu/moe/ui/MainActivity.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/theme/Theme.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/component/material/TonalCard.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/component/material/ExpressiveSwitch.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/MiuixGlassCard.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/component/profile/AppProfileConfigMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/appprofile/AppProfileMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/colorpalette/ColorPaletteScreenMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMaterial.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMaterial.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMaterial.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMiuix.kt`
- `manager/app/src/main/java/com/xinsu/moe/ui/screen/settings/SettingsMiuix.kt`
- `.github/workflows/theme-contracts.yml`

### New tests

- `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpecTest.kt`
- `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalogTest.kt`
- `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducerTest.kt`
- `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicyTest.kt`

---

## Task 1: 建立正确地基和隔离工作区

**Files:**

- Verify: `kernel/Kbuild`
- Import: `docs/superpowers/specs/2026-07-19-lavish-theme-card-energy-design.md`
- Import: `docs/superpowers/plans/2026-07-19-lavish-theme-card-energy.md`

- [ ] **Step 1: 获取并验证最新主线**

在仓库根目录运行：

```powershell
git fetch origin main --tags
git merge-base --is-ancestor a7ae7bb9ba7e57f8ba1f79ff816e2db5b716b246 origin/main
git show -s --format='%h %s' origin/main
git grep -n -F 'XNSU_EXPECTED_SIZE := 0x037d' origin/main -- kernel/Kbuild
git grep -n -F 'XNSU_EXPECTED_HASH := 4861a86778da1deb60f19391f97ea2c01c1458de20662ff78e303127cdf1731a' origin/main -- kernel/Kbuild
```

Expected: `merge-base` 退出码为 0；日志不早于 PR #37；两条 `git grep` 各只命中一次。如果任一条件失败，停止，不创建工作区。

- [ ] **Step 2: 确认目标分支和路径均未被占用**

```powershell
git show-ref --verify --quiet refs/heads/codex/lavish-theme-card-energy
$branchExists = $LASTEXITCODE -eq 0
$pathExists = Test-Path -LiteralPath '.worktrees\lavish-theme-card-energy'
if ($branchExists -or $pathExists) { throw 'target branch or worktree already exists' }
```

Expected: 无异常。

- [ ] **Step 3: 从 `origin/main` 创建隔离工作区**

```powershell
git worktree add '.worktrees\lavish-theme-card-energy' -b codex/lavish-theme-card-energy origin/main
git -C '.worktrees\lavish-theme-card-energy' status --short --branch
```

Expected: 当前分支为 `codex/lavish-theme-card-energy`，工作树为空。

- [ ] **Step 4: 只导入已经确认的设计与本计划**

```powershell
git -C '.worktrees\lavish-theme-card-energy' checkout codex/theme-personalization-redesign -- `
  docs/superpowers/specs/2026-07-19-lavish-theme-card-energy-design.md `
  docs/superpowers/plans/2026-07-19-lavish-theme-card-energy.md
git -C '.worktrees\lavish-theme-card-energy' add docs/superpowers
git -C '.worktrees\lavish-theme-card-energy' commit -m 'docs(manager): record lavish theme card energy plan'
Set-Location (Resolve-Path '.worktrees\lavish-theme-card-energy')
```

Expected: 提交仅含两份 Markdown，没有生产代码；此后所有未带 `git -C` 的命令都在新工作区执行。

---

## Task 2: 锁定装饰模型和结构指纹

**Files:**

- Create: `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpecTest.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpec.kt`

- [ ] **Step 1: 先写失败测试**

测试必须覆盖：五个 `DecoratedCardRole`；32 个 `OrnamentMotif`；`structuralFingerprint()` 忽略 `themeId` 与色槽、但会对 frame/pedestal/ambient/energy/layout 任一结构变化给出不同结果。

```kotlin
class ThemeDecorationSpecTest {
    @Test
    fun `card roles are stable and complete`() {
        assertEquals(
            listOf("Hero", "Monitor", "Function", "Standard", "Compact"),
            DecoratedCardRole.entries.map { it.name },
        )
    }

    @Test
    fun `all dedicated motifs exist`() {
        assertEquals(32, OrnamentMotif.entries.size)
        assertEquals(32, OrnamentMotif.entries.map { it.name }.toSet().size)
    }

    @Test
    fun `structural fingerprint excludes colors but includes geometry`() {
        val base = sampleSpec()
        val recolored = base.copy(
            themeId = "renamed",
            accents = base.accents.reversed(),
            frame = base.frame.copy(accent = AccentRole.Inverse),
            iconPedestal = base.iconPedestal.copy(accent = AccentRole.SurfaceGlow),
            energy = base.energy.copy(accents = listOf(AccentRole.Tertiary)),
        )
        assertEquals(
            base.structuralFingerprint(),
            recolored.structuralFingerprint(),
        )
        val structuralVariants = listOf(
            base.copy(frame = base.frame.copy(lineCount = base.frame.lineCount + 1)),
            base.copy(iconPedestal = base.iconPedestal.copy(ringCount = base.iconPedestal.ringCount + 1)),
            base.copy(ambient = base.ambient.copy(amplitude = base.ambient.amplitude + 0.1f)),
            base.copy(energy = base.energy.copy(direction = EnergyDirection.EndToStart)),
            base.copy(layout = base.layout.copy(safeInsetFraction = 0.2f)),
        )
        structuralVariants.forEach { variant ->
            assertNotEquals(base.structuralFingerprint(), variant.structuralFingerprint())
        }
    }

    private fun sampleSpec() = ThemeDecorationSpec(
        themeId = "ink-white-companions",
        motif = OrnamentMotif.InkPanels,
        frame = FrameRecipe(FrameStyle.PanelSplit, 2, 1, true, AccentRole.Outline),
        iconPedestal = IconPedestalRecipe(PedestalStyle.InkStamp, 1, AccentRole.Primary),
        ambient = AmbientRecipe(AmbientStyle.Halftone, DriftAxis.Horizontal, 0.2f),
        energy = EnergyRecipe(
            EnergyPathStyle.BrushSweep,
            ParticleStyle.InkDot,
            EnergyDirection.StartToEnd,
            10,
            0f,
            listOf(AccentRole.Primary, AccentRole.SurfaceGlow),
        ),
        layout = CardLayoutRecipe(TitleRailStyle.CaptionStrip, BadgeAnchor.TopEnd, 0.14f),
        accents = listOf(AccentRole.Primary, AccentRole.Outline),
    )
}
```

CI command（暂时应失败，因为生产类型尚不存在）：

```bash
cd manager
./gradlew :app:testDebugUnitTest --tests 'com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpecTest'
```

Expected: Kotlin unresolved-reference 编译失败。

- [ ] **Step 2: 实现最小、不可变的数据模型**

`ThemeDecorationSpec.kt` 必须包含以下公共形状；枚举名称必须原样使用，后续目录和渲染器据此做穷尽 `when`：

```kotlin
@Immutable
data class ThemeDecorationSpec(
    val themeId: String,
    val motif: OrnamentMotif,
    val frame: FrameRecipe,
    val iconPedestal: IconPedestalRecipe,
    val ambient: AmbientRecipe,
    val energy: EnergyRecipe,
    val layout: CardLayoutRecipe,
    val accents: List<AccentRole>,
) {
    fun structuralFingerprint(): String = listOf(
        motif, frame.structuralFingerprint(), iconPedestal.structuralFingerprint(),
        ambient.structuralFingerprint(), energy.structuralFingerprint(), layout.structuralFingerprint(),
    ).joinToString("|")
}

enum class DecoratedCardRole { Hero, Monitor, Function, Standard, Compact }

enum class AccentRole { Primary, Secondary, Tertiary, Outline, Inverse, SurfaceGlow }

enum class EnergyDirection { StartToEnd, EndToStart, CenterOut, OutsideIn, BottomToTop, TopToBottom, Clockwise, CounterClockwise }

enum class OrnamentMotif {
    InkPanels, HeartRibbon, FrostScarf, IronWind, CloudSlope, SakuraStreet,
    SakuraCrown, CatCourtyard, MintPull, InkPoster, SilverMoon, CobaltDress,
    SkyRibbon, Windfield, CrimsonFocus, CreamStreet, BlackRose, SeaBreeze,
    PinkMist, FrostCrimson, BlueFlameCat, VisualNovel, SnowWindow, MoonOrbit,
    SakuraFan, MintVine, LavenderCrystal, CyberCircuit, ObsidianFacet,
    MicaLayer, EmberForge, JadeCloud,
}
```

配套数据类固定为：

```kotlin
@Immutable data class FrameRecipe(val style: FrameStyle, val lineCount: Int, val cutCorners: Int, val insetStroke: Boolean, val accent: AccentRole)
@Immutable data class IconPedestalRecipe(val style: PedestalStyle, val ringCount: Int, val accent: AccentRole)
@Immutable data class AmbientRecipe(val style: AmbientStyle, val driftAxis: DriftAxis, val amplitude: Float)
@Immutable data class EnergyRecipe(val path: EnergyPathStyle, val particle: ParticleStyle, val direction: EnergyDirection, val baseParticles: Int, val phaseOffset: Float, val accents: List<AccentRole>)
@Immutable data class CardLayoutRecipe(val titleRail: TitleRailStyle, val badgeAnchor: BadgeAnchor, val safeInsetFraction: Float)
```

为 `FrameStyle`、`PedestalStyle`、`AmbientStyle`、`EnergyPathStyle`、`ParticleStyle`、`DriftAxis`、`TitleRailStyle`、`BadgeAnchor` 定义后续 Task 3 表中用到的全部枚举值，不允许使用自由字符串。每个子配方的 `structuralFingerprint()` 只串联几何/运动字段，明确排除 `accent`/`accents`；`ThemeDecorationSpec.structuralFingerprint()` 明确排除 `themeId` 和顶层 `accents`。

- [ ] **Step 3: 在 CI 中重跑单测**

Expected: `ThemeDecorationSpecTest` 通过。

- [ ] **Step 4: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpec.kt `
        manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationSpecTest.kt
git commit -m 'feat(manager): define theme decoration contracts'
```

---

## Task 3: 注册 32 套显式主题配方和确定性回退

**Files:**

- Create: `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalogTest.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalog.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/MainActivity.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/theme/Theme.kt`

- [ ] **Step 1: 写 32/32 覆盖与非换色伪适配测试**

```kotlin
class ThemeDecorationCatalogTest {
    @Test
    fun `catalog matches every built in theme exactly once`() {
        assertEquals(BuiltInThemes.all.map { it.id }.toSet(), ThemeDecorationCatalog.all.keys)
        assertEquals(32, ThemeDecorationCatalog.all.size)
    }

    @Test
    fun `every built in theme has a distinct structural fingerprint`() {
        assertEquals(32, ThemeDecorationCatalog.all.values.map { it.structuralFingerprint() }.toSet().size)
    }

    @Test
    fun `all artwork themes still own complete token bundles`() {
        val artwork = BuiltInThemes.all.filter { it.tokenBundleId != null }
        assertEquals(21, artwork.size)
        artwork.forEach { assertNotNull(BuiltInThemeCatalog.byId(it.id), it.id) }
    }

    @Test
    fun `custom fallback is deterministic and never aliases a built in id`() {
        val first = ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0xFF7A53C7.toInt(), ColorMode.DARK)
        val second = ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0xFF7A53C7.toInt(), ColorMode.DARK)
        assertEquals(first, second)
        assertFalse(first.themeId in ThemeDecorationCatalog.all)
    }
}
```

Expected first run: unresolved `ThemeDecorationCatalog` failure。

- [ ] **Step 2: 按下表逐条建立目录，不得省略或合并 ID**

表中每一行分别映射 `id / motif / frame / pedestal / ambient / energy path / particle / direction / title rail`。`frame`、`pedestal` 和 `ambient` 的枚举应采用表内 PascalCase 名称。对零基行号 `index in 0..31`，其余字段按下面公式固定生成，避免实现时自由发挥导致配方漂移：

```kotlin
lineCount = 2 + index % 2
cutCorners = index % 3
insetStroke = index % 2 == 0
ringCount = 1 + index % 3
driftAxis = DriftAxis.entries[index % DriftAxis.entries.size]
amplitude = 0.10f + (index % 5) * 0.04f
baseParticles = listOf(10, 12, 8, 10, 8)[index % 5]
phaseOffset = index / 32f
badgeAnchor = BadgeAnchor.entries[index % BadgeAnchor.entries.size]
safeInsetFraction = 0.10f + (index % 4) * 0.02f
accents = listOf(AccentRole.Primary, AccentRole.Secondary, AccentRole.Tertiary)
```

`DriftAxis` 的声明顺序固定为 `None, Horizontal, Vertical, Diagonal, Radial`，`BadgeAnchor` 固定为 `TopStart, TopEnd, CenterStart, CenterEnd, BottomStart, BottomEnd`。`frame.accent` 使用 `accents[0]`，`iconPedestal.accent` 使用 `accents[1]`，`energy.accents` 使用完整列表。

| ID | Motif | Frame | Pedestal | Ambient | Path | Particle | Direction | Title rail |
|---|---|---|---|---|---|---|---|---|
| ink-white-companions | InkPanels | PanelSplit | InkStamp | Halftone | BrushSweep | InkDot | StartToEnd | CaptionStrip |
| twin-peach-heartstrings | HeartRibbon | DoubleRibbon | HeartGem | SugarPearl | TwinConverge | HeartSpark | OutsideIn | RibbonTab |
| winter-blue-scarf | FrostScarf | TartanFrost | IceGem | FineSnow | CrystalGrow | SnowFlake | CenterOut | WovenLabel |
| dusk-iron-wind | IronWind | RivetRail | BoltPlate | WindSparks | RailCharge | EmberSpark | StartToEnd | MetalPlate |
| cloud-slope-stars | CloudSlope | HorizonArc | StarMedallion | CloudBand | RisingArc | StarShard | BottomToTop | HorizonTag |
| sakura-street-walk | SakuraStreet | WoodSakura | PetalSeal | SunlitPetals | PetalUpdraft | SakuraPetal | BottomToTop | WoodPlaque |
| sakura-crown-overture | SakuraCrown | CrownFiligree | CrownJewel | RoyalPetals | CrownOrbit | JewelPetal | Clockwise | CrownRibbon |
| golden-eye-cat-courtyard | CatCourtyard | FelineCut | PawSeal | LeafWindow | PawSteps | CatGlint | StartToEnd | WhiskerTab |
| mint-pull | MintPull | PullCord | KnotGem | MintLeaves | ElasticConverge | MintLeaf | OutsideIn | BowLabel |
| ink-order-poster | InkPoster | PosterCrop | TypeBlock | TornPaper | SlashCut | GlyphShard | StartToEnd | PosterBar |
| moonlit-silver-blue | SilverMoon | LunarFiligree | MoonGem | StarDust | LunarOrbit | SilverStar | Clockwise | MoonRibbon |
| cobalt-night-dress | CobaltDress | NightLace | Rosette | MoonLeaves | BeamDrop | BlueLeaf | TopToBottom | LaceLabel |
| clear-sky-blue-ribbon | SkyRibbon | SkyKnot | RibbonGem | CloudCurl | RibbonFlip | SkySpark | StartToEnd | SkyBanner |
| windfield-doll | Windfield | WindStitch | WindRose | GrassWave | FieldSweep | GrassLeaf | StartToEnd | FieldTag |
| crimson-eye-jump | CrimsonFocus | FocusBracket | ReticleGem | SpeedLines | FocusBurst | CrimsonShard | OutsideIn | CameraStrip |
| cream-street-corner | CreamStreet | StreetStamp | SunSeal | WarmGrid | LampMarch | CreamSpark | StartToEnd | StreetLabel |
| black-rose-stone-court | BlackRose | ThornMasonry | RoseGem | StoneCracks | ThornGrow | RosePetal | StartToEnd | GothicPlaque |
| sea-breeze-song | SeaBreeze | TideLine | ShellGem | WaterGlint | TideFill | Bubble | StartToEnd | SailorRibbon |
| pink-mist-night-window | PinkMist | WindowMist | MistPearl | NightWindow | MistGather | PinkDroplet | OutsideIn | WindowLabel |
| frost-white-crimson-eye | FrostCrimson | FrostGem | CrimsonGem | Rime | CrystalConverge | FrostShard | OutsideIn | WhiteSeal |
| blue-flame-cat-shadow | BlueFlameCat | CatFlame | CatSigil | FlameHalo | FlameSpiral | BlueFlame | CounterClockwise | NeonPaw |
| SakuraVN | VisualNovel | DialogFrame | ChoiceCursor | SakuraOverlay | ChoiceConfirm | PetalCursor | StartToEnd | NamePlate |
| Snow | SnowWindow | SnowFacet | SnowCrystal | PowderSnow | FrostBloom | HexSnow | CenterOut | FrostTab |
| Moonlit | MoonOrbit | OrbitRing | OrbitCore | Constellation | PlanetOrbit | Meteor | Clockwise | OrbitLabel |
| Sakura | SakuraFan | PetalGold | FanSeal | SoftPetals | PetalFountain | GoldPetal | BottomToTop | FanRibbon |
| Mint | MintVine | BotanicalGlass | Dewdrop | VineVein | VineGrow | GlowLeaf | StartToEnd | LeafLabel |
| Lavender | LavenderCrystal | CrystalConstellation | Prism | LavenderStars | PrismConnect | CrystalShard | CenterOut | MirrorTab |
| Cyber | CyberCircuit | CircuitScan | Chip | ScanGrid | DataPulse | DataPacket | StartToEnd | HudRail |
| Obsidian | ObsidianFacet | ObsidianCrack | ObsidianShard | DarkRefraction | CoreFracture | SpectrumShard | CenterOut | FacetLabel |
| Mica | MicaLayer | MicaSheet | MicaPearl | PearlDust | LayerSweep | IridescentFlake | StartToEnd | FilmTab |
| Ember | EmberForge | ForgeEdge | EmberCore | FurnaceAsh | FuseBurn | FireSpark | StartToEnd | ForgePlate |
| Jade | JadeCloud | JadeSeal | JadeMedallion | AuspiciousCloud | QiFlow | JadeSpark | Clockwise | ScrollLabel |

目录实现必须公开：

```kotlin
object ThemeDecorationCatalog {
    val all: Map<String, ThemeDecorationSpec>
    fun resolve(themeId: String?, keyColor: Int, colorMode: ColorMode): ThemeDecorationSpec
}
```

`resolve` 顺序固定为：显式 32 项 → `themeId == None && keyColor != 0` 时以 `(keyColor, colorMode.value)` 生成确定性自定义配方 → `None` 的 XinovaSU 默认纹章 → 未知 ID 的默认纹章。不得调用随机数。

- [ ] **Step 3: 在应用根 Provider 一次解析并注入**

在 `Theme.kt` 新增：

```kotlin
val LocalThemeDecorationSpec = staticCompositionLocalOf {
    ThemeDecorationCatalog.defaultSpec
}
```

在 `MainActivity.kt` 与现有 `themeTokenBundle` 同级 `remember`：

```kotlin
val themeDecorationSpec = remember(appSettings.themePresetId, appSettings.keyColor, appSettings.colorMode) {
    ThemeDecorationCatalog.resolve(
        themeId = appSettings.themePresetId,
        keyColor = appSettings.keyColor,
        colorMode = appSettings.colorMode,
    )
}
```

并在现有根 `CompositionLocalProvider` 中加入 `LocalThemeDecorationSpec provides themeDecorationSpec`。

- [ ] **Step 4: CI 重跑目录测试**

Expected: 32 个 ID 完全相等、32 个结构指纹均唯一、21 个 artwork token 仍完整、回退测试通过。

- [ ] **Step 5: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/MainActivity.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/theme/Theme.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalog.kt `
        manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationCatalogTest.kt
git commit -m 'feat(manager): register dedicated decoration for every theme'
```

---

## Task 4: 用纯状态规划器锁定注能时序和降级

**Files:**

- Create: `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducerTest.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducer.kt`
- Create: `manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicyTest.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicy.kt`

- [ ] **Step 1: 写失败测试覆盖错误路径**

```kotlin
class EnergyTransitionReducerTest {
    @Test
    fun `first checked composition syncs without burst`() {
        val plan = EnergyTransitionReducer.next(null, true, 1f, enabled = true, reduceMotion = false)
        assertEquals(1f, plan.target)
        assertEquals(0, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
        assertTrue(plan.initializeOnly)
    }

    @Test
    fun `activation uses 900 ms and card resonance`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = true, reduceMotion = false)
        assertEquals(0f, plan.from)
        assertEquals(1f, plan.target)
        assertEquals(900, plan.durationMillis)
        assertTrue(plan.emitParticles)
        assertTrue(plan.resonateCard)
    }

    @Test
    fun `deactivation uses 360 ms without activation burst`() {
        val plan = EnergyTransitionReducer.next(true, false, 1f, enabled = true, reduceMotion = false)
        assertEquals(0f, plan.target)
        assertEquals(360, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
    }

    @Test
    fun `rapid reversal starts from current progress`() {
        val plan = EnergyTransitionReducer.next(true, false, 0.62f, enabled = true, reduceMotion = false)
        assertEquals(0.62f, plan.from)
        assertEquals(0f, plan.target)
    }

    @Test
    fun `disabled changes synchronize without emission`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = false, reduceMotion = false)
        assertEquals(1f, plan.target)
        assertEquals(0, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
    }

    @Test
    fun `reduced motion uses 120 ms static highlight`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = true, reduceMotion = true)
        assertEquals(120, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
    }
}
```

`DecorationDensityPolicyTest` 固定以下预算：

- 正常：Hero 14、Monitor 8、Function 8、Standard 6、Compact 4。
- 低内存：粒子减半、环境漂移关闭。
- 省电：粒子最大 4、环境漂移关闭。
- 减少动画：粒子 0、环境漂移关闭、只保留 120ms 高亮。
- 任意组合粒子数不得小于 0 或超过角色基准。

Expected first CI run: 缺少 reducer/policy 类型而失败。

- [ ] **Step 2: 实现显式规划结果**

```kotlin
data class EnergyTransitionPlan(
    val from: Float,
    val target: Float,
    val durationMillis: Int,
    val emitParticles: Boolean,
    val resonateCard: Boolean,
    val initializeOnly: Boolean,
)

object EnergyTransitionReducer {
    fun next(
        previousChecked: Boolean?,
        checked: Boolean,
        currentProgress: Float,
        enabled: Boolean,
        reduceMotion: Boolean,
    ): EnergyTransitionPlan
}
```

规则必须直接编码：`previousChecked == null` 或 `!enabled` 时零时长同步；开启 900ms；关闭 360ms；减少动画 120ms 且无粒子；`from` 永远使用传入 `currentProgress.coerceIn(0f, 1f)`。

`DecorationDensityPolicy.forRole(role, lowRam, powerSave, reduceMotion)` 为纯函数，不读取 Android 服务。Android 环境只在 Compose 层转成布尔输入。

渲染时粒子数固定取 `minOf(spec.energy.baseParticles, policy.maxParticles)`；环境漂移只有 `policy.ambientMotion == true` 时启用。

- [ ] **Step 3: CI 重跑两组测试并提交**

Expected: reducer 和 density policy 全绿。

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducer.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicy.kt `
        manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/EnergyTransitionReducerTest.kt `
        manager/app/src/test/java/com/xinsu/moe/ui/theme/decoration/DecorationDensityPolicyTest.kt
git commit -m 'feat(manager): define reversible energy motion policy'
```

---

## Task 5: 实现公共卡片装饰宿主并保留两种玻璃材质

**Files:**

- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/DecorationColors.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergyPulseHost.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/ThemedCardDecoration.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/ArtworkOrnaments.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/PaletteOrnaments.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergyOrnaments.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/component/material/TonalCard.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/MiuixGlassCard.kt`

- [ ] **Step 1: 建立统一色槽与最近卡片宿主**

```kotlin
@Immutable
data class DecorationColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val outline: Color,
    val inverse: Color,
    val surfaceGlow: Color,
)

@Composable fun materialDecorationColors(): DecorationColors
@Composable fun miuixDecorationColors(): DecorationColors

@Stable
class EnergyPulseHostState internal constructor() {
    var owner: Long by mutableLongStateOf(Long.MIN_VALUE)
        private set
    var progress: Float by mutableFloatStateOf(0f)
        private set

    fun publish(owner: Long, progress: Float)
    fun release(owner: Long)
}

val LocalEnergyPulseHost = compositionLocalOf<EnergyPulseHostState?> { null }
```

两个配色函数分别只从 `MaterialTheme.colorScheme` 与 `MiuixTheme.colorScheme` 取值，不读取 `KawaiiPalette` 或硬编码某个主题色。`publish` 只有当前或更新 owner 才接受；owner 由进程内单调递增的 `AtomicLong` 分配，`release` 仅清除同一 owner，防止同一卡片快速切换时旧协程覆盖新脉冲。

- [ ] **Step 2: 实现不会改变测量/点击语义的装饰内容层**

```kotlin
@Composable
fun DecoratedCardContent(
    role: DecoratedCardRole,
    colors: DecorationColors,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)
```

实现要求：内部单个 `Column` 使用 `drawWithCache`；顺序为 ambient/frame 背景 → `drawContent()` → foreground/energy；不加 `clickable`、`pointerInput` 或语义 modifier；默认填满可用宽度但高度由原内容测量；文字安全区由 `CardLayoutRecipe.safeInsetFraction` 限制角饰绘制范围，不靠额外 padding 改变现有页面布局。

- [ ] **Step 3: 实现 21+11 个穷尽绘制分派**

`ArtworkOrnaments.kt` 对前 21 个 `OrnamentMotif` 使用无 `else` 的穷尽 `when`，每项调用独立 helper：`drawInkPanels` 到 `drawBlueFlameCat`。`PaletteOrnaments.kt` 对后 11 项调用 `drawVisualNovel` 到 `drawJadeCloud`。每个 helper 至少组合三种结构元素：边框/轨道、角饰、环境纹理；不能仅改变颜色。

公共低阶 primitive 限定为线、弧、圆、菱形、叶片、花瓣、丝带、网点、晶体、火花和波纹；高阶 helper 决定独特组合。`EnergyOrnaments.kt` 同样对 32 个 motif 穷尽分派，根据 Task 3 表绘制轨迹和粒子，粒子位置由 `themeId + particleIndex` 的确定性散列计算，不使用 `Random`。

- [ ] **Step 4: 接入 Material 卡片但不破坏透明度**

给 `TonalCard` 两个 overload 都新增末尾默认参数：

```kotlin
role: DecoratedCardRole = DecoratedCardRole.Standard,
active: Boolean = false,
```

保留现有 `LocalGlassCard.current` 时 `baseContainerColor.copy(alpha = 0.55f)` 的逻辑和原 `Card` 点击参数；仅把原 `content()` 放入 `DecoratedCardContent`。Material 装饰色从 `MaterialTheme.colorScheme` 映射，不硬编码主题颜色。

- [ ] **Step 5: 接入 Miuix 卡片且逐字保留真模糊路径**

给 `MiuixGlassCard` 两个 overload 增加同样的 `role`/`active` 默认参数。保留现有 `glassCardStyle()`、`LocalCardBackdrop`、`textureBlur`、24f blur radius、`BlendColorEntry` 与 `Color.Transparent` container 路径；只在 `MiuixCard` 的 `content` 内挂载 `DecoratedCardContent`。Miuix 装饰色从 `MiuixTheme.colorScheme` 映射。

- [ ] **Step 6: 运行轻量守卫**

```powershell
python scripts/check_theme_surface_guards.py
git diff --check
git diff origin/main -- kernel/Kbuild userspace .github/workflows/release.yml
```

Expected: surface guard PASS；`diff --check` 无输出；最后一条无输出。

- [ ] **Step 7: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/component/decoration `
        manager/app/src/main/java/com/xinsu/moe/ui/component/material/TonalCard.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/MiuixGlassCard.kt
git commit -m 'feat(manager): render lavish themed card layers'
```

---

## Task 6: 把 Material 开关接入可反转注能动画

**Files:**

- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergySwitchVisual.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/component/material/ExpressiveSwitch.kt`

- [ ] **Step 1: 建立只包裹、不重写平台 Switch 的视觉核心**

```kotlin
@Composable
fun EnergySwitchVisual(
    checked: Boolean,
    enabled: Boolean,
    colors: DecorationColors,
    modifier: Modifier = Modifier,
    switch: @Composable () -> Unit,
)
```

内部 `Animatable` 首值必须为 `if (checked) 1f else 0f`；`previousChecked` 初始为 `null`。每次 `checked` 变化通过 `EnergyTransitionReducer.next` 生成计划，并直接从 `Animatable.value` 反向或正向 `animateTo`。在 animation block 中向最近 `LocalEnergyPulseHost` 发布当前 owner/progress；`finally` 中只释放同 owner。

阶段曲线固定：0～0.12 压缩、0.12～0.30 蓄能、0.30～0.50 灌注、0.50～0.72 卡片共振、0.72～0.90 沉降；总开启 900ms。关闭 360ms。`ValueAnimator.areAnimatorsEnabled()` 为 false 时使用 120ms 静态高亮、0 粒子。`ActivityManager.isLowRamDevice` 与 `PowerManager.isPowerSaveMode` 只输入 `DecorationDensityPolicy`。

- [ ] **Step 2: 保留 Material3 的完整接口和业务回调**

`ExpressiveSwitch` 的现有参数签名不删不改；仅把当前 Material3 `Switch` 作为 `EnergySwitchVisual` 的 `switch` lambda，并把 `materialDecorationColors()` 传给视觉层。仍由原 `Switch` 接收 `onCheckedChange`、`enabled`、`colors`、`interactionSource` 和 thumb icon。

明确禁止：在视觉层再次调用 `onCheckedChange`；给装饰 Box 增加点击；首次 `checked=true` 时播放完整爆发。

- [ ] **Step 3: 静态验证唯一原生入口**

```powershell
git grep -n -E '\bSwitch\(' -- manager/app/src/main/java/com/xinsu/moe | Select-String -NotMatch 'ExpressiveSwitch.kt|EnergyMiuixSwitch.kt'
```

Expected at this stage: 只剩 `ModuleMiuix.kt` 一处，留给 Task 7。

- [ ] **Step 4: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/component/decoration/EnergySwitchVisual.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/component/material/ExpressiveSwitch.kt
git commit -m 'feat(manager): inject themed energy into material switches'
```

---

## Task 7: 本地复刻 Miuix 0.9.1 偏好行并迁移全部 24 个入口

**Files:**

- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitch.kt`
- Create: `manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitchPreference.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/component/profile/AppProfileConfigMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/appprofile/AppProfileMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/colorpalette/ColorPaletteScreenMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/settings/SettingsMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMiuix.kt`

- [ ] **Step 1: 创建 Miuix 原生 Switch 视觉包装**

```kotlin
@Composable
fun EnergyMiuixSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.switchColors(),
    enabled: Boolean = true,
) {
    EnergySwitchVisual(
        checked = checked,
        enabled = enabled,
        colors = miuixDecorationColors(),
        modifier = modifier,
    ) {
        top.yukonga.miuix.kmp.basic.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = colors,
            enabled = enabled,
        )
    }
}
```

这保留 Miuix 的 49x28dp 轨道、拖拽、hover、ToggleOn/ToggleOff 触觉和 `Role.Switch` 语义。

- [ ] **Step 2: 依据上游 0.9.1 的真实 API 实现偏好行**

`EnergyMiuixSwitchPreference` 参数必须完整保留如下：

```kotlin
@Composable
fun EnergyMiuixSwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)
```

内部继续使用 Miuix `BasicComponent`：endActions 先放原自定义 action，再放 `EnergyMiuixSwitch`；整行 `onClick` 仅在 enabled 时调用一次 `onCheckedChange(!checked)`；`role = Role.Switch`。布局保持上游 0.9.1 的 endActions `padding(end = 8.dp)`、`Alignment.CenterVertically` 和 `weight(1f, fill = false)`。

- [ ] **Step 3: 机械迁移 23 个 `SwitchPreference` 和一个 raw `Switch`**

逐文件替换 import 和调用名，参数不改：

- `AppProfileConfigMiuix.kt`: 1
- `AppProfileMiuix.kt`: 1
- `ColorPaletteScreenMiuix.kt`: 6
- `FunctionsMiuix.kt`: 6
- `SettingsMiuix.kt`: 9
- `ModuleMiuix.kt`: raw `Switch` 1

迁移后运行：

```powershell
git grep -n 'top.yukonga.miuix.kmp.preference.SwitchPreference' -- manager/app/src/main/java/com/xinsu/moe
git grep -n -E '\bSwitchPreference\(' -- manager/app/src/main/java/com/xinsu/moe
git grep -n -E '\bSwitch\(' -- manager/app/src/main/java/com/xinsu/moe | Select-String -NotMatch 'ExpressiveSwitch.kt|EnergyMiuixSwitch.kt'
```

Expected: 三条命令均无输出。若出现一处，不能提交。

- [ ] **Step 4: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitch.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/component/miuix/EnergyMiuixSwitchPreference.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/component/profile/AppProfileConfigMiuix.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/appprofile/AppProfileMiuix.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/colorpalette/ColorPaletteScreenMiuix.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMiuix.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/settings/SettingsMiuix.kt `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMiuix.kt
git commit -m 'feat(manager): migrate miuix switches to themed energy'
```

---

## Task 8: 为核心页面分配五种卡片角色与静态激活态

**Files:**

- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMaterial.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMaterial.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/functions/FunctionsMiuix.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMaterial.kt`
- Modify: `manager/app/src/main/java/com/xinsu/moe/ui/screen/module/ModuleMiuix.kt`

- [ ] **Step 1: 主页按语义分配角色**

Material 与 Miuix 必须一致：

- Root/Working/Not installed 主状态卡：`Hero`。
- Hardware monitor：`Monitor`。
- Superuser/Module 数量小卡：`Compact`。
- Learn XinovaSU、Learn KernelSU、Donate：`Compact`。
- 系统 Info：`Standard`。

卡片调用只传 `role`；不在页面选择 motif、粒子或颜色。所有未显式迁移的公共卡片仍由 wrapper 默认获得 `Standard` 装饰，不会出现裸卡。

- [ ] **Step 2: 功能卡和模块卡分配角色**

`FunctionsMaterial`/`FunctionsMiuix` 六张主功能卡统一 `Function`，并把该功能真实 `checked` 传给 `active`，使开启后只保留静态微光。`ModuleMaterial`/`ModuleMiuix` 的模块条目使用 `Compact`，不添加常驻粒子。

- [ ] **Step 3: 检查双渲染器角色对称性**

```powershell
git grep -n 'DecoratedCardRole\.' -- manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMaterial.kt manager/app/src/main/java/com/xinsu/moe/ui/screen/home/HomeMiuix.kt
git grep -n 'DecoratedCardRole.Function' -- manager/app/src/main/java/com/xinsu/moe/ui/screen/functions
git grep -n 'DecoratedCardRole.Compact' -- manager/app/src/main/java/com/xinsu/moe/ui/screen/module
```

Expected: 两套 home 都含 Hero/Monitor/Compact/Standard；两套 functions 均有 Function；两套 module 均有 Compact。

- [ ] **Step 4: 提交**

```powershell
git add manager/app/src/main/java/com/xinsu/moe/ui/screen/home `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/functions `
        manager/app/src/main/java/com/xinsu/moe/ui/screen/module
git commit -m 'feat(manager): assign semantic decoration roles to cards'
```

---

## Task 9: 增加 32 主题预览矩阵、源码守卫和轻量测试 CI

**Files:**

- Create: `manager/app/src/debug/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationPreviewMatrix.kt`
- Create: `scripts/check_theme_decoration_guards.py`
- Create: `.github/workflows/theme-contracts.yml`

- [ ] **Step 1: 创建可枚举 32 项的 PreviewParameterProvider**

```kotlin
private class ThemeDecorationPreviewProvider : PreviewParameterProvider<ThemeDecorationSpec> {
    override val values: Sequence<ThemeDecorationSpec> = ThemeDecorationCatalog.all.values.asSequence()
}
```

建立两个预览入口 `MaterialThemeDecorationPreview` 与 `MiuixThemeDecorationPreview`，各使用 `@PreviewParameter(ThemeDecorationPreviewProvider::class, limit = 32)`。每个预览同时显示 Hero、Monitor、Function、Standard、Compact，并放置一个 off/一个 on 的对应开关。预览只读，不写设置、不加载外部图片。

- [ ] **Step 2: 写会失败的 Python 守卫**

`scripts/check_theme_decoration_guards.py` 必须检查：

1. `BuiltInThemes.all` 的 ID 集与 `ThemeDecorationCatalog` 注册 ID 都是 32 且完全相等。
2. 业务页面不得 import/call Miuix `SwitchPreference`。
3. raw Material3 `Switch` 只允许在 `ExpressiveSwitch.kt`；raw Miuix `Switch` 只允许在 `EnergyMiuixSwitch.kt`。
4. `TonalCard.kt` 与 `MiuixGlassCard.kt` 都必须调用 `DecoratedCardContent`。
5. `ArtworkOrnaments.kt` 包含 21 个显式 motif 分支，`PaletteOrnaments.kt` 包含 11 个；禁止 `else ->` 静默回退。
6. `kernel/Kbuild` 的两个签名锚点各精确出现一次。

先在仅添加脚本但未完成全部匹配时运行一次，确认它会退出 1；修正检测对象后运行：

```powershell
python scripts/check_theme_catalog.py
python scripts/check_theme_surface_guards.py
python scripts/check_theme_decoration_guards.py
```

Expected: 三项均输出 `PASS` 并退出 0。

- [ ] **Step 3: 新增不上传 artifact、不触发 native 矩阵的轻量 CI**

不要修改 `build-manager.yml`：它的 native detector 会把该文件自身的变更判为 native-affecting并重跑 7-KMI。新增 `.github/workflows/theme-contracts.yml`，内容固定为：

```yaml
name: Theme Contracts

on:
  pull_request:
    branches: [main, dev]
    paths:
      - 'manager/**'
      - 'scripts/check_theme_*.py'
      - '.github/workflows/theme-contracts.yml'

jobs:
  theme-contracts:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 21
      - uses: gradle/actions/setup-gradle@v6
      - uses: android-actions/setup-android@v4
      - name: Verify source contracts
        run: |
          python3 scripts/check_theme_catalog.py
          python3 scripts/check_theme_surface_guards.py
          python3 scripts/check_theme_decoration_guards.py
      - name: Run manager unit tests
        working-directory: manager
        run: ./gradlew :app:testDebugUnitTest
```

该 workflow 不上传 artifact，不运行 `assembleRelease`，也不在现有 `build-manager.yml` 的 native-affecting正则名单中。原 Build Manager 仍由 `manager/**` 触发并只执行 manager build；`native=false`。

- [ ] **Step 4: 运行本地轻量验证并提交**

```powershell
python scripts/check_theme_catalog.py
python scripts/check_theme_surface_guards.py
python scripts/check_theme_decoration_guards.py
git diff --check
git diff origin/main -- kernel/Kbuild userspace .github/workflows/release.yml
```

Expected: 三项 PASS；其余无输出。

```powershell
git add manager/app/src/debug/java/com/xinsu/moe/ui/theme/decoration/ThemeDecorationPreviewMatrix.kt `
        scripts/check_theme_decoration_guards.py `
        .github/workflows/theme-contracts.yml
git commit -m 'ci(manager): guard themed cards and energy switches'
```

---

## Task 10: PR、CI 与视觉验收

**Files:**

- Verify only: all changed files

- [ ] **Step 1: 最终本地只读审计**

```powershell
git status --short
git diff --check origin/main...HEAD
python scripts/check_theme_catalog.py
python scripts/check_theme_surface_guards.py
python scripts/check_theme_decoration_guards.py
git diff --name-only origin/main...HEAD
git diff origin/main...HEAD -- kernel/Kbuild userspace .github/workflows/release.yml
```

Expected: 工作树干净；三项 PASS；变更列表仅落在 Global Constraints 允许范围；受保护路径 diff 无输出。

- [ ] **Step 2: 推送分支并创建 Draft PR**

```powershell
git push -u origin codex/lavish-theme-card-energy
gh pr create --repo kusesad-1122/XinovaSU-dev --base main --head codex/lavish-theme-card-energy --draft `
  --title 'feat(manager): lavish theme cards and energy switches' `
  --body 'Implements dedicated card ornament recipes for all 32 built-in themes, shared Material/Miuix card decoration, and reversible theme-specific energy motion for every binary switch. No kernel, signing-anchor, userspace, or release workflow changes.'
```

- [ ] **Step 3: 等待 Build Manager CI 的可失败检查**

必须确认：

- `Verify theme contracts` 三个脚本全绿。
- Theme Contracts 的 `testDebugUnitTest` 包含新增测试类且全绿。
- Build Manager 的 `assembleRelease` 成功。
- Build Manager 的 native detection 为 false，未运行 7-KMI、ksud 或 xnsuinit，也未新增大型 artifact。

任何 Kotlin compile、unit test 或 guard 失败，读取真实日志后回到对应 Task 修复；不得通过删测试、放宽为 `else` 或恢复 raw Switch 绕过。

- [ ] **Step 4: 生成 32 主题双渲染器视觉矩阵**

在 Android Studio Layout Inspector/Compose Preview 中导出 Material 32 项与 Miuix 32 项。逐项核对：motif 与 Task 3 表一致、五角色文字安全、玻璃背景可见、on/off switch 不遮挡。此检查为 `[UNVERIFIED]`，直到实际预览矩阵被人工看过。

- [ ] **Step 5: 代表性真机检查**

至少选择低亮、浅色、高饱和、复杂立绘和五种绘制家族各一项：`ink-white-companions`、`sakura-street-walk`、`moonlit-silver-blue`、`Cyber`、`Obsidian`、`Jade`。每项在 Material/Miuix 检查：

- 首次打开且开关已开，不爆发。
- 单击开启只更新一次业务状态，900ms 注能且最近卡片共振。
- 300ms 内连续开关能从当前进度反转，无叠加残影。
- Miuix 拖动和触觉仍有效；Material thumb icon 仍正确。
- 系统关闭动画、省电、低内存策略按契约降级。
- TalkBack 只读到一个 Switch，不读出装饰图形。
- Miuix 真模糊和 Material 半透明都保留；滑动长列表没有明显持续掉帧。

以上在真机完成前均为 `[UNVERIFIED]`，不能仅凭 CI 宣称“99%适配”已经达成。

- [ ] **Step 6: 通过后转 Ready，但不合并、不发版**

只有 CI 全绿、32 主题矩阵人工通过、代表性真机检查完成后，才运行：

```powershell
gh pr ready --repo kusesad-1122/XinovaSU-dev
```

本计划终点是 ready PR；合并 main、删旧 release/tag、打版本 tag 或触发发布均不在本次授权范围。

---

## Completion Checklist

- [ ] 32/32 内置主题显式配方且结构指纹唯一。
- [ ] 21 个立绘 token 目录完整，无重复造 token。
- [ ] 五种卡片角色在 Material/Miuix 语义对称。
- [ ] 所有公共卡片默认有 Standard 装饰，无裸卡。
- [ ] 23 个 Miuix `SwitchPreference` 和 1 个 raw Miuix `Switch` 全部迁移。
- [ ] Material3/Miuix 平台 Switch 仍承担点击、拖动、触觉和语义。
- [ ] 首次组合、开、关、快反、disabled、reduce motion 测试全绿。
- [ ] Miuix 真模糊与 Material 0.55 半透明路径保持。
- [ ] Python 守卫与 `testDebugUnitTest` 接入无 artifact 的 Theme Contracts CI。
- [ ] `kernel/Kbuild`、`userspace/`、`release.yml` 无 diff。
- [ ] 32 主题双渲染器预览矩阵与代表性真机验收关闭 `[UNVERIFIED]`。
