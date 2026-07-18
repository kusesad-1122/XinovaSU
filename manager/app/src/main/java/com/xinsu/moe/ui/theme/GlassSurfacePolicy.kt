package com.xinsu.moe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.lightColorScheme as cloneMiuixColorScheme

internal fun Color.multiplyAlpha(fraction: Float): Color =
    copy(alpha = alpha * fraction.coerceIn(0f, 1f))

internal fun ColorScheme.applyGlassSurfaces(
    active: Boolean,
    opacity: Float,
): ColorScheme {
    if (!active) return this

    return copy(
        background = Color.Transparent,
        surface = surface.multiplyAlpha(opacity),
        surfaceDim = surfaceDim.multiplyAlpha(opacity),
        surfaceBright = surfaceBright.multiplyAlpha(opacity),
        surfaceContainerLowest = surfaceContainerLowest.multiplyAlpha(opacity),
        surfaceContainerLow = surfaceContainerLow.multiplyAlpha(opacity),
        surfaceContainer = surfaceContainer.multiplyAlpha(opacity),
        surfaceContainerHigh = surfaceContainerHigh.multiplyAlpha(opacity),
        surfaceContainerHighest = surfaceContainerHighest.multiplyAlpha(opacity),
        surfaceVariant = surfaceVariant.multiplyAlpha(opacity),
    )
}

internal fun MiuixColors.applyGlassSurfaces(
    active: Boolean,
    opacity: Float,
): MiuixColors {
    if (!active) return this

    return cloneMiuixColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimary,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButton,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryVariant = secondaryVariant,
        onSecondaryVariant = onSecondaryVariant,
        disabledSecondary = disabledSecondary,
        disabledOnSecondary = disabledOnSecondary,
        disabledSecondaryVariant = disabledSecondaryVariant,
        disabledOnSecondaryVariant = disabledOnSecondaryVariant,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        secondaryContainerVariant = secondaryContainerVariant,
        onSecondaryContainerVariant = onSecondaryContainerVariant,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        background = Color.Transparent,
        onBackground = onBackground,
        onBackgroundVariant = onBackgroundVariant,
        surface = surface.multiplyAlpha(opacity),
        onSurface = onSurface,
        surfaceVariant = surfaceVariant.multiplyAlpha(opacity),
        onSurfaceSecondary = onSurfaceSecondary,
        onSurfaceVariantSummary = onSurfaceVariantSummary,
        onSurfaceVariantActions = onSurfaceVariantActions,
        disabledOnSurface = disabledOnSurface,
        surfaceContainer = surfaceContainer.multiplyAlpha(opacity),
        onSurfaceContainer = onSurfaceContainer,
        onSurfaceContainerVariant = onSurfaceContainerVariant,
        surfaceContainerHigh = surfaceContainerHigh.multiplyAlpha(opacity),
        onSurfaceContainerHigh = onSurfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest.multiplyAlpha(opacity),
        onSurfaceContainerHighest = onSurfaceContainerHighest,
        outline = outline,
        dividerLine = dividerLine,
        windowDimming = windowDimming,
        sliderKeyPoint = sliderKeyPoint,
        sliderKeyPointForeground = sliderKeyPointForeground,
        sliderBackground = sliderBackground,
    )
}

@Composable
@ReadOnlyComposable
internal fun glassExplicitContainerColor(opaque: Color): Color =
    if (LocalBackgroundStyle.current.isActive) {
        opaque.multiplyAlpha(LocalCardOpacity.current / 100f)
    } else {
        opaque
    }

@Composable
@ReadOnlyComposable
internal fun transparentChromeColor(opaque: Color): Color =
    if (LocalBackgroundStyle.current.isActive) Color.Transparent else opaque
