package com.xinsu.moe.ui.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

fun ThemeTokenBundle.toMaterialColorScheme(isDark: Boolean): ColorScheme =
    (if (isDark) dark else light).toMaterialColorScheme(isDark)

fun ThemeTokenBundle.toMiuixColorScheme(isDark: Boolean): MiuixColors =
    (if (isDark) dark else light).toMiuixColorScheme(isDark)

fun SemanticColorTokens.toMaterialColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary.toComposeColor(),
        onPrimary = onPrimary.toComposeColor(),
        primaryContainer = primaryContainer.toComposeColor(),
        onPrimaryContainer = onPrimaryContainer.toComposeColor(),
        inversePrimary = inversePrimary.toComposeColor(),
        secondary = secondary.toComposeColor(),
        onSecondary = onSecondary.toComposeColor(),
        secondaryContainer = secondaryContainer.toComposeColor(),
        onSecondaryContainer = onSecondaryContainer.toComposeColor(),
        tertiary = tertiary.toComposeColor(),
        onTertiary = onTertiary.toComposeColor(),
        tertiaryContainer = tertiaryContainer.toComposeColor(),
        onTertiaryContainer = onTertiaryContainer.toComposeColor(),
        background = background.toComposeColor(),
        onBackground = onBackground.toComposeColor(),
        surface = surface.toComposeColor(),
        onSurface = onSurface.toComposeColor(),
        surfaceVariant = surfaceVariant.toComposeColor(),
        onSurfaceVariant = onSurfaceVariant.toComposeColor(),
        surfaceContainerLowest = surfaceContainerLowest.toComposeColor(),
        surfaceContainerLow = surfaceContainerLow.toComposeColor(),
        surfaceContainer = surfaceContainer.toComposeColor(),
        surfaceContainerHigh = surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = surfaceContainerHighest.toComposeColor(),
        surfaceBright = surfaceBright.toComposeColor(),
        surfaceDim = surfaceDim.toComposeColor(),
        outline = outline.toComposeColor(),
        outlineVariant = outlineVariant.toComposeColor(),
        inverseSurface = inverseSurface.toComposeColor(),
        inverseOnSurface = inverseOnSurface.toComposeColor(),
        error = error.toComposeColor(),
        onError = onError.toComposeColor(),
        errorContainer = errorContainer.toComposeColor(),
        onErrorContainer = onErrorContainer.toComposeColor(),
        scrim = scrim.toComposeColor(),
    )
}

fun SemanticColorTokens.toMiuixColorScheme(isDark: Boolean): MiuixColors {
    fun dark(): MiuixColors = miuixDarkColorScheme(
        primary = primary.toComposeColor(), onPrimary = onPrimary.toComposeColor(),
        primaryVariant = primaryContainer.toComposeColor(), onPrimaryVariant = onPrimaryContainer.toComposeColor(),
        primaryContainer = primaryContainer.toComposeColor(), onPrimaryContainer = onPrimaryContainer.toComposeColor(),
        secondary = secondary.toComposeColor(), onSecondary = onSecondary.toComposeColor(),
        secondaryVariant = secondaryContainer.toComposeColor(), onSecondaryVariant = onSecondaryContainer.toComposeColor(),
        secondaryContainer = secondaryContainer.toComposeColor(), onSecondaryContainer = onSecondaryContainer.toComposeColor(),
        tertiaryContainer = tertiaryContainer.toComposeColor(), onTertiaryContainer = onTertiaryContainer.toComposeColor(),
        background = background.toComposeColor(), onBackground = onBackground.toComposeColor(),
        onBackgroundVariant = onSurfaceVariant.toComposeColor(),
        surface = surface.toComposeColor(), onSurface = onSurface.toComposeColor(),
        surfaceVariant = surfaceVariant.toComposeColor(),
        onSurfaceSecondary = onSurfaceVariant.toComposeColor(),
        onSurfaceVariantSummary = onSurfaceVariant.toComposeColor(),
        onSurfaceVariantActions = onSurfaceVariant.toComposeColor(),
        surfaceContainer = surfaceContainer.toComposeColor(), onSurfaceContainer = onSurface.toComposeColor(),
        onSurfaceContainerVariant = onSurfaceVariant.toComposeColor(),
        surfaceContainerHigh = surfaceContainerHigh.toComposeColor(), onSurfaceContainerHigh = onSurface.toComposeColor(),
        surfaceContainerHighest = surfaceContainerHighest.toComposeColor(), onSurfaceContainerHighest = onSurface.toComposeColor(),
        outline = outline.toComposeColor(), dividerLine = outlineVariant.toComposeColor(),
    )

    fun light(): MiuixColors = miuixLightColorScheme(
        primary = primary.toComposeColor(), onPrimary = onPrimary.toComposeColor(),
        primaryVariant = primaryContainer.toComposeColor(), onPrimaryVariant = onPrimaryContainer.toComposeColor(),
        primaryContainer = primaryContainer.toComposeColor(), onPrimaryContainer = onPrimaryContainer.toComposeColor(),
        secondary = secondary.toComposeColor(), onSecondary = onSecondary.toComposeColor(),
        secondaryVariant = secondaryContainer.toComposeColor(), onSecondaryVariant = onSecondaryContainer.toComposeColor(),
        secondaryContainer = secondaryContainer.toComposeColor(), onSecondaryContainer = onSecondaryContainer.toComposeColor(),
        tertiaryContainer = tertiaryContainer.toComposeColor(), onTertiaryContainer = onTertiaryContainer.toComposeColor(),
        background = background.toComposeColor(), onBackground = onBackground.toComposeColor(),
        onBackgroundVariant = onSurfaceVariant.toComposeColor(),
        surface = surface.toComposeColor(), onSurface = onSurface.toComposeColor(),
        surfaceVariant = surfaceVariant.toComposeColor(),
        onSurfaceSecondary = onSurfaceVariant.toComposeColor(),
        onSurfaceVariantSummary = onSurfaceVariant.toComposeColor(),
        onSurfaceVariantActions = onSurfaceVariant.toComposeColor(),
        surfaceContainer = surfaceContainer.toComposeColor(), onSurfaceContainer = onSurface.toComposeColor(),
        onSurfaceContainerVariant = onSurfaceVariant.toComposeColor(),
        surfaceContainerHigh = surfaceContainerHigh.toComposeColor(), onSurfaceContainerHigh = onSurface.toComposeColor(),
        surfaceContainerHighest = surfaceContainerHighest.toComposeColor(), onSurfaceContainerHighest = onSurface.toComposeColor(),
        outline = outline.toComposeColor(), dividerLine = outlineVariant.toComposeColor(),
    )

    return if (isDark) dark() else light()
}

// Tokens are packed as 0xAARRGGBB ARGB longs. Compose's Color(ULong) expects its own packed
// representation (with a colour-space id in the low bits), so feeding a raw ARGB value there
// produces a bogus colour space and crashes downstream. Convert through the ARGB Int constructor.
fun Long.toComposeColor(): Color = Color((this and 0xFFFFFFFFL).toInt())
