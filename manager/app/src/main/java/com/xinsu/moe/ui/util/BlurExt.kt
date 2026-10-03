package com.xinsu.moe.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import com.xinsu.moe.ui.component.liquid.LiquidGlassSpec
import com.xinsu.moe.ui.component.liquid.lens
import com.xinsu.moe.ui.component.liquid.vibrancy
import com.xinsu.moe.ui.theme.LocalLiquidGlassSetting
import com.xinsu.moe.ui.theme.LocalTopBarGlassSetting
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
// miuix 0.9.2 起该符号从 kmp.blur 搬到 kmp.shader
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 栏材质宿主：顶栏、非悬浮底栏、横屏导航栏都走这里，所以栏的玻璃材质只需在这一处切换。
 *
 * 材质分档：
 *   • [liquidGlass] = true → 液态玻璃：模糊 + 折射 + 色散 + 边缘高光，参数取自 [LiquidGlassSpec]
 *     （API 33+ 才有 AGSL，低版本自动退回毛玻璃档）
 *   • 否则                → 原来的毛玻璃（25f 模糊 + 87% 表面色），与改动前逐字节一致
 *
 * [liquidGlass] 默认取 [LocalTopBarGlassSetting]：顶栏与下方滚动的卡片会采样同一个 backdrop，
 * 部分布局下会读成一道叠糊接缝，所以顶栏由独立开关控制；底栏 / 导航栏的调用点显式传入
 * [barLiquidGlassEnabled]（跟随"液态玻璃"总开关）。
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    liquidGlass: Boolean = LocalTopBarGlassSetting.current,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f
    val useLiquidGlass = blurActive && backdrop != null && liquidGlass && isRuntimeShaderSupported()

    val barModifier = if (useLiquidGlass) {
        val highlightBase =
            if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
        val scrimAlpha =
            if (isDark) LiquidGlassSpec.DarkScrimAlpha else LiquidGlassSpec.LightScrimAlpha
        Modifier.drawBackdrop(
            backdrop = backdrop!!,
            shape = { RectangleShape },
            effects = {
                vibrancy()
                val radius = LiquidGlassSpec.BlurRadius.toPx()
                blur(radius, radius)
                lens(
                    refractionHeight = LiquidGlassSpec.RefractionHeight.toPx(),
                    refractionAmount = LiquidGlassSpec.RefractionAmount.toPx(),
                    chromaticAberration = LiquidGlassSpec.dispersionOrDefault(),
                )
            },
            highlight = { highlightBase.copy(width = LiquidGlassSpec.HighlightWidth) },
            // 栏比卡片更需要压住背景：底色在卡片基础上再提 20% 不透明度，保证顶栏标题可读。
            onDrawSurface = { drawRect(surfaceColor.copy(alpha = (scrimAlpha + 0.2f).coerceAtMost(1f))) },
        )
    } else if (blurActive && backdrop != null) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 25f,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = surfaceColor.copy(0.87f)),
                ),
            ),
        )
    } else {
        Modifier
    }

    Box(modifier = barModifier) {
        content()
    }
}

/**
 * 底栏 / 横屏导航栏是否使用液态玻璃 —— 跟随"液态玻璃"总开关（[LocalLiquidGlassSetting]）。
 * 顶栏不用这个，顶栏走 [BlurredBar] 的默认值，由独立的顶栏开关控制。
 */
@Composable
fun barLiquidGlassEnabled(): Boolean = LocalLiquidGlassSetting.current
