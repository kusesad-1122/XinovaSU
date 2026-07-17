package com.xinsu.moe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// Hand-authored theme presets, applied app-wide across both the Material and Miuix renderers.
// Unlike the seed-driven (MaterialKolor / Monet) path, each preset is a fully specified
// light/dark palette so the exact colors are preserved instead of being harmonised away from a
// single seed.
//
// Two families (XinovaSU's own identity, not a fork of any other manager):
//  - Pastel "kawaii" set (Sakura / Mint / Lavender / Cyber): a shared deep-navy night canvas
//    with a swapped soft accent trio.
//  - Modern set (Obsidian / Mica / Ember / Jade): each ships its own canvas + accents for a
//    distinct, contemporary mood.
// Dark-mode first throughout; the enum name is kept for backwards compatibility of the stored
// "theme_preset" preference value.
enum class KawaiiPalette {
    None,
    Sakura,
    Mint,
    Lavender,
    Cyber,
    Obsidian,
    Mica,
    Ember,
    Jade,
    SakuraVN,
    Snow,
    Moonlit;

    companion object {
        // Parse a stored preference value, falling back to None on any mismatch.
        fun fromName(value: String?): KawaiiPalette =
            entries.firstOrNull { it.name == value } ?: None
    }
}

val KawaiiPalette.isActive: Boolean get() = this != KawaiiPalette.None

// --- Canvas (neutral surfaces) ------------------------------------------------------------

private class Canvas(
    val background: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color = Color(0xFF000000),
)

// Shared pastel night / blush canvas used by the kawaii family.
private val KawaiiDark = Canvas(
    background = Color(0xFF1A1A2E), onBackground = Color(0xFFF0E8F0), onBackgroundVariant = Color(0xFFC5BFD6),
    surface = Color(0xFF20203A), onSurface = Color(0xFFF0E8F0),
    surfaceVariant = Color(0xFF2D2D44), onSurfaceVariant = Color(0xFFC5BFD6),
    surfaceContainerLowest = Color(0xFF141420), surfaceContainerLow = Color(0xFF1C1C30),
    surfaceContainer = Color(0xFF24243C), surfaceContainerHigh = Color(0xFF2D2D44),
    surfaceContainerHighest = Color(0xFF383858),
    surfaceBright = Color(0xFF3A3A56), surfaceDim = Color(0xFF141420),
    outline = Color(0xFF716B8A), outlineVariant = Color(0xFF3B3B56),
    inverseSurface = Color(0xFFF0E8F0), inverseOnSurface = Color(0xFF2D2D3F),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)
private val KawaiiLight = Canvas(
    background = Color(0xFFFFF5F7), onBackground = Color(0xFF2D2D3F), onBackgroundVariant = Color(0xFF5C5568),
    surface = Color(0xFFFFFBFC), onSurface = Color(0xFF2D2D3F),
    surfaceVariant = Color(0xFFF1E6EC), onSurfaceVariant = Color(0xFF5C5568),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFDF3F7),
    surfaceContainer = Color(0xFFF9EBF1), surfaceContainerHigh = Color(0xFFF4E4EC),
    surfaceContainerHighest = Color(0xFFEEDEE7),
    surfaceBright = Color(0xFFFFFBFC), surfaceDim = Color(0xFFEDDEE6),
    outline = Color(0xFF9A93A6), outlineVariant = Color(0xFFD7CDD9),
    inverseSurface = Color(0xFF2D2D3F), inverseOnSurface = Color(0xFFFFF5F7),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

// Obsidian: sleek near-black canvas, cool neutrals.
private val ObsidianDark = Canvas(
    background = Color(0xFF101114), onBackground = Color(0xFFE5E8EE), onBackgroundVariant = Color(0xFFB7BDC7),
    surface = Color(0xFF16181D), onSurface = Color(0xFFE5E8EE),
    surfaceVariant = Color(0xFF262A31), onSurfaceVariant = Color(0xFFC2C7D0),
    surfaceContainerLowest = Color(0xFF0C0D10), surfaceContainerLow = Color(0xFF16181D),
    surfaceContainer = Color(0xFF1A1D22), surfaceContainerHigh = Color(0xFF24272D),
    surfaceContainerHighest = Color(0xFF2E3138),
    surfaceBright = Color(0xFF2E3138), surfaceDim = Color(0xFF0C0D10),
    outline = Color(0xFF7C8290), outlineVariant = Color(0xFF33373F),
    inverseSurface = Color(0xFFE5E8EE), inverseOnSurface = Color(0xFF22252C),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)
private val ObsidianLight = Canvas(
    background = Color(0xFFFBFCFE), onBackground = Color(0xFF1A1C22), onBackgroundVariant = Color(0xFF44474F),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE9EBF0), onSurfaceVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F6FA),
    surfaceContainer = Color(0xFFEEF1F6), surfaceContainerHigh = Color(0xFFE8EBF1),
    surfaceContainerHighest = Color(0xFFE2E6EC),
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFDDE0E7),
    outline = Color(0xFF757984), outlineVariant = Color(0xFFC5C8D0),
    inverseSurface = Color(0xFF1A1C22), inverseOnSurface = Color(0xFFF1F3F8),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

// Mica: neutral, minimal cool slate.
private val MicaDark = Canvas(
    background = Color(0xFF1A1C1E), onBackground = Color(0xFFE2E2E6), onBackgroundVariant = Color(0xFFC4C7CC),
    surface = Color(0xFF1F2123), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF292C2F), onSurfaceVariant = Color(0xFFC4C7CC),
    surfaceContainerLowest = Color(0xFF141618), surfaceContainerLow = Color(0xFF1C1E20),
    surfaceContainer = Color(0xFF212426), surfaceContainerHigh = Color(0xFF2A2D30),
    surfaceContainerHighest = Color(0xFF34383B),
    surfaceBright = Color(0xFF34383B), surfaceDim = Color(0xFF141618),
    outline = Color(0xFF8A8E93), outlineVariant = Color(0xFF3A3D40),
    inverseSurface = Color(0xFFE2E2E6), inverseOnSurface = Color(0xFF2B2D30),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)
private val MicaLight = Canvas(
    background = Color(0xFFF7F9FB), onBackground = Color(0xFF191C1E), onBackgroundVariant = Color(0xFF41484D),
    surface = Color(0xFFFDFDFF), onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE3E7EA), onSurfaceVariant = Color(0xFF41484D),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFECEFF1), surfaceContainerHigh = Color(0xFFE6E9EC),
    surfaceContainerHighest = Color(0xFFE0E4E6),
    surfaceBright = Color(0xFFFDFDFF), surfaceDim = Color(0xFFDCDFE1),
    outline = Color(0xFF71787D), outlineVariant = Color(0xFFC1C7CB),
    inverseSurface = Color(0xFF2D3133), inverseOnSurface = Color(0xFFEFF1F3),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

// Ember: warm charcoal canvas.
private val EmberDark = Canvas(
    background = Color(0xFF1A1613), onBackground = Color(0xFFEFE0D8), onBackgroundVariant = Color(0xFFD6C3B8),
    surface = Color(0xFF221C18), onSurface = Color(0xFFEFE0D8),
    surfaceVariant = Color(0xFF33291F), onSurfaceVariant = Color(0xFFD6C3B8),
    surfaceContainerLowest = Color(0xFF140F0C), surfaceContainerLow = Color(0xFF1C1713),
    surfaceContainer = Color(0xFF241E19), surfaceContainerHigh = Color(0xFF2E271F),
    surfaceContainerHighest = Color(0xFF392F26),
    surfaceBright = Color(0xFF392F26), surfaceDim = Color(0xFF140F0C),
    outline = Color(0xFF9A8578), outlineVariant = Color(0xFF40352C),
    inverseSurface = Color(0xFFEFE0D8), inverseOnSurface = Color(0xFF2E271F),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)
private val EmberLight = Canvas(
    background = Color(0xFFFFF8F4), onBackground = Color(0xFF241914), onBackgroundVariant = Color(0xFF53433B),
    surface = Color(0xFFFFFBF9), onSurface = Color(0xFF241914),
    surfaceVariant = Color(0xFFF3E0D5), onSurfaceVariant = Color(0xFF53433B),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF1E9),
    surfaceContainer = Color(0xFFFCEBE0), surfaceContainerHigh = Color(0xFFF7E4D8),
    surfaceContainerHighest = Color(0xFFF1DECF),
    surfaceBright = Color(0xFFFFFBF9), surfaceDim = Color(0xFFE9D7CC),
    outline = Color(0xFF85736A), outlineVariant = Color(0xFFD8C3B7),
    inverseSurface = Color(0xFF3A2E27), inverseOnSurface = Color(0xFFFFEDE4),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

// Jade: fresh forest-charcoal canvas.
private val JadeDark = Canvas(
    background = Color(0xFF101613), onBackground = Color(0xFFDEE5DE), onBackgroundVariant = Color(0xFFBFCCC3),
    surface = Color(0xFF16201B), onSurface = Color(0xFFDEE5DE),
    surfaceVariant = Color(0xFF263129), onSurfaceVariant = Color(0xFFBFCCC3),
    surfaceContainerLowest = Color(0xFF0B120E), surfaceContainerLow = Color(0xFF141C17),
    surfaceContainer = Color(0xFF18231C), surfaceContainerHigh = Color(0xFF212E25),
    surfaceContainerHighest = Color(0xFF2B392F),
    surfaceBright = Color(0xFF2B392F), surfaceDim = Color(0xFF0B120E),
    outline = Color(0xFF7A8A80), outlineVariant = Color(0xFF333F37),
    inverseSurface = Color(0xFFDEE5DE), inverseOnSurface = Color(0xFF2A342D),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)
private val JadeLight = Canvas(
    background = Color(0xFFF3FAF5), onBackground = Color(0xFF171D19), onBackgroundVariant = Color(0xFF3E4A43),
    surface = Color(0xFFFBFDF9), onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE7DE), onSurfaceVariant = Color(0xFF3E4A43),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F7F1),
    surfaceContainer = Color(0xFFEAF2EB), surfaceContainerHigh = Color(0xFFE4ECE5),
    surfaceContainerHighest = Color(0xFFDEE7E0),
    surfaceBright = Color(0xFFFBFDF9), surfaceDim = Color(0xFFD6E0D8),
    outline = Color(0xFF6E7A72), outlineVariant = Color(0xFFBEC9C0),
    inverseSurface = Color(0xFF2B322D), inverseOnSurface = Color(0xFFECF3ED),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

// Sakura VN: a bright, near-white "visual novel" canvas with the faintest warm-pink undertone
// (light-first, made to sit under a side character illustration), plus a soft plum-charcoal dark.
private val SakuraVNLight = Canvas(
    background = Color(0xFFFFF8FA), onBackground = Color(0xFF3A2E33), onBackgroundVariant = Color(0xFF6E5C64),
    surface = Color(0xFFFFFDFE), onSurface = Color(0xFF3A2E33),
    surfaceVariant = Color(0xFFF3E7EC), onSurfaceVariant = Color(0xFF6E5C64),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFEF6F9),
    surfaceContainer = Color(0xFFFBEFF4), surfaceContainerHigh = Color(0xFFF6E8EF),
    surfaceContainerHighest = Color(0xFFF1E2E9),
    surfaceBright = Color(0xFFFFFDFE), surfaceDim = Color(0xFFF0E2E8),
    outline = Color(0xFFA5949C), outlineVariant = Color(0xFFDCCCD4),
    inverseSurface = Color(0xFF3A2E33), inverseOnSurface = Color(0xFFFDEFF3),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)
private val SakuraVNDark = Canvas(
    background = Color(0xFF1A1417), onBackground = Color(0xFFF0E3E8), onBackgroundVariant = Color(0xFFD3C1C9),
    surface = Color(0xFF201A1D), onSurface = Color(0xFFF0E3E8),
    surfaceVariant = Color(0xFF352B30), onSurfaceVariant = Color(0xFFD3C1C9),
    surfaceContainerLowest = Color(0xFF140F12), surfaceContainerLow = Color(0xFF1C161A),
    surfaceContainer = Color(0xFF231C20), surfaceContainerHigh = Color(0xFF2D242A),
    surfaceContainerHighest = Color(0xFF382E34),
    surfaceBright = Color(0xFF3A2F35), surfaceDim = Color(0xFF140F12),
    outline = Color(0xFF9C8B93), outlineVariant = Color(0xFF4A3E44),
    inverseSurface = Color(0xFFF0E3E8), inverseOnSurface = Color(0xFF2D242A),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)

// Snow: a cool, crisp near-white canvas (winter light) with a deep slate dark.
private val SnowLight = Canvas(
    background = Color(0xFFF7FAFD), onBackground = Color(0xFF1B2733), onBackgroundVariant = Color(0xFF44515F),
    surface = Color(0xFFFCFDFF), onSurface = Color(0xFF1B2733),
    surfaceVariant = Color(0xFFE2E9F0), onSurfaceVariant = Color(0xFF44515F),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F5FA),
    surfaceContainer = Color(0xFFEBF0F6), surfaceContainerHigh = Color(0xFFE5EBF2),
    surfaceContainerHighest = Color(0xFFDFE6EE),
    surfaceBright = Color(0xFFFCFDFF), surfaceDim = Color(0xFFDCE3EB),
    outline = Color(0xFF74808D), outlineVariant = Color(0xFFC3CCD6),
    inverseSurface = Color(0xFF1B2733), inverseOnSurface = Color(0xFFEFF3F8),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)
private val SnowDark = Canvas(
    background = Color(0xFF10151B), onBackground = Color(0xFFDDE4EC), onBackgroundVariant = Color(0xFFBAC4CF),
    surface = Color(0xFF161C23), onSurface = Color(0xFFDDE4EC),
    surfaceVariant = Color(0xFF29323C), onSurfaceVariant = Color(0xFFBAC4CF),
    surfaceContainerLowest = Color(0xFF0B0F14), surfaceContainerLow = Color(0xFF151A20),
    surfaceContainer = Color(0xFF191F26), surfaceContainerHigh = Color(0xFF232A32),
    surfaceContainerHighest = Color(0xFF2E353E),
    surfaceBright = Color(0xFF2E353E), surfaceDim = Color(0xFF0B0F14),
    outline = Color(0xFF828D99), outlineVariant = Color(0xFF39424C),
    inverseSurface = Color(0xFFDDE4EC), inverseOnSurface = Color(0xFF272E36),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)

// Moonlit: a deep indigo night canvas (moon-lit), with a soft cool-lavender light.
private val MoonlitLight = Canvas(
    background = Color(0xFFF6F7FD), onBackground = Color(0xFF1A1C2A), onBackgroundVariant = Color(0xFF45485C),
    surface = Color(0xFFFCFCFF), onSurface = Color(0xFF1A1C2A),
    surfaceVariant = Color(0xFFE4E6F0), onSurfaceVariant = Color(0xFF45485C),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F2FB),
    surfaceContainer = Color(0xFFEBECF7), surfaceContainerHigh = Color(0xFFE5E7F3),
    surfaceContainerHighest = Color(0xFFDFE1EE),
    surfaceBright = Color(0xFFFCFCFF), surfaceDim = Color(0xFFDCDEEC),
    outline = Color(0xFF767989), outlineVariant = Color(0xFFC6C8D6),
    inverseSurface = Color(0xFF1A1C2A), inverseOnSurface = Color(0xFFF0F1F9),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)
private val MoonlitDark = Canvas(
    background = Color(0xFF0E1220), onBackground = Color(0xFFE3E6F2), onBackgroundVariant = Color(0xFFBEC3D8),
    surface = Color(0xFF141827), onSurface = Color(0xFFE3E6F2),
    surfaceVariant = Color(0xFF272C40), onSurfaceVariant = Color(0xFFBEC3D8),
    surfaceContainerLowest = Color(0xFF090C17), surfaceContainerLow = Color(0xFF121624),
    surfaceContainer = Color(0xFF171B2B), surfaceContainerHigh = Color(0xFF212637),
    surfaceContainerHighest = Color(0xFF2B3143),
    surfaceBright = Color(0xFF2B3143), surfaceDim = Color(0xFF090C17),
    outline = Color(0xFF868BA3), outlineVariant = Color(0xFF373D52),
    inverseSurface = Color(0xFFE3E6F2), inverseOnSurface = Color(0xFF262B3B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF5C1A1A),
    errorContainer = Color(0xFF7A2A2A), onErrorContainer = Color(0xFFFFDAD6),
)

private fun KawaiiPalette.canvas(isDark: Boolean): Canvas = when (this) {
    KawaiiPalette.Obsidian -> if (isDark) ObsidianDark else ObsidianLight
    KawaiiPalette.Mica -> if (isDark) MicaDark else MicaLight
    KawaiiPalette.Ember -> if (isDark) EmberDark else EmberLight
    KawaiiPalette.Jade -> if (isDark) JadeDark else JadeLight
    KawaiiPalette.SakuraVN -> if (isDark) SakuraVNDark else SakuraVNLight
    KawaiiPalette.Snow -> if (isDark) SnowDark else SnowLight
    KawaiiPalette.Moonlit -> if (isDark) MoonlitDark else MoonlitLight
    else -> if (isDark) KawaiiDark else KawaiiLight
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
    // Signature accent, kept as an extra token for later phases (glow / glass highlights).
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

private val ObsidianDarkAccents = Accents(
    primary = Color(0xFF6FB4FF), onPrimary = Color(0xFF00335C),
    primaryContainer = Color(0xFF14487D), onPrimaryContainer = Color(0xFFD3E4FF),
    inversePrimary = Color(0xFF1B5FA6),
    secondary = Color(0xFF9FC9E8), onSecondary = Color(0xFF063045),
    secondaryContainer = Color(0xFF24485E), onSecondaryContainer = Color(0xFFCDE6FA),
    tertiary = Color(0xFFB9A8FF), onTertiary = Color(0xFF2A1B58),
    tertiaryContainer = Color(0xFF423178), onTertiaryContainer = Color(0xFFE6DDFF),
    neon = Color(0xFF58F5FF),
)
private val ObsidianLightAccents = Accents(
    primary = Color(0xFF0A5DB0), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C3A),
    inversePrimary = Color(0xFF6FB4FF),
    secondary = Color(0xFF33617D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE6FA), onSecondaryContainer = Color(0xFF001E30),
    tertiary = Color(0xFF5B429B), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6DDFF), onTertiaryContainer = Color(0xFF180050),
    neon = Color(0xFF007C86),
)

private val MicaDarkAccents = Accents(
    primary = Color(0xFFA7C8DE), onPrimary = Color(0xFF10303F),
    primaryContainer = Color(0xFF2A4756), onPrimaryContainer = Color(0xFFC6E7FD),
    inversePrimary = Color(0xFF3E6376),
    secondary = Color(0xFFB8C4CC), onSecondary = Color(0xFF232C31),
    secondaryContainer = Color(0xFF3A434A), onSecondaryContainer = Color(0xFFD4E0E8),
    tertiary = Color(0xFFC6BFD0), onTertiary = Color(0xFF2D2836),
    tertiaryContainer = Color(0xFF443F4E), onTertiaryContainer = Color(0xFFE3DBEC),
    neon = Color(0xFF8FD3E8),
)
private val MicaLightAccents = Accents(
    primary = Color(0xFF38617A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6E7FD), onPrimaryContainer = Color(0xFF001E30),
    inversePrimary = Color(0xFFA7C8DE),
    secondary = Color(0xFF4F5B62), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E0E8), onSecondaryContainer = Color(0xFF0B1E26),
    tertiary = Color(0xFF5C596A), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3DBEC), onTertiaryContainer = Color(0xFF191626),
    neon = Color(0xFF2F94B0),
)

private val EmberDarkAccents = Accents(
    primary = Color(0xFFFFB08A), onPrimary = Color(0xFF4E2200),
    primaryContainer = Color(0xFF6E3A18), onPrimaryContainer = Color(0xFFFFDBC8),
    inversePrimary = Color(0xFF9C4E20),
    secondary = Color(0xFFF2C57C), onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF614017), onSecondaryContainer = Color(0xFFFFDFA6),
    tertiary = Color(0xFFE7A0A0), onTertiary = Color(0xFF4C1F22),
    tertiaryContainer = Color(0xFF683B3C), onTertiaryContainer = Color(0xFFFFD9D9),
    neon = Color(0xFFFFD166),
)
private val EmberLightAccents = Accents(
    primary = Color(0xFFA2521E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8), onPrimaryContainer = Color(0xFF351200),
    inversePrimary = Color(0xFFFFB08A),
    secondary = Color(0xFF7C5A18), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDFA6), onSecondaryContainer = Color(0xFF271900),
    tertiary = Color(0xFF9C4247), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDADB), onTertiaryContainer = Color(0xFF400009),
    neon = Color(0xFFB4870A),
)

private val JadeDarkAccents = Accents(
    primary = Color(0xFF62D6A2), onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF12513A), onPrimaryContainer = Color(0xFF86F4C0),
    inversePrimary = Color(0xFF1E8C63),
    secondary = Color(0xFF78D0CC), onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF1E4E4B), onSecondaryContainer = Color(0xFF96EDE8),
    tertiary = Color(0xFFC7D687), onTertiary = Color(0xFF2A3400),
    tertiaryContainer = Color(0xFF3F4C12), onTertiaryContainer = Color(0xFFE3F2A0),
    neon = Color(0xFF6BFFC0),
)
private val JadeLightAccents = Accents(
    primary = Color(0xFF00764F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF86F4C0), onPrimaryContainer = Color(0xFF002115),
    inversePrimary = Color(0xFF62D6A2),
    secondary = Color(0xFF006A66), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF96EDE8), onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF4F631A), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3F2A0), onTertiaryContainer = Color(0xFF151F00),
    neon = Color(0xFF00A97A),
)

private val SakuraVNLightAccents = Accents(
    primary = Color(0xFFC8547A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E3), onPrimaryContainer = Color(0xFF3E0A1E),
    inversePrimary = Color(0xFFFFB0C8),
    secondary = Color(0xFF9C5A72), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E5), onSecondaryContainer = Color(0xFF3A0A20),
    tertiary = Color(0xFF8A6A3E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEB0), onTertiaryContainer = Color(0xFF2C1A00),
    neon = Color(0xFFFF8FB0),
)
private val SakuraVNDarkAccents = Accents(
    primary = Color(0xFFFFB0C8), onPrimary = Color(0xFF5A1730),
    primaryContainer = Color(0xFF7A3350), onPrimaryContainer = Color(0xFFFFD9E3),
    inversePrimary = Color(0xFFC8547A),
    secondary = Color(0xFFF0B4C8), onSecondary = Color(0xFF4A1F30),
    secondaryContainer = Color(0xFF663246), onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = Color(0xFFE8C08A), onTertiary = Color(0xFF432C08),
    tertiaryContainer = Color(0xFF5F441F), onTertiaryContainer = Color(0xFFFFDEB0),
    neon = Color(0xFFFF8FB0),
)

private val SnowLightAccents = Accents(
    primary = Color(0xFF3E6491), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FB), onPrimaryContainer = Color(0xFF001C38),
    inversePrimary = Color(0xFFA6C8F0),
    secondary = Color(0xFF50627A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7), onSecondaryContainer = Color(0xFF0D1D2F),
    tertiary = Color(0xFF9A6A3C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCB8), onTertiaryContainer = Color(0xFF301D00),
    neon = Color(0xFFA6C8F0),
)
private val SnowDarkAccents = Accents(
    primary = Color(0xFFA6C8F0), onPrimary = Color(0xFF0A3054),
    primaryContainer = Color(0xFF264872), onPrimaryContainer = Color(0xFFD4E3FB),
    inversePrimary = Color(0xFF3E6491),
    secondary = Color(0xFFB6C6DE), onSecondary = Color(0xFF223244),
    secondaryContainer = Color(0xFF394A5C), onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFECBC86), onTertiary = Color(0xFF48290A),
    tertiaryContainer = Color(0xFF633F1C), onTertiaryContainer = Color(0xFFFFDCB8),
    neon = Color(0xFFC7DBF5),
)

private val MoonlitLightAccents = Accents(
    primary = Color(0xFF3F5C9E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE4FF), onPrimaryContainer = Color(0xFF001945),
    inversePrimary = Color(0xFFAEC2FF),
    secondary = Color(0xFF505A76), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E1FF), onSecondaryContainer = Color(0xFF0C1A31),
    tertiary = Color(0xFF624B87), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9DDFF), onTertiaryContainer = Color(0xFF1E0C40),
    neon = Color(0xFF5A78C0),
)
private val MoonlitDarkAccents = Accents(
    primary = Color(0xFFAEC2FF), onPrimary = Color(0xFF122A55),
    primaryContainer = Color(0xFF2C4172), onPrimaryContainer = Color(0xFFDBE4FF),
    inversePrimary = Color(0xFF3F5C9E),
    secondary = Color(0xFFB8C4E8), onSecondary = Color(0xFF232C44),
    secondaryContainer = Color(0xFF39425C), onSecondaryContainer = Color(0xFFD8E1FF),
    tertiary = Color(0xFFCBB4E8), onTertiary = Color(0xFF32204A),
    tertiaryContainer = Color(0xFF483863), onTertiaryContainer = Color(0xFFE9DDFF),
    neon = Color(0xFF9FB8FF),
)

private fun KawaiiPalette.accents(isDark: Boolean): Accents = when (this) {
    KawaiiPalette.Sakura -> if (isDark) SakuraDark else SakuraLight
    KawaiiPalette.SakuraVN -> if (isDark) SakuraVNDarkAccents else SakuraVNLightAccents
    KawaiiPalette.Snow -> if (isDark) SnowDarkAccents else SnowLightAccents
    KawaiiPalette.Moonlit -> if (isDark) MoonlitDarkAccents else MoonlitLightAccents
    KawaiiPalette.Mint -> if (isDark) MintDark else MintLight
    KawaiiPalette.Lavender -> if (isDark) LavenderDark else LavenderLight
    KawaiiPalette.Cyber -> if (isDark) CyberDark else CyberLight
    KawaiiPalette.Obsidian -> if (isDark) ObsidianDarkAccents else ObsidianLightAccents
    KawaiiPalette.Mica -> if (isDark) MicaDarkAccents else MicaLightAccents
    KawaiiPalette.Ember -> if (isDark) EmberDarkAccents else EmberLightAccents
    KawaiiPalette.Jade -> if (isDark) JadeDarkAccents else JadeLightAccents
    KawaiiPalette.None -> if (isDark) SakuraDark else SakuraLight
}

// The signature accent for the active preset/mode (exposed for later glow/glass work).
fun KawaiiPalette.neonAccent(isDark: Boolean): Color = accents(isDark).neon

// --- Material3 color scheme ----------------------------------------------------------------

fun KawaiiPalette.materialColorScheme(isDark: Boolean): ColorScheme {
    val a = accents(isDark)
    val c = canvas(isDark)
    return if (isDark) {
        darkColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            inversePrimary = a.inversePrimary,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiary = a.tertiary, onTertiary = a.onTertiary,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            surfaceContainerLowest = c.surfaceContainerLowest,
            surfaceContainerLow = c.surfaceContainerLow,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.surfaceContainerHigh,
            surfaceContainerHighest = c.surfaceContainerHighest,
            surfaceBright = c.surfaceBright, surfaceDim = c.surfaceDim,
            outline = c.outline, outlineVariant = c.outlineVariant,
            inverseSurface = c.inverseSurface, inverseOnSurface = c.inverseOnSurface,
            error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
            scrim = c.scrim,
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
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            surfaceContainerLowest = c.surfaceContainerLowest,
            surfaceContainerLow = c.surfaceContainerLow,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.surfaceContainerHigh,
            surfaceContainerHighest = c.surfaceContainerHighest,
            surfaceBright = c.surfaceBright, surfaceDim = c.surfaceDim,
            outline = c.outline, outlineVariant = c.outlineVariant,
            inverseSurface = c.inverseSurface, inverseOnSurface = c.inverseOnSurface,
            error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
            scrim = c.scrim,
        )
    }
}

// --- Miuix color scheme --------------------------------------------------------------------

fun KawaiiPalette.miuixColorScheme(isDark: Boolean): MiuixColors {
    val a = accents(isDark)
    val c = canvas(isDark)
    return if (isDark) {
        miuixDarkColorScheme(
            primary = a.primary, onPrimary = a.onPrimary,
            primaryVariant = a.primaryContainer, onPrimaryVariant = a.onPrimaryContainer,
            primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
            secondary = a.secondary, onSecondary = a.onSecondary,
            secondaryVariant = a.secondaryContainer, onSecondaryVariant = a.onSecondaryContainer,
            secondaryContainer = a.secondaryContainer, onSecondaryContainer = a.onSecondaryContainer,
            tertiaryContainer = a.tertiaryContainer, onTertiaryContainer = a.onTertiaryContainer,
            background = c.background, onBackground = c.onBackground,
            onBackgroundVariant = c.onBackgroundVariant,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant,
            onSurfaceSecondary = c.onSurfaceVariant,
            onSurfaceVariantSummary = c.onSurfaceVariant,
            onSurfaceVariantActions = c.onSurfaceVariant,
            surfaceContainer = c.surfaceContainer,
            onSurfaceContainer = c.onSurface,
            onSurfaceContainerVariant = c.onSurfaceVariant,
            surfaceContainerHigh = c.surfaceContainerHigh,
            onSurfaceContainerHigh = c.onSurface,
            surfaceContainerHighest = c.surfaceContainerHighest,
            onSurfaceContainerHighest = c.onSurface,
            outline = c.outline, dividerLine = c.outlineVariant,
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
            background = c.background, onBackground = c.onBackground,
            onBackgroundVariant = c.onBackgroundVariant,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant,
            onSurfaceSecondary = c.onSurfaceVariant,
            onSurfaceVariantSummary = c.onSurfaceVariant,
            onSurfaceVariantActions = c.onSurfaceVariant,
            surfaceContainer = c.surfaceContainer,
            onSurfaceContainer = c.onSurface,
            onSurfaceContainerVariant = c.onSurfaceVariant,
            surfaceContainerHigh = c.surfaceContainerHigh,
            onSurfaceContainerHigh = c.onSurface,
            surfaceContainerHighest = c.surfaceContainerHighest,
            onSurfaceContainerHighest = c.onSurface,
            outline = c.outline, dividerLine = c.outlineVariant,
        )
    }
}
