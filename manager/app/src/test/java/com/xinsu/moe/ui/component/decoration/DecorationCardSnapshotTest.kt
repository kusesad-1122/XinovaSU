package com.xinsu.moe.ui.component.decoration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.component.material.TonalCard
import com.xinsu.moe.ui.theme.AppSettings
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.MaterialXinovaSUTheme
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationCatalog
import com.xinsu.moe.ui.theme.neonAccent
import org.junit.Rule
import org.junit.Test

/**
 * 无设备截图：用 LayoutLib 在 JVM 上把装饰卡渲染成 PNG。
 *
 * 为什么需要它：CI 只能证明「编译通过」，不能证明「界面没坏」。本项目此前就遇到过
 * miuix 0.9.3 把 TextField 三个颜色参数合并成 `colors` 这类改动 —— 编译报错还算幸运，
 * 更危险的是编译通过但视觉悄悄变了的改动（例如主题装饰的 frame / pedestal 被换掉、
 * 形变过渡时长写错导致动画不播）。
 *
 * 主题选择（第一版选过 Sakura / Cyber / Obsidian，实测是个错误选择）：
 * KawaiiPalette 里有两族 —— 注释见 KawaiiPalette.kt:17-20：
 *   - pastel "kawaii" 族（Sakura / Mint / Lavender / Cyber）**共用同一个深蓝灰画布**
 *     KawaiiDark(0xFF1A1A2E)，只有 accents 不同；
 *   - modern 族（Obsidian / Mica / Ember / Jade）各自带独立画布 + 独立 accents。
 * 所以拿 Sakura 和 Cyber 截图会得到几乎相同的背景（像素级实测两者均为 rgb(26,26,46)），
 * 无法用于视觉回归。现改用 modern 族三套，画布与强调色都不同。
 *
 * 已知限制（Paparazzi/LayoutLib 的固有限制，非本测试缺陷）：
 *   - RuntimeShader / AGSL 背景不渲染（isRuntimeShaderSupported() 返 false 后优雅降级），
 *     所以截图里没有动态流光背景。装饰卡的 frame / pedestal / particle 走纯 Canvas 绘制，
 *     这部分能正常出图 —— 而那正是二次元视觉的主体。
 *   - 动画被冻结在第一帧，EnergyTimeline 的过程态无法直接观察（需用 gif 模式）。
 */
class DecorationCardSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false,
    )

    private fun renderTheme(themeId: String, preset: KawaiiPalette) {
        val spec = ThemeDecorationCatalog.resolve(
            themeId = themeId,
            keyColor = 0,
            colorMode = ColorMode.DARK,
        )

        paparazzi.snapshot {
            MaterialXinovaSUTheme(
                appSettings = AppSettings(
                    colorMode = ColorMode.DARK,
                    // keyColor != 0 是必须的：keyColor == 0 会让 MaterialTheme 走
                    // dynamicColor 分支（系统取色），在 Paparazzi 环境下拿不到壁纸色，
                    // 三套主题会全部渲染成同一个默认配色。
                    keyColor = preset.neonAccent(isDark = true).toArgb(),
                    paletteStyle = PaletteStyle.TonalSpot,
                    colorSpec = ColorSpec.SpecVersion.Default,
                    themePresetId = preset.name,
                )
            ) {
                CompositionLocalProvider(LocalThemeDecorationSpec provides spec) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Column {
                            // 用真实的 TonalCard 而不是裸 DecoratedCardContent：
                            // 后者只画装饰层，背景/圆角/内边距都由外层 Card 提供
                            // （见 TonalCard.kt:38-58）。直接渲染裸装饰层会得到
                            // 「只有细线、没有底色」的图，与实际界面不符 —— 第一版就踩了这个。
                            TonalCard(
                                onClick = {},
                                role = DecoratedCardRole.Standard,
                            ) {
                                Text(
                                    "Standard · $themeId",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "frame=${spec.frame.style} · pedestal=${spec.iconPedestal.style}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // DecoratedCardContent 自身不带外边距，两卡之间必须显式留白，
                            // 否则 Hero 的标题会紧贴 Standard 的副标题（第一版截图已踩到）。
                            Spacer(Modifier.height(20.dp))

                            // Hero：frameScale 最大且开启二级装饰，最能暴露几何/密度策略回归
                            TonalCard(
                                onClick = {},
                                role = DecoratedCardRole.Hero,
                                active = true,
                            ) {
                                Text(
                                    "Hero · ${spec.motif}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "ambient=${spec.ambient.style} · path=${spec.energy.path} · particle=${spec.energy.particle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun ember() = renderTheme("Ember", KawaiiPalette.Ember)

    @Test
    fun jade() = renderTheme("Jade", KawaiiPalette.Jade)

    @Test
    fun obsidian() = renderTheme("Obsidian", KawaiiPalette.Obsidian)
}
