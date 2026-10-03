// Kyant 引擎版的液态玻璃卡片 —— 用 io.github.kyant0:backdrop（即 Kyant0/AndroidLiquidGlass，
// 2.x 起改称 Backdrop）的 Backdrop API 实现，与 Miuix 引擎版（MiuixGlassCard）共用：
//   • 同一套视觉规格：LiquidGlassSpec
//   • 同一份色散 shader：ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER（见 Lens.kt，
//     文件头即标注 Adapted from Kyant0/AndroidLiquidGlass，Apache 2.0）
//
// 与上游 API 的差异（必须知道，否则会把色散做得过重）：
//   上游 2.0.1 的 lens() 只暴露 `chromaticAberration: Boolean`，shader 内强度被写死 1.0；
//   本文件改用 runtimeShaderEffect 注入本项目那份把强度参数化的 shader，从而精确取到 0.02。
//
// 引擎选择：真库的 Backdrop 与 miuix-blur 的 LayerBackdrop 是两套不兼容的类型，各自需要
// 一次图层捕获。因此在同一个界面上叠用两个引擎会让图层捕获翻倍、滚动掉帧 —— 正确用法是
// **按界面分工**：全局卡片走 Miuix 引擎（全应用一次捕获），需要自成一体的玻璃表面（弹层、
// 单张主卡、以及本文件的 @Preview）走 Kyant 引擎。
package com.xinsu.moe.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

/**
 * 建一个 Kyant 引擎的 backdrop。调用方必须把 [layerBackdrop] 挂到"玻璃背后那层内容"上，
 * 否则玻璃会采样到空白：
 * ```
 * val backdrop = rememberKyantGlassBackdrop()
 * Box {
 *     Box(Modifier.matchParentSize().layerBackdrop(backdrop)) { 背景内容() }
 *     KyantLiquidGlassCard(backdrop) { 卡片内容() }
 * }
 * ```
 */
@Composable
fun rememberKyantGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * Kyant 引擎的液态玻璃卡片。
 *
 * @param backdrop 由 [rememberKyantGlassBackdrop] 创建、并已用 `Modifier.layerBackdrop` 绑定到
 *   背景内容层的 backdrop。
 * @param shape 卡片形状，默认 [LiquidGlassSpec.CardCornerRadius]。
 * @param containerColor 底色基底；实际绘制时按明暗自动套上 [LiquidGlassSpec] 的对比度不透明度。
 * @param pressDeformation 是否启用"仅按压时"的轻微弹性形变（长列表可逐项关掉）。
 */
@Composable
fun KyantLiquidGlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LiquidGlassSpec.CardCornerRadius),
    containerColor: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    pressDeformation: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = containerColor.luminance() < 0.5f
    val scrimAlpha = if (isDark) LiquidGlassSpec.DarkScrimAlpha else LiquidGlassSpec.LightScrimAlpha
    // 注意预设名的来源差异：GlassStroke* 是 miuix-blur 的 Highlight 预设，
    // 真库 io.github.kyant0:backdrop 只有 Default / Ambient / Plain（见其 Highlight.kt）。
    // 厚度与透明度按 LiquidGlassSpec 统一，深色模式压低一档 alpha 避免边缘过亮。
    val highlight = Highlight.Default.copy(
        width = LiquidGlassSpec.HighlightWidth,
        alpha = if (isDark) 0.8f else 1f,
    )

    Box(
        modifier = modifier
            .liquidPressScale(enabled = pressDeformation)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(LiquidGlassSpec.BlurRadius.toPx())
                    // 上游 lens() 的色散只有布尔开关（强度写死 1.0），这里直接注入本项目
                    // 参数化的那一份 shader，精确落到 LiquidGlassSpec.Dispersion。
                    val cornerRadii = roundedRectRadii(shape, size, layoutDirection, this)
                    if (cornerRadii == null) {
                        lens(
                            refractionHeight = LiquidGlassSpec.RefractionHeight.toPx(),
                            refractionAmount = LiquidGlassSpec.RefractionAmount.toPx(),
                            chromaticAberration = true,
                        )
                    } else {
                        val refractionHeight = LiquidGlassSpec.RefractionHeight.toPx()
                        val refractionAmount = LiquidGlassSpec.RefractionAmount.toPx()
                        // 折射会在边缘外扩一圈采样，先撑开 padding，offset 再据此对齐。
                        if (padding < refractionAmount) padding = refractionAmount
                        runtimeShaderEffect(
                            key = "KyantLiquidGlassLensDispersion",
                            shaderString = ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER,
                            uniformShaderName = "content",
                        ) {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("offset", -padding, -padding)
                            setFloatUniform("cornerRadii", cornerRadii)
                            setFloatUniform("refractionHeight", refractionHeight)
                            setFloatUniform("refractionAmount", -refractionAmount)
                            setFloatUniform("depthEffect", 0f)
                            setFloatUniform("chromaticAberration", LiquidGlassSpec.Dispersion)
                        }
                    }
                },
                highlight = { highlight },
                onDrawSurface = { drawRect(containerColor.copy(alpha = scrimAlpha)) },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * 取圆角半径四元组（左上、右上、右下、左下）。形状非 [CornerBasedShape] 时返回 null，
 * 由调用方退回上游 lens()。
 */
private fun roundedRectRadii(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): FloatArray? {
    val cornerShape = shape as? CornerBasedShape ?: return null
    val maxRadius = size.minDimension / 2f
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft =
        if (isLtr) cornerShape.topStart.toPx(size, density) else cornerShape.topEnd.toPx(size, density)
    val topRight =
        if (isLtr) cornerShape.topEnd.toPx(size, density) else cornerShape.topStart.toPx(size, density)
    val bottomRight =
        if (isLtr) cornerShape.bottomEnd.toPx(size, density) else cornerShape.bottomStart.toPx(size, density)
    val bottomLeft =
        if (isLtr) cornerShape.bottomStart.toPx(size, density) else cornerShape.bottomEnd.toPx(size, density)
    return floatArrayOf(
        topLeft.fastCoerceAtMost(maxRadius),
        topRight.fastCoerceAtMost(maxRadius),
        bottomRight.fastCoerceAtMost(maxRadius),
        bottomLeft.fastCoerceAtMost(maxRadius),
    )
}

// ───────────────────────────── 预览 ─────────────────────────────
//
// 自包含预览：背景层用 Modifier.layerBackdrop 绑定到同一个 backdrop，
// 这样 Android Studio 里能直接看到真实的折射与色散（静态渲染不含动画，属正常）。

@Preview(name = "Kyant 液态玻璃卡片 · 浅色", widthDp = 360, heightDp = 420)
@Composable
private fun KyantLiquidGlassCardLightPreview() {
    KyantLiquidGlassCardPreviewSurface(containerColor = Color.White)
}

@Preview(name = "Kyant 液态玻璃卡片 · 深色", widthDp = 360, heightDp = 420)
@Composable
private fun KyantLiquidGlassCardDarkPreview() {
    KyantLiquidGlassCardPreviewSurface(containerColor = Color(0xFF141218))
}

@Composable
private fun KyantLiquidGlassCardPreviewSurface(containerColor: Color) {
    val isDark = containerColor.luminance() < 0.5f
    val backdrop = rememberKyantGlassBackdrop()
    val onSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

    Box(modifier = Modifier.fillMaxSize().background(containerColor)) {
        // 玻璃背后那层：用渐变小球模拟真实壁纸，方便肉眼判断折射/色散是否过重。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .size(180.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF7F77DD), Color(0xFF1D9E75)),
                        ),
                        RoundedCornerShape(90.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 64.dp)
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFD4537E), Color(0xFFEF9F27)),
                        ),
                        RoundedCornerShape(70.dp),
                    ),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KyantLiquidGlassCard(
                backdrop = backdrop,
                containerColor = containerColor,
            ) {
                androidx.compose.material3.Text(
                    text = "液态玻璃卡片",
                    color = onSurface,
                    fontWeight = FontWeight.Medium,
                )
                androidx.compose.material3.Text(
                    text = "模糊 22dp · 色散 0.02 · 高光 1.2dp · 圆角 18dp",
                    color = onSurface.copy(alpha = 0.75f),
                )
            }
            KyantLiquidGlassCard(
                backdrop = backdrop,
                containerColor = containerColor,
            ) {
                androidx.compose.material3.Text(
                    text = "只换材质，不动版式",
                    color = onSurface,
                    fontWeight = FontWeight.Medium,
                )
                androidx.compose.material3.Text(
                    text = "开关、图标、文字与底部导航均保持原样",
                    color = onSurface.copy(alpha = 0.75f),
                )
            }
        }
    }
}
