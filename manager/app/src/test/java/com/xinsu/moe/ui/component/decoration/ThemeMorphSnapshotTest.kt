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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
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
import com.xinsu.moe.ui.theme.decoration.ThemeMorphFrame
import com.xinsu.moe.ui.theme.decoration.ThemeMorphTimeline
import com.xinsu.moe.ui.theme.neonAccent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 主题形变过渡（theme morph）的逐帧渲染。
 *
 * [ThemeMorphTimeline] 是纯函数，参数化后可以直接按帧求值 —— 这让
 * 「760ms 的过渡中间长什么样」第一次变得可观测，而不是只能靠真机播放。
 *
 * 覆盖的帧位刻意选在关键节点上：
 *  - 0.00  起点（纯旧配方）
 *  - 0.20  淡出期（造型仍是旧的，但尺寸类参数已在插值）
 *  - 0.38  刚过 StyleSwapShare(0.34)，造型应已切到新配方
 *  - 0.50  中点（光扫最亮处）
 *  - 0.75  余韵（光扫已扫出右侧，亮度应衰减）
 *  - 1.00  终点（纯新配方）
 *
 * 另外单独出一张「光扫强度曲线」图：把 sweepAlpha 随进度的变化画成条形，
 * 用于肉眼确认光扫是平滑升起再衰减，而不是中途跳变。
 * （早先开发时正是靠这个发现光扫在窗口末会凭空消失。）
 *
 * 局限：Paparazzi 逐帧渲染的是**静态配方**，不是真实播放的动画 ——
 * 它验证的是过渡的每一帧画面对不对，不验证 Animatable 的时间驱动是否流畅。
 * 流畅度仍需真机。
 */
class ThemeMorphSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false,
    )

    private val from = ThemeDecorationCatalog.resolve("Ember", 0, ColorMode.DARK)
    private val to = ThemeDecorationCatalog.resolve("Jade", 0, ColorMode.DARK)

    private fun frameSpec(progress: Float): ThemeMorphFrame =
        ThemeMorphTimeline.frame(from = from, to = to, rawProgress = progress)

    private fun renderFrame(frame: ThemeMorphFrame, label: String) {
        val preset = KawaiiPalette.Ember
        paparazzi.snapshot {
            MaterialXinovaSUTheme(
                appSettings = AppSettings(
                    colorMode = ColorMode.DARK,
                    keyColor = preset.neonAccent(isDark = true).toArgb(),
                    paletteStyle = PaletteStyle.TonalSpot,
                    colorSpec = ColorSpec.SpecVersion.Default,
                    themePresetId = preset.name,
                )
            ) {
                CompositionLocalProvider(
                    // 关键：把形变帧直接喂给绘制层，绕过 Animatable 的时间驱动。
                    LocalThemeDecorationSpec provides frame.spec,
                    LocalThemeMorphFrame provides frame,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        TonalCard(
                            onClick = {},
                            role = DecoratedCardRole.Hero,
                            active = true,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "frame=${frame.spec.frame.style} · " +
                                    "pedestal=${frame.spec.iconPedestal.style} · " +
                                    "eased=${"%.2f".format(frame.eased)} · " +
                                    "sweep=${"%.2f".format(frame.sweep)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun morphKeyFrames() {
        listOf(0.00f, 0.20f, 0.38f, 0.50f, 0.75f, 1.00f).forEach { p ->
            renderFrame(frameSpec(p), "morph p=$p")
        }
    }

    /**
     * 光扫强度曲线：横轴 = 过渡进度，纵轴 = sweepAlpha。
     * 预期是「平滑升起 → 中线附近达峰 → 平滑衰减到 0」的钟形，无跳变。
     * 这张图是给眼睛看的断言 —— 数值断言在
     * `.workbuddy/verify_theme_morph.py` 里（31 项，已全过）。
     */
    @Test
    fun sweepCurve() {
        val steps = 24
        val preset = KawaiiPalette.Obsidian
        paparazzi.snapshot {
            MaterialXinovaSUTheme(
                appSettings = AppSettings(
                    colorMode = ColorMode.DARK,
                    keyColor = preset.neonAccent(isDark = true).toArgb(),
                    paletteStyle = PaletteStyle.TonalSpot,
                    colorSpec = ColorSpec.SpecVersion.Default,
                    themePresetId = preset.name,
                )
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "光扫强度曲线（ThemeMorphTimeline.sweepAlpha）",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(
                            Modifier.height(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            repeat(steps) { i ->
                                val t = i / (steps - 1f)
                                val alpha = ThemeMorphTimeline.sweepAlpha(
                                    ThemeMorphTimeline.sweepAt(t)
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height((220 * alpha).dp.coerceAtLeast(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(
                                    Modifier.width(2.dp)
                                )
                            }
                        }
                        Spacer(
                            Modifier.height(8.dp)
                        )
                        Text(
                            "0.00 → 1.00，共 $steps 根；峰值应在中间（光带扫过卡片中线时最亮）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    /**
     * 造型切换点验证：eased < StyleSwapShare(0.34) 时应是旧 frame，
     * 超过后应是新 frame。这条是断言不是图 —— 它防的是「样式提前/延后切换」。
     */
    @Test
    fun styleSwapHappensAtExpectedProgress() {
        // 换算：ease(t) = 0.34 -> t ≈ 0.4179
        val beforeSwap = ThemeMorphTimeline.frame(from, to, rawProgress = 0.20f)
        val afterSwap = ThemeMorphTimeline.frame(from, to, rawProgress = 0.50f)

        assertEquals(
            "t=0.20 时 eased 应小于 StyleSwapShare，造型仍为旧配方",
            from.frame.style,
            beforeSwap.spec.frame.style,
        )
        assertEquals(
            "t=0.50 时 eased 应超过 StyleSwapShare，造型已切为新配方",
            to.frame.style,
            afterSwap.spec.frame.style,
        )
        assertEquals(
            "t=0 与 t=1 的光扫都应不可见",
            0f,
            ThemeMorphTimeline.sweepAlpha(ThemeMorphTimeline.sweepAt(0f)),
            0.0001f,
        )
        assertEquals(
            "t=1 时位置应已达 SweepEnd，亮度归零",
            0f,
            ThemeMorphTimeline.sweepAlpha(ThemeMorphTimeline.sweepAt(1f)),
            0.0001f,
        )
    }
}
