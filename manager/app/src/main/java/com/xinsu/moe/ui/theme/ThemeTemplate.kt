package com.xinsu.moe.ui.theme

import androidx.compose.runtime.Immutable

// ============================================================================================
//  The unified "complete theme" model.
//
//  A theme is not just a set of colors — it is a NAMED, one-tap bundle of everything that gives
//  the app its look: a color palette (light + dark) plus the background composition it was
//  designed around. This one shape is reused everywhere:
//    - the built-in themes listed below,
//    - a theme imported from a shared .xnsutheme file (see ThemeBundle),
//    - a theme the user exports.
//  So "how do I build my own theme?" has one answer: author a palette in KawaiiPalette.kt, then
//  add one ThemeTemplate line here pointing at it. That's the whole recipe.
//
//  Images are deliberately NOT part of a template. A theme declares its palette and HOW the
//  background is composed (gradient wash, or a side-anchored character portrait); the actual
//  picture is always whatever the user supplies at runtime. This keeps the app art-free and
//  copyright-safe while still letting anyone drop their own illustration into the look.
// ============================================================================================
@Immutable
data class ThemeTemplate(
    /** Stable id; also the KawaiiPalette enum name, so it round-trips through prefs and bundles. */
    val id: String,
    /** Display-name string resource. */
    val nameRes: Int,
    /** The color set — its light + dark variants live in KawaiiPalette.kt. */
    val palette: KawaiiPalette,
    /** The background this theme is built around (Portrait = the VN character look). */
    val recommendedBackground: BackgroundStyle,
    /**
     * Light/dark the theme is designed for (ColorMode value: 1 = light, 2 = dark), applied on
     * one-tap. null leaves the user's current mode untouched.
     */
    val preferredColorMode: Int? = null,
)

object BuiltInThemes {
    // To add a complete theme: author its colors in KawaiiPalette.kt (a new enum value + its
    // Canvas/Accents), then copy one line below and point it at that palette. Nothing else.
    val all: List<ThemeTemplate> = listOf(
        ThemeTemplate("SakuraVN", com.xinsu.moe.R.string.theme_preset_sakuravn, KawaiiPalette.SakuraVN, BackgroundStyle.Portrait, preferredColorMode = 1),
        ThemeTemplate("Snow", com.xinsu.moe.R.string.theme_preset_snow, KawaiiPalette.Snow, BackgroundStyle.Bloom, preferredColorMode = 1),
        ThemeTemplate("Moonlit", com.xinsu.moe.R.string.theme_preset_moonlit, KawaiiPalette.Moonlit, BackgroundStyle.Stage, preferredColorMode = 2),
        ThemeTemplate("Sakura", com.xinsu.moe.R.string.theme_preset_sakura, KawaiiPalette.Sakura, BackgroundStyle.Gradient),
        ThemeTemplate("Mint", com.xinsu.moe.R.string.theme_preset_mint, KawaiiPalette.Mint, BackgroundStyle.Gradient),
        ThemeTemplate("Lavender", com.xinsu.moe.R.string.theme_preset_lavender, KawaiiPalette.Lavender, BackgroundStyle.Gradient),
        ThemeTemplate("Cyber", com.xinsu.moe.R.string.theme_preset_cyber, KawaiiPalette.Cyber, BackgroundStyle.Gradient),
        ThemeTemplate("Obsidian", com.xinsu.moe.R.string.theme_preset_obsidian, KawaiiPalette.Obsidian, BackgroundStyle.Gradient),
        ThemeTemplate("Mica", com.xinsu.moe.R.string.theme_preset_mica, KawaiiPalette.Mica, BackgroundStyle.Gradient),
        ThemeTemplate("Ember", com.xinsu.moe.R.string.theme_preset_ember, KawaiiPalette.Ember, BackgroundStyle.Gradient),
        ThemeTemplate("Jade", com.xinsu.moe.R.string.theme_preset_jade, KawaiiPalette.Jade, BackgroundStyle.Gradient),
    )

    fun byId(id: String?): ThemeTemplate? = all.firstOrNull { it.id == id }
}
