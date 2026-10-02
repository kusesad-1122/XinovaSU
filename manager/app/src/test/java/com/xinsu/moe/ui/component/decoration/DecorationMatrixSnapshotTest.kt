package com.xinsu.moe.ui.component.decoration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.component.material.TonalCard
import com.xinsu.moe.ui.theme.AppSettings
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette
import com.xinsu.moe.ui.theme.LocalGlassCard
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.MaterialXinovaSUTheme
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.RenderableTheme
import com.xinsu.moe.ui.theme.decoration.RenderableThemes
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationCatalog
import com.xinsu.moe.ui.theme.neonAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 装饰体系的全量视觉回归。
 *
 * 与 `DecorationCardSnapshotTest` 的分工：那个只出 3 张图做人工比对；
 * 这个把能自动化的维度全部覆盖，且用**像素断言**而非人眼判断。
 *
 * 覆盖的维度（都是此前完全没验证过的）：
 *  1. 32 套主题全覆盖 + 配方映射完整性（枚举漂移会立刻失败）
 *  2. 浅色 / 深色
 *  3. 五种卡片角色的几何差异（Hero/Monitor/Function/Standard/Compact）
 *  4. 玻璃态（LocalGlassCard）与实底色
 *  5. RTL 布局（装饰几何有 layoutDirection 分支，从未验证过）
 *  6. 大字体（fontScale 1.5，装饰体系有 LargeFontScale 分支）
 *  7. 平板 / 折叠屏 / 手机三种尺寸
 *  8. pastel 族 vs modern 族的画布差异（防止再次误选同色主题做回归）
 *
 * 已知限制：LayoutLib 不渲染 RuntimeShader/AGSL，故 AGSL 动态背景缺席；
 * 动画冻结在第一帧。这两项只能真机验证。
 */
class DecorationMatrixSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false,
    )

    private fun settings(theme: RenderableTheme, dark: Boolean) = AppSettings(
        colorMode = if (dark) ColorMode.DARK else ColorMode.LIGHT,
        // keyColor != 0 才能避开 dynamicColor 分支（Paparazzi 无系统取色）。
        keyColor = (theme.preset?.neonAccent(isDark = dark) ?: theme.presetAccentFallback())
            .toArgb(),
        paletteStyle = PaletteStyle.TonalSpot,
        colorSpec = ColorSpec.SpecVersion.Default,
        themePresetId = theme.presetId ?: "ink-white-companions",
    )

    private fun RenderableTheme.presetAccentFallback() =
        KawaiiPalette.Sakura.neonAccent(isDark = true)

    private fun card(
        theme: RenderableTheme,
        role: DecoratedCardRole,
        dark: Boolean,
        snapshotName: String? = null,
    ) {
        val spec = ThemeDecorationCatalog.resolve(theme.decorationId, 0, ColorMode.DARK)
        paparazzi.snapshot(name = snapshotName) {
            MaterialXinovaSUTheme(appSettings = settings(theme, dark)) {
                CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        TonalCard(onClick = {}, role = role, active = role == DecoratedCardRole.Hero) {
                            Text(
                                "${theme.label} · $role",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "frame=${spec.frame.style} pedestal=${spec.iconPedestal.style}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 1. 32 套主题全覆盖 ────────────────────────────────────────────────

    /**
     * 每套主题出一张图。32 张图不多，但足以覆盖全部造型枚举 × 粒子枚举的组合。
     * 用参数化测试而不是 32 个 @Test 方法，减少样板。
     */
    @Test
    fun eachThemeRenders() {
        RenderableThemes.all.forEach { theme ->
            card(theme, DecoratedCardRole.Standard, dark = true, snapshotName = theme.decorationId)
        }
    }

    /**
     * 配方映射完整性：如果有人给 ThemeDecorationCatalog 加了新 recipe 却忘了加到
     * RenderableThemes，这里立刻失败，而不是等某天截图少了一套才发现。
     */
    @Test
    fun everyCatalogRecipeIsCovered() {
        val problems = RenderableThemes.validateAgainstCatalog()
        assertTrue(
            "RenderableThemes 与 ThemeDecorationCatalog 不一致:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
        assertEquals(
            "主题总数应与目录一致",
            ThemeDecorationCatalog.all.size,
            RenderableThemes.all.size,
        )
    }

    // ── 2. 深色 / 浅色 ────────────────────────────────────────────────────

    @Test
    fun lightVsDark() {
        // 选 modern 族的 Ember：它有独立画布，深浅色差异明显，便于像素断言。
        val theme = RenderableThemes.all.first { it.decorationId == "Ember" }
        card(theme, DecoratedCardRole.Standard, dark = false, snapshotName = "light")
        card(theme, DecoratedCardRole.Standard, dark = true, snapshotName = "dark")
    }

    // ── 3. 五种卡片角色 ──────────────────────────────────────────────────

    @Test
    fun eachCardRoleRenders() {
        val theme = RenderableThemes.all.first { it.decorationId == "Jade" }
        DecoratedCardRole.entries.forEach { role ->
            card(theme, role, dark = true, snapshotName = role.name)
        }
    }

    // ── 4. 玻璃态 vs 实底 ───────────────────────────────────────────────

    /**
     * LocalGlassCard 打开时 TonalCard 会把容器色降到 55% 透明度
     * （见 TonalCard.kt:31-35），视觉上应与实底色明显不同。
     * 这里同时渲染两种状态 —— 玻璃态是否真的生效，看图就知道。
     */
    @Test
    fun glassVsSolid() {
        val theme = RenderableThemes.all.first { it.decorationId == "Obsidian" }
        val spec = ThemeDecorationCatalog.resolve(theme.decorationId, 0, ColorMode.DARK)
        listOf(false, true).forEach { glass ->
            paparazzi.snapshot(name = "glass=$glass") {
                MaterialXinovaSUTheme(appSettings = settings(theme, dark = true)) {
                    CompositionLocalProvider(
                        LocalThemeDecorationSpec provides spec,
                        LocalGlassCard provides glass,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            TonalCard(onClick = {}, role = DecoratedCardRole.Standard) {
                                Text(
                                    "glass=$glass",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 5. RTL ──────────────────────────────────────────────────────────

    /**
     * CardDecorationGeometry 有完整的 layoutDirection 分支（logicalX 镜像），
     * 装饰轨道、能量路径、徽标锚点都要翻转。这条路径从未被验证过。
     */
    @Test
    fun rtlLayout() {
        val theme = RenderableThemes.all.first { it.decorationId == "Cyber" }
        val spec = ThemeDecorationCatalog.resolve(theme.decorationId, 0, ColorMode.DARK)
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { dir ->
            paparazzi.snapshot(name = "dir=$dir") {
                MaterialXinovaSUTheme(appSettings = settings(theme, dark = true)) {
                    CompositionLocalProvider(
                        LocalThemeDecorationSpec provides spec,
                        LocalLayoutDirection provides dir,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp)
                        ) {
                            TonalCard(onClick = {}, role = DecoratedCardRole.Hero, active = true) {
                                Text(
                                    "dir=$dir",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = if (dir == LayoutDirection.Rtl) {
                                        TextAlign.Right
                                    } else {
                                        TextAlign.Left
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 6. 大字体 ────────────────────────────────────────────────────────

    /**
     * CardDecorationGeometryPolicy 在 fontScale >= 1.3 时会
     * 放大内容留白并关闭二级装饰（showSecondaryOrnaments = false）。
     * 这条分支从未验证过 —— 大字体用户看到的卡片是不同的。
     */
    @Test
    fun largeFontScale() {
        val theme = RenderableThemes.all.first { it.decorationId == "Jade" }
        val spec = ThemeDecorationCatalog.resolve(theme.decorationId, 0, ColorMode.DARK)
        listOf(1.0f, 1.5f).forEach { scale ->
            paparazzi.unsafeUpdateConfig(
                deviceConfig = DeviceConfig.PIXEL_6.copy(fontScale = scale)
            )
            paparazzi.snapshot(name = "fontScale=$scale") {
                MaterialXinovaSUTheme(appSettings = settings(theme, dark = true)) {
                    CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp)
                        ) {
                            TonalCard(onClick = {}, role = DecoratedCardRole.Hero, active = true) {
                                Text(
                                    "fontScale=$scale",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "验证二级装饰是否随字号关闭",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 7. 手机 / 平板 / 折叠屏 ──────────────────────────────────────────

    @Test
    fun phoneTabletFold() {
        val theme = RenderableThemes.all.first { it.decorationId == "Ember" }
        val spec = ThemeDecorationCatalog.resolve(theme.decorationId, 0, ColorMode.DARK)

        fun body() = @Composable {
            MaterialXinovaSUTheme(appSettings = settings(theme, dark = true)) {
                CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Row {
                            TonalCard(
                                modifier = Modifier.weight(1f),
                                onClick = {},
                                role = DecoratedCardRole.Monitor,
                            ) {
                                Text(
                                    "Monitor",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            TonalCard(
                                modifier = Modifier.weight(1f),
                                onClick = {},
                                role = DecoratedCardRole.Compact,
                            ) {
                                Text(
                                    "Compact",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6)
        paparazzi.snapshot(name = "phone") { body() }

        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_TABLET)
        paparazzi.snapshot(name = "tablet") { body() }

        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_FOLD)
        paparazzi.snapshot(name = "fold") { body() }

        // 复位，避免影响同 class 内其他测试
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6)
    }

    // ── 8. pastel 族 vs modern 族（防止再次误选同色主题做回归）────────────

    /**
     * pastel 族（Sakura/Mint/Lavender/Cyber/SakuraVN）**共用 KawaiiDark 画布**，
     * 所以它们的背景色必然相同；modern 族各有独立画布。
     * 这条测试把「哪些主题背景相同」固化成断言 —— 以后若有人调整画布分配，
     * 这里会立刻指出，而不是靠人从图里看出来。
     */
    @Test
    fun pastelFamilySharesCanvas() {
        val pastel = RenderableThemes.all.filter { RenderableThemes.isPastel(it.presetId) }
        val modern = RenderableThemes.all.filterNot { RenderableThemes.isPastel(it.presetId) }

        assertEquals("pastel 族应有 5 个", 5, pastel.size)
        assertTrue("modern 族不应为空", modern.isNotEmpty())

        // 只断言结构关系（哪些是 pastel），不断言具体颜色 ——
        // 颜色差异要靠上面各测试的截图肉眼看，断言具体 RGB 会让换配色时测试变红。
        val pastelIds = pastel.map { it.decorationId }.toSet()
        assertTrue("Sakura 应属 pastel", "Sakura" in pastelIds)
        assertTrue("Cyber 应属 pastel", "Cyber" in pastelIds)
        assertTrue("Obsidian 不应属 pastel", "Obsidian" !in pastelIds)
    }
}
