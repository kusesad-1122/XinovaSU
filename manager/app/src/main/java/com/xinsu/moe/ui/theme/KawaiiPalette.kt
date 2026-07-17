package com.xinsu.moe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// Hand-authored "kawaii tech" pastel theme presets, applied app-wide across both the
// Material and Miuix renderers. Unlike the seed-driven (MaterialKolor / Monet) path, each
// preset is a fully specified light/dark palette so the exact accent trio is preserved
// instead of being harmonised away from a single seed color.
//
// Design language (XinovaSU's own identity, not a fork of any other manager):
//  - a shared deep-navy "night" canvas in dark mode / soft blush canvas in light mode,
//  - dark-mode-first tuning,
//  - a signature electric-cyan neon accent shared across the family,
//  - each preset only swaps the primary/secondary/tertiary accent trio.
enum class KawaiiPalette {
    None,
    Sakura,
    Mint,
    Lavender,
    Cyber;

    companion object {
        // Parse a stored preference value, falling back to None on any mismatch.
        fun fromName(value: String?): KawaiiPalette =
            entries.firstOrNull { it.name == value } ?: None
    }
}

val KawaiiPalette.isActive: Boolean get() = this != KawaiiPalette.None

// --- Shared canvas (neutrals) -------------------------------------------------------------

private object DarkCanvas {
    val background = Color(0xFF1A1A2E)
    val onBackground = Color(0xFFF0E8F0)
    val onBackgroundVariant = Color(0xFFC5BFD6)
    val surface = Color(0xFF20203A)
    val onSurface = Color(0xFFF0E8F0)
    val surfaceVariant = Color(0xFF2D2D44)
    val onSurfaceVariant = Color(0xFFC5BFD6)
    val surfaceContainerLowest = Color(0xFF141420)
    val surfaceContainerLow = Color(0xFF1C1C30)
    val surfaceContainer = Color(0xFF24243C)
    val surfaceContainerHigh = Color(0xFF2D2D44)
    val surfaceContainerHighest = Color(0xFF383858)
    val surfaceBright = Color(0xFF3A3A56)
    val surfaceDim = Color(0xFF141420)
    val outline = Color(0xFF716B8A)
    val outlineVariant = Color(0xFF3B3B56)
    val inverseSurface = Color(0xFFF0E8F0)
    val inverseOnSurface = Color(0xFF2D2D3F)
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF5C1A1A)
    val errorContainer = Color(0xFF7A2A2A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val scrim = Color(0xFF000000)
}

private object LightCanvas {
    val background = Color(0xFFFFF5F7)
    val onBackground = Color(0xFF2D2D3F)
    val onBackgroundVariant = Color(0xFF5C5568)
    val surface = Color(0xFFFFFBFC)
    val onSurface = Color(0xFF2D2D3F)
    val surfaceVariant = Color(0xFFF1E6EC)
    val onSurfaceVariant = Color(0xFF5C5568)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFFDF3F7)
    val surfaceContainer = Color(0xFFF9EBF1)
    val surfaceContainerHigh = Color(0xFFF4E4EC)
    val surfaceContainerHighest = Color(0xFFEEDEE7)
    val surfaceBright = Color(0xFFFFFBFC)
    val surfaceDim = Color(0xFFEDDEE6)
    val outline = Color(0xFF9A93A6)
    val outlineVariant = Color(0xFFD7CDD9)
    val inverseSurface = Color(0xFF2D2D3F)
    val inverseOnSurface = Color(0xFFFFF5F7)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
    val scrim = Color(0xFF000000)
}

// --- Per-preset accent trio ---------------------------------------------------------------

private class Accents(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    // Signature neon accent, kept as an extra token for later phases (glow / glass highlights).
    val neon: Color,
)

private val SakuraDark = Accents(
    primary = Color(0xFFFFB7C5), onPrimary = Color(0xFF4A2531),
    primaryContainer = Color(0xFF6B4351), onPrimaryContainer = Color(0xFFFFD9E1),
    inversePrimary = Color(0xFFB84A63),
    secondary = Color(0xFF87CEEB), onSecondary = Color(0xFF05323F),
    secondaryContainer = Color(0xFF2E4A59), onSecondaryContainer = Color(0xFFC6E9F7),
    tertiary = Color(0xFFDDA0DD), onTertiary = Color(0xFF3E2A42),
    tertiaryContainer = Color(0xFF55405A), onTertiaryContainer = Color(0xFFF4DCF4),
    neon = Color(0xFF7DF9FF),
)
private val SakuraLight = Accents(
    primary = Color(0xFFB84A63), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E1), onPrimaryContainer = Color(0xFF3E0A1B),
    inversePrimary = Color(0xFFFFB7C5),
    secondary = Color(0xFF356B84), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6E9F7), onSecondaryContainer = Color(0xFF04212E),
    tertiary = Color(0xFF87508F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4DCF4), onTertiaryContainer = Color(0xFF2C0F36),
    neon = Color(0xFF1E9BAA),
)

private val MintDark = Accents(
    primary = Color(0xFF9EE6C9), onPrimary = Color(0xFF07352A),
    primaryContainer = Color(0xFF2E5346), onPrimaryContainer = Color(0xFFBFF2DF),
    inversePrimary = Color(0xFF3C9C7C),
    secondary = Color(0xFF9AD9E0), onSecondary = Color(0xFF06333A),
    secondaryContainer = Color(0xFF2C4A50), onSecondaryContainer = Color(0xFFC2ECF1),
    tertiary = Color(0xFFB7C7F0), onTertiary = Color(0xFF1E2A4A),
    tertiaryContainer = Color(0xFF3A4468), onTertiaryContainer = Color(0xFFDBE2FA),
    neon = Color(0xFF7DF9FF),
)
private val MintLight = Accents(
    primary = Color(0xFF2C7A5F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFF2DF), onPrimaryContainer = Color(0xFF00281C),
    inversePrimary = Color(0xFF9EE6C9),
    secondary = Color(0xFF2E6B73), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2ECF1), onSecondaryContainer = Color(0xFF042A30),
    tertiary = Color(0xFF4A5896), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDBE2FA), onTertiaryContainer = Color(0xFF0A1642),
    neon = Color(0xFF1E9BAA),
)

private val LavenderDark = Accents(
    primary = Color(0xFFCBB2F0), onPrimary = Color(0xFF2E1E4A),
    primaryContainer = Color(0xFF453764), onPrimaryContainer = Color(0xFFE7DBFA),
    inversePrimary = Color(0xFF7E5CC0),
    secondary = Color(0xFFC2A8E0), onSecondary = Color(0xFF2A1C42),
    secondaryContainer = Color(0xFF433459), onSecondaryContainer = Color(0xFFE5D8F5),
    tertiary = Color(0xFFF0A8CE), onTertiary = Color(0xFF45202F),
    tertiaryContainer = Color(0xFF63384C), onTertiaryContainer = Color(0xFFFCD9E9),
    neon = Color(0xFF7DF9FF),
)
private val LavenderLight = Accents(
    primary = Color(0xFF6A4C9C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DBFA), onPrimaryContainer = Color(0xFF23103F),
    inversePrimary = Color(0xFFCBB2F0),
    secondary = Color(0xFF6B4F94), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5D8F5), onSecondaryContainer = Color(0xFF200E3B),
    tertiary = Color(0xFFA84E7A), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCD9E9), onTertiaryContainer = Color(0xFF3A0F26),
    neon = Color(0xFF1E9BAA),
)

private val CyberDark = Accents(
    primary = Color(0xFF7DF9FF), onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF105057), onPrimaryContainer = Color(0xFFB0F7FC),
    inversePrimary = Color(0xFF1C8C97),
    secondary = Color(0xFF8FB8F0), onSecondary = Color(0xFF07284A),
    secondaryContainer = Color(0xFF264364), onSecondaryContainer = Color(0xFFCFE0FB),
    tertiary = Color(0xFFC79BF0), onTertiary = Color(0xFF33204A),
    tertiaryContainer = Color(0xFF483963), onTertiaryContainer = Color(0xFFE9DBFA),
    neon = Color(0xFFFF7DE1),
)
private val CyberLight = Accents(
    primary = Color(0xFF00727E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB0F7FC), onPrimaryContainer = Color(0xFF002024),
    inversePrimary = Color(0xFF7DF9FF),
    secondary = Color(0xFF33608E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE0FB), onSecondaryContainer = Color(0xFF0A1D33),
    tertiary = Color(0xFF6D4E96), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9DBFA), onTertiaryContainer = Color(0xFF260F41),
    neon = Color(0xFFC02BA0),
)

private fun KawaiiPalette.accents(isDark: Boolean): Accents = when (this) {
    KawaiiPalette.Sakura -> if (isDark) SakuraDark else SakuraLight
    KawaiiPalette.Mint -> if (isDark) MintDark else MintLight
    KawaiiPalette.Lavender -> if (isDark) LavenderDark else LavenderLight
    KawaiiPalette.Cyber -> if (isDark) CyberDark else CyberLight
    KawaiiPalette.None -> if (isDark) SakuraDark else SakuraLight
}

// The signature neon accent for the active preset/mode (exposed for later glow/glass work).
fun KawaiiPalette.neonAccent(isDark: Boolean): Color = accents(isDark).neon

// --- Material3 color scheme ----------------------------------------------------------------

fun KawaiiPalette.materialColorScheme(isDark: Boolean): ColorScheme {
    val a = accents(isDark)
    return if (isDark) {
        darkColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            inversePrimary = a.inversePrimary,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiary = a.tertiary, onTertiary = a.onTertiary,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = DarkCanvas.background, onBackground = DarkCanvas.onBackground,
            surface = DarkCanvas.surface, onSurface = DarkCanvas.onSurface,
            surfaceVariant = DarkCanvas.surfaceVariant, onSurfaceVariant = DarkCanvas.onSurfaceVariant,
            surfaceContainerLowest = DarkCanvas.surfaceContainerLowest,
            surfaceContainerLow = DarkCanvas.surfaceContainerLow,
            surfaceContainer = DarkCanvas.surfaceContainer,
            surfaceContainerHigh = DarkCanvas.surfaceContainerHigh,
            surfaceContainerHighest = DarkCanvas.surfaceContainerHighest,
            surfaceBright = DarkCanvas.surfaceBright, surfaceDim = DarkCanvas.surfaceDim,
            outline = DarkCanvas.outline, outlineVariant = DarkCanvas.outlineVariant,
            inverseSurface = DarkCanvas.inverseSurface, inverseOnSurface = DarkCanvas.inverseOnSurface,
            error = DarkCanvas.error, onError = DarkCanvas.onError,
            errorContainer = DarkCanvas.errorContainer, onErrorContainer = DarkCanvas.onErrorContainer,
            scrim = DarkCanvas.scrim,
        )
    } else {
        lightColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            inversePrimary = a.inversePrimary,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiary = a.tertiary, onTertiary = a.onTertiary,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = LightCanvas.background, onBackground = LightCanvas.onBackground,
            surface = LightCanvas.surface, onSurface = LightCanvas.onSurface,
            surfaceVariant = LightCanvas.surfaceVariant, onSurfaceVariant = LightCanvas.onSurfaceVariant,
            surfaceContainerLowest = LightCanvas.surfaceContainerLowest,
            surfaceContainerLow = LightCanvas.surfaceContainerLow,
            surfaceContainer = LightCanvas.surfaceContainer,
            surfaceContainerHigh = LightCanvas.surfaceContainerHigh,
            surfaceContainerHighest = LightCanvas.surfaceContainerHighest,
            surfaceBright = LightCanvas.surfaceBright, surfaceDim = LightCanvas.surfaceDim,
            outline = LightCanvas.outline, outlineVariant = LightCanvas.outlineVariant,
            inverseSurface = LightCanvas.inverseSurface, inverseOnSurface = LightCanvas.inverseOnSurface,
            error = LightCanvas.error, onError = LightCanvas.onError,
            errorContainer = LightCanvas.errorContainer, onErrorContainer = LightCanvas.onErrorContainer,
            scrim = LightCanvas.scrim,
        )
    }
}

// --- Miuix color scheme --------------------------------------------------------------------

fun KawaiiPalette.miuixColorScheme(isDark: Boolean): MiuixColors {
    val a = accents(isDark)
    return if (isDark) {
        miuixDarkColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryVariant = a.primaryContainer, onPrimaryVariant = a.onPrimaryContainer,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryVariant = a.secondaryContainer, onSecondaryVariant = a.onSecondaryContainer,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = DarkCanvas.background, onBackground = DarkCanvas.onBackground,
            onBackgroundVariant = DarkCanvas.onBackgroundVariant,
            surface = DarkCanvas.surface, onSurface = DarkCanvas.onSurface,
            surfaceVariant = DarkCanvas.surfaceVariant,
            onSurfaceSecondary = DarkCanvas.onSurfaceVariant,
            onSurfaceVariantSummary = DarkCanvas.onSurfaceVariant,
            onSurfaceVariantActions = DarkCanvas.onSurfaceVariant,
            surfaceContainer = DarkCanvas.surfaceContainer,
            onSurfaceContainer = DarkCanvas.onSurface,
            onSurfaceContainerVariant = DarkCanvas.onSurfaceVariant,
            surfaceContainerHigh = DarkCanvas.surfaceContainerHigh,
            onSurfaceContainerHigh = DarkCanvas.onSurface,
            surfaceContainerHighest = DarkCanvas.surfaceContainerHighest,
            onSurfaceContainerHighest = DarkCanvas.onSurface,
            outline = DarkCanvas.outline, dividerLine = DarkCanvas.outlineVariant,
        )
    } else {
        miuixLightColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryVariant = a.primaryContainer, onPrimaryVariant = a.onPrimaryContainer,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryVariant = a.secondaryContainer, onSecondaryVariant = a.onSecondaryContainer,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = LightCanvas.background, onBackground = LightCanvas.onBackground,
            onBackgroundVariant = LightCanvas.onBackgroundVariant,
            surface = LightCanvas.surface, onSurface = LightCanvas.onSurface,
            surfaceVariant = LightCanvas.surfaceVariant,
            onSurfaceSecondary = LightCanvas.onSurfaceVariant,
            onSurfaceVariantSummary = LightCanvas.onSurfaceVariant,
            onSurfaceVariantActions = LightCanvas.onSurfaceVariant,
            surfaceContainer = LightCanvas.surfaceContainer,
            onSurfaceContainer = LightCanvas.onSurface,
            onSurfaceContainerVariant = LightCanvas.onSurfaceVariant,
            surfaceContainerHigh = LightCanvas.surfaceContainerHigh,
            onSurfaceContainerHigh = LightCanvas.onSurface,
            surfaceContainerHighest = LightCanvas.surfaceContainerHighest,
            onSurfaceContainerHighest = LightCanvas.onSurface,
            outline = LightCanvas.outline, dividerLine = LightCanvas.outlineVariant,
        )
    }
}
