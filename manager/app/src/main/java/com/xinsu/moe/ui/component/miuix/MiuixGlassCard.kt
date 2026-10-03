package com.xinsu.moe.ui.component.miuix

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.xinsu.moe.ui.component.decoration.DecoratedCardContent
import com.xinsu.moe.ui.component.decoration.miuixDecorationColors
import com.xinsu.moe.ui.component.liquid.LiquidGlassSpec
import com.xinsu.moe.ui.component.liquid.lens
import com.xinsu.moe.ui.component.liquid.liquidPressScale
import com.xinsu.moe.ui.component.liquid.vibrancy
import com.xinsu.moe.ui.theme.LocalCardBackdrop
import com.xinsu.moe.ui.theme.LocalGlassCard
import com.xinsu.moe.ui.theme.LocalHomeCardCornerRadius
import com.xinsu.moe.ui.theme.LocalLiquidGlassSetting
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.glassExplicitContainerColor
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

// When the app-wide "glass cards" flag is on AND a background backdrop is available, turn the card
// into glass by sampling a blurred/tinted copy of the app background behind it, then making the
// card's own container transparent so the sample shows through.
//
// Two material tiers, both driven by the same [LiquidGlassSpec]:
//   • "液态玻璃" on  → full liquid glass: refraction + dispersion + 1.2dp edge highlight, plus a
//                      slight press-only elastic scale.
//   • "液态玻璃" off → frosted tier (textureBlur), the previous behaviour.
// API 31–32 has no AGSL, so `lens()`/`blur()` no-op there and the frosted tier is what you get.
//
// Returns the glass background modifier (or [Modifier] when off) paired with the container colour
// to use.
@Composable
private fun glassCardStyle(cornerRadius: Dp, resolvedContainer: Color): Pair<Modifier, Color> {
    if (!LocalGlassCard.current) return Modifier to resolvedContainer
    // 注意：不能写成 `if (c) X else null ?: return` —— elvis 只绑定到 else 分支的 null 上，
    // 整体类型仍是 X? ，会让后面的 drawBackdrop/textureBlur 拿到可空值。
    val glassBackdrop = LocalCardBackdrop.current ?: return Modifier to resolvedContainer

    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val isDark = resolvedContainer.luminance() < 0.5f
    val scrimAlpha = if (isDark) LiquidGlassSpec.DarkScrimAlpha else LiquidGlassSpec.LightScrimAlpha
    val scrim = resolvedContainer.copy(alpha = scrimAlpha)

    if (!LocalLiquidGlassSetting.current || !isRuntimeShaderSupported()) {
        return Modifier
            .textureBlur(
                backdrop = glassBackdrop,
                shape = shape,
                blurRadius = with(density) { LiquidGlassSpec.BlurRadius.toPx() },
                colors = BlurColors(
                    blendColors = listOf(BlendColorEntry(scrim)),
                ),
            )
            .liquidPressScale() to Color.Transparent
    }

    val highlightBase = if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    val highlight = highlightBase.copy(width = LiquidGlassSpec.HighlightWidth)

    val liquidGlass = Modifier.drawBackdrop(
        backdrop = glassBackdrop,
        shape = { shape },
        effects = {
            vibrancy()
            val radius = LiquidGlassSpec.BlurRadius.toPx()
            blur(radius, radius)
            lens(
                refractionHeight = LiquidGlassSpec.RefractionHeight.toPx(),
                refractionAmount = LiquidGlassSpec.RefractionAmount.toPx(),
                // 色散强度由本项目的 Float 版 lens() 精确控制（上游只给 Boolean 开关）。
                chromaticAberration = LiquidGlassSpec.dispersionOrDefault(),
            )
        },
        highlight = { highlight },
        onDrawSurface = { drawRect(scrim) },
    )

    return liquidGlass.liquidPressScale() to Color.Transparent
}

@Composable
fun MiuixGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LocalHomeCardCornerRadius.current ?: CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    containerColor: Color? = null,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    role: DecoratedCardRole = DecoratedCardRole.Standard,
    active: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolved = containerColor?.let { glassExplicitContainerColor(it) }
        ?: MiuixTheme.colorScheme.surfaceContainer
    val (glassModifier, cardColor) = glassCardStyle(cornerRadius, resolved)
    val decorationColors = miuixDecorationColors()
    MiuixCard(
        modifier = modifier.then(glassModifier),
        cornerRadius = cornerRadius,
        insideMargin = insideMargin,
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = contentColor,
        ),
        content = {
            DecoratedCardContent(
                role = role,
                colors = decorationColors,
                active = active,
                content = content,
            )
        },
    )
}

@Composable
fun MiuixGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LocalHomeCardCornerRadius.current ?: CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    containerColor: Color? = null,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    role: DecoratedCardRole = DecoratedCardRole.Standard,
    active: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolved = containerColor?.let { glassExplicitContainerColor(it) }
        ?: MiuixTheme.colorScheme.surfaceContainer
    val (glassModifier, cardColor) = glassCardStyle(cornerRadius, resolved)
    val decorationColors = miuixDecorationColors()
    MiuixCard(
        modifier = modifier.then(glassModifier),
        cornerRadius = cornerRadius,
        insideMargin = insideMargin,
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = contentColor,
        ),
        pressFeedbackType = pressFeedbackType,
        showIndication = showIndication,
        holdDownState = holdDownState,
        onClick = onClick,
        onLongPress = onLongPress,
        content = {
            DecoratedCardContent(
                role = role,
                colors = decorationColors,
                active = active,
                content = content,
            )
        },
    )
}
