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
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.theme.AppSettings
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.MaterialXinovaSUTheme
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationCatalog
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
 * 三套主题刻意挑选了差异最大的三种二次元风格（id 取自
 * ThemeDecorationCatalog.AUTHORED_RECIPES，注意大小写）：
 *   - Sakura    樱花（暖粉 / PetalGold 框 / PetalFountain 粒子）
 *   - Cyber     赛博（冷青 / CircuitScan 框 / DataPulse 粒子）
 *   - Obsidian  黑曜（深灰 / ObsidianCrack 框 / CoreFracture 粒子）
 *
 * 已知限制（Paparazzi/LayoutLib 的固有限制，非本测试缺陷）：
 *   - RuntimeShader / AGSL 背景不渲染（isRuntimeShaderSupported() 返 false 后优雅降级），
 *     所以图里没有动态流光背景。装饰卡的 frame / pedestal / particle 走纯 Canvas 绘制，
 *     这部分能正常出图 —— 而那正是二次元视觉的主体。
 *   - 动画冻结在第一帧，EnergyTimeline 的过程态需用 paparazzi.gif 单独观察。
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
                    keyColor = 0,
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
                            DecoratedCardContent(
                                role = DecoratedCardRole.Standard,
                                colors = materialDecorationColors(),
                            ) {
                                Text(
                                    "Standard · $themeId",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "frame=${spec.frame.style} pedestal=${spec.iconPedestal.style}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Hero：frameScale 最大且开启二级装饰，最能暴露几何/密度策略回归
                            DecoratedCardContent(
                                role = DecoratedCardRole.Hero,
                                colors = materialDecorationColors(),
                                active = true,
                            ) {
                                Text(
                                    "Hero · ${spec.motif}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "ambient=${spec.ambient.style} path=${spec.energy.path}",
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
    fun sakura() = renderTheme("Sakura", KawaiiPalette.Sakura)

    @Test
    fun cyber() = renderTheme("Cyber", KawaiiPalette.Cyber)

    @Test
    fun obsidian() = renderTheme("Obsidian", KawaiiPalette.Obsidian)
}
