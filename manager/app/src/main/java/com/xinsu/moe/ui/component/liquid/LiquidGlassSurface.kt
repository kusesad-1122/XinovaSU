// 任意形状的液态玻璃面 —— 圆形按钮、弹出卡片、顶部面板这类"非标准卡片"的玻璃材质入口。
//
// 与 MiuixGlassCard 共用 LiquidGlassSpec 的全部参数与同一个 backdrop，
// 所以圆按钮、弹层卡片、普通卡片在视觉上是同一种材质。
//
// 本文件被两套新组件共用：
//   组件 1（右上角电源触发器 ⇄ 卡片形变）读 [liquidGlassTrigger]
//   组件 2（顶部下拉面板）              读 [liquidGlassSurface]（22dp 圆角 / 22dp 模糊）
package com.xinsu.moe.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xinsu.moe.ui.theme.LocalGlassBlurRadius
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 当前主题是不是深色（按主题表面色亮度判断，与 MiuixGlassCard 同一套口径）。 */
@Composable
internal fun isDarkSurface(): Boolean = MiuixTheme.colorScheme.surface.luminance() < 0.5f

/**
 * 把一个任意 [shapeProvider] 形状的容器变成液态玻璃面。
 *
 * [shapeProvider] 是**惰性求值**的（不是固定 Shape）：形变展开那种"每帧都在变圆角"的场景下，
 * 动画值必须在 draw 阶段读取才能保证每帧刷新 —— 依赖重组去更新 shape 会漏帧甚至停在中间态。
 *
 * 行为与 [com.xinsu.moe.ui.component.miuix.MiuixGlassCard] 完全一致：
 *   • backdrop 可用且 API 33+ → 折射 + 色散 + 模糊 + 底色（完整液态玻璃，RuntimeShader 实现）
 *   • backdrop 可用但 API 31–32 → 模糊 + 底色（库内部自动跳过 AGSL）
 *   • backdrop 不可用（未开玻璃卡片/不支持）→ 半透明实底，保证不出现"透明的黑洞"
 *
 * @param dispersion 色散强度，默认 [LiquidGlassSpec.Dispersion]（0.02）；
 *   触发器用更克制的 [LiquidGlassSpec.TriggerDispersion]（0.015）。
 * @param highlight 边缘高光工厂，`isDark` 由本函数按主题算好传进来。
 *   ⚠️ 只给正圆 / 小圆角面用 —— 实测整幅矩形栏上 miuix 的 BloomStroke 会渲染成纯白块。
 */
@Composable
fun Modifier.liquidGlassSurface(
    backdrop: LayerBackdrop?,
    shapeProvider: () -> Shape,
    refractionHeight: Dp = LiquidGlassSpec.RefractionHeight,
    refractionAmount: Dp = LiquidGlassSpec.RefractionAmount,
    blurRadius: Dp = LocalGlassBlurRadius.current,
    /**
     * 覆盖底色不透明度。默认由明暗主题推导；
     * **嵌在已有一层玻璃上的玻璃**（例如设置页里的滑块）要传更小的值，
     * 否则两层底色叠加会明显变白。
     */
    scrimAlphaOverride: Float? = null,
    dispersion: Float = LiquidGlassSpec.Dispersion,
    highlight: ((isDark: Boolean) -> Highlight?)? = null,
): Modifier {
    val surface = MiuixTheme.colorScheme.surface
    val density = LocalDensity.current
    val isDark = surface.luminance() < 0.5f
    val scrimAlpha = scrimAlphaOverride
        ?: if (isDark) LiquidGlassSpec.DarkScrimAlpha else LiquidGlassSpec.LightScrimAlpha
    val scrim: Color = surface.copy(alpha = scrimAlpha)
    val glassHighlight = highlight?.invoke(isDark)

    if (backdrop == null) {
        // 没有图层可采样时的兜底：半透明实底，比纯透明安全（纯透明会让白字变看不见）。
        return this.clip(shapeProvider()).background(surface.copy(alpha = 0.9f))
    }

    val radiusPx = with(density) { blurRadius.toPx() }

    if (!isRuntimeShaderSupported()) {
        return this.textureBlur(
            backdrop = backdrop,
            shape = shapeProvider(),
            blurRadius = radiusPx,
            colors = BlurColors(blendColors = listOf(BlendColorEntry(scrim))),
            highlight = glassHighlight,
        )
    }

    val refractionHeightPx = with(density) { refractionHeight.toPx() }
    val refractionAmountPx = with(density) { refractionAmount.toPx() }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = shapeProvider,
        effects = {
            vibrancy()
            blur(radiusPx, radiusPx)
            lens(
                refractionHeight = refractionHeightPx,
                refractionAmount = refractionAmountPx,
                chromaticAberration = LiquidGlassSpec.dispersionOrDefault(dispersion),
            )
        },
        highlight = { glassHighlight },
        onDrawSurface = { drawRect(scrim) },
    )
}

/**
 * 圆形液态玻璃 —— 通用小圆按钮（40dp 级别）。
 *
 * 圆形直径只有 40dp（半径 20dp），所以折射参数单独取 [LiquidGlassSpec.CircleRefractionHeight]
 * / [LiquidGlassSpec.CircleRefractionAmount]，否则整个圆面都会在扭曲。
 */
@Composable
fun Modifier.liquidGlassCircle(backdrop: LayerBackdrop?): Modifier = liquidGlassSurface(
    backdrop = backdrop,
    // CircleShape 本身就是 CornerBasedShape（Percent(50)），lens() 能正常取到圆角半径。
    shapeProvider = { CircleShape },
    refractionHeight = LiquidGlassSpec.CircleRefractionHeight,
    refractionAmount = LiquidGlassSpec.CircleRefractionAmount,
)

/**
 * 【组件 1 专用】右上角电源触发器的材质：56dp 正圆，
 * 模糊 20dp / 色散 0.015 / 1.2dp 边缘高光 / 半透明基底。
 *
 * 与卡片走同一套 RuntimeShader 管线，只是参数按需求单独收敛：
 * 圆面折射带宽取 12dp（小于半径 28dp，否则整个圆都在扭曲）。
 */
@Composable
fun Modifier.liquidGlassTrigger(backdrop: LayerBackdrop?): Modifier = liquidGlassSurface(
    backdrop = backdrop,
    shapeProvider = { CircleShape },
    refractionHeight = LiquidGlassSpec.TriggerRefractionHeight,
    refractionAmount = LiquidGlassSpec.TriggerRefractionAmount,
    blurRadius = LiquidGlassSpec.TriggerBlurRadius,
    dispersion = LiquidGlassSpec.TriggerDispersion,
    highlight = { isDark ->
        val preset = if (isDark) Highlight.GlassStrokeSmallDark else Highlight.GlassStrokeSmallLight
        preset.copy(width = LiquidGlassSpec.TriggerHighlightWidth)
    },
)

/** 弹出卡片的圆角（电源菜单等）。与卡片圆角同族，读起来像同一套材质。 */
val PopupCardCornerRadius: Dp = 20.dp
