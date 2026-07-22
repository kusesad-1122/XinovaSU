package com.xinsu.moe.ui.component.miuix

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.xinsu.moe.ui.component.decoration.DecoratedCardContent
import com.xinsu.moe.ui.component.decoration.miuixDecorationColors
import com.xinsu.moe.ui.theme.LocalCardBackdrop
import com.xinsu.moe.ui.theme.LocalGlassCard
import com.xinsu.moe.ui.theme.LocalHomeCardCornerRadius
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.glassExplicitContainerColor
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

// When the scoped "glass cards" flag is on AND a background backdrop is available, frost the card:
// draw a blurred, tinted sample of the app background behind it (reusing the same miuix-blur stack
// as the liquid-glass bottom bar) and make the card's own container transparent so it shows.
// Returns the glass background modifier (or [Modifier] when off) paired with the container color to
// use.
@Composable
private fun glassCardStyle(cornerRadius: Dp, resolvedContainer: Color): Pair<Modifier, Color> {
    val glassBackdrop = if (LocalGlassCard.current) LocalCardBackdrop.current else null
    return if (glassBackdrop != null) {
        Modifier.textureBlur(
            backdrop = glassBackdrop,
            shape = RoundedCornerShape(cornerRadius),
            blurRadius = 24f,
            colors = BlurColors(
                blendColors = listOf(BlendColorEntry(color = resolvedContainer.copy(alpha = 0.5f))),
            ),
        ) to Color.Transparent
    } else {
        Modifier to resolvedContainer
    }
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
