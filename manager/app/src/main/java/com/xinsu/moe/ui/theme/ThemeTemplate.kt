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
//  Legacy templates still let users supply their own illustration. New token-bundle templates
//  additionally point at a checked-in, provenance-tracked keyart resource.
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
    /** Optional renderer-neutral bundle. Legacy templates continue through [palette]. */
    val tokenBundleId: String? = null,
    /** Compile-time checked artwork used by the theme gallery. */
    val artworkRes: Int? = null,
    /** Short editorial description shown in the large gallery preview. */
    val summaryRes: Int? = null,
    /** Visual family used for gallery browsing, not for colour resolution. */
    val galleryMood: ThemeGalleryMood = ThemeGalleryMood.Legacy,
)

enum class ThemeGalleryMood { Minimal, Playful, Daylight, Cinematic, Nocturne, Legacy }

object BuiltInThemes {
    // Artwork themes are first-class template ids. KawaiiPalette remains a legacy colour fallback.
    private fun token(
        id: String,
        nameRes: Int,
        artworkRes: Int,
        summaryRes: Int,
        preferredColorMode: Int,
        galleryMood: ThemeGalleryMood,
    ) = ThemeTemplate(
        id = id,
        nameRes = nameRes,
        palette = KawaiiPalette.None,
        recommendedBackground = BackgroundStyle.Stage,
        preferredColorMode = preferredColorMode,
        tokenBundleId = id,
        artworkRes = artworkRes,
        summaryRes = summaryRes,
        galleryMood = galleryMood,
    )

    val all: List<ThemeTemplate> = listOf(
        token("ink-white-companions", com.xinsu.moe.R.string.theme_ink_white_companions, com.xinsu.moe.R.drawable.theme_ink_white_companions, com.xinsu.moe.R.string.theme_ink_white_companions_summary, 1, ThemeGalleryMood.Minimal),
        token("twin-peach-heartstrings", com.xinsu.moe.R.string.theme_twin_peach_heartstrings, com.xinsu.moe.R.drawable.theme_twin_peach_heartstrings, com.xinsu.moe.R.string.theme_twin_peach_heartstrings_summary, 1, ThemeGalleryMood.Playful),
        token("winter-blue-scarf", com.xinsu.moe.R.string.theme_winter_blue_scarf, com.xinsu.moe.R.drawable.theme_winter_blue_scarf, com.xinsu.moe.R.string.theme_winter_blue_scarf_summary, 1, ThemeGalleryMood.Daylight),
        token("dusk-iron-wind", com.xinsu.moe.R.string.theme_dusk_iron_wind, com.xinsu.moe.R.drawable.theme_dusk_iron_wind, com.xinsu.moe.R.string.theme_dusk_iron_wind_summary, 2, ThemeGalleryMood.Cinematic),
        token("cloud-slope-stars", com.xinsu.moe.R.string.theme_cloud_slope_stars, com.xinsu.moe.R.drawable.theme_cloud_slope_stars, com.xinsu.moe.R.string.theme_cloud_slope_stars_summary, 1, ThemeGalleryMood.Daylight),
        token("sakura-street-walk", com.xinsu.moe.R.string.theme_sakura_street_walk, com.xinsu.moe.R.drawable.theme_sakura_street_walk, com.xinsu.moe.R.string.theme_sakura_street_walk_summary, 1, ThemeGalleryMood.Daylight),
        token("sakura-crown-overture", com.xinsu.moe.R.string.theme_sakura_crown_overture, com.xinsu.moe.R.drawable.theme_sakura_crown_overture, com.xinsu.moe.R.string.theme_sakura_crown_overture_summary, 1, ThemeGalleryMood.Playful),
        token("golden-eye-cat-courtyard", com.xinsu.moe.R.string.theme_golden_eye_cat_courtyard, com.xinsu.moe.R.drawable.theme_golden_eye_cat_courtyard, com.xinsu.moe.R.string.theme_golden_eye_cat_courtyard_summary, 1, ThemeGalleryMood.Daylight),
        token("mint-pull", com.xinsu.moe.R.string.theme_mint_pull, com.xinsu.moe.R.drawable.theme_mint_pull, com.xinsu.moe.R.string.theme_mint_pull_summary, 1, ThemeGalleryMood.Playful),
        token("ink-order-poster", com.xinsu.moe.R.string.theme_ink_order_poster, com.xinsu.moe.R.drawable.theme_ink_order_poster, com.xinsu.moe.R.string.theme_ink_order_poster_summary, 1, ThemeGalleryMood.Minimal),
        token("moonlit-silver-blue", com.xinsu.moe.R.string.theme_moonlit_silver_blue, com.xinsu.moe.R.drawable.theme_moonlit_silver_blue, com.xinsu.moe.R.string.theme_moonlit_silver_blue_summary, 2, ThemeGalleryMood.Nocturne),
        token("cobalt-night-dress", com.xinsu.moe.R.string.theme_cobalt_night_dress, com.xinsu.moe.R.drawable.theme_cobalt_night_dress, com.xinsu.moe.R.string.theme_cobalt_night_dress_summary, 2, ThemeGalleryMood.Nocturne),
        token("clear-sky-blue-ribbon", com.xinsu.moe.R.string.theme_clear_sky_blue_ribbon, com.xinsu.moe.R.drawable.theme_clear_sky_blue_ribbon, com.xinsu.moe.R.string.theme_clear_sky_blue_ribbon_summary, 1, ThemeGalleryMood.Cinematic),
        token("windfield-doll", com.xinsu.moe.R.string.theme_windfield_doll, com.xinsu.moe.R.drawable.theme_windfield_doll, com.xinsu.moe.R.string.theme_windfield_doll_summary, 2, ThemeGalleryMood.Cinematic),
        token("crimson-eye-jump", com.xinsu.moe.R.string.theme_crimson_eye_jump, com.xinsu.moe.R.drawable.theme_crimson_eye_jump, com.xinsu.moe.R.string.theme_crimson_eye_jump_summary, 1, ThemeGalleryMood.Minimal),
        token("cream-street-corner", com.xinsu.moe.R.string.theme_cream_street_corner, com.xinsu.moe.R.drawable.theme_cream_street_corner, com.xinsu.moe.R.string.theme_cream_street_corner_summary, 1, ThemeGalleryMood.Daylight),
        token("black-rose-stone-court", com.xinsu.moe.R.string.theme_black_rose_stone_court, com.xinsu.moe.R.drawable.theme_black_rose_stone_court, com.xinsu.moe.R.string.theme_black_rose_stone_court_summary, 2, ThemeGalleryMood.Nocturne),
        token("sea-breeze-song", com.xinsu.moe.R.string.theme_sea_breeze_song, com.xinsu.moe.R.drawable.theme_sea_breeze_song, com.xinsu.moe.R.string.theme_sea_breeze_song_summary, 1, ThemeGalleryMood.Daylight),
        token("pink-mist-night-window", com.xinsu.moe.R.string.theme_pink_mist_night_window, com.xinsu.moe.R.drawable.theme_pink_mist_night_window, com.xinsu.moe.R.string.theme_pink_mist_night_window_summary, 2, ThemeGalleryMood.Nocturne),
        token("frost-white-crimson-eye", com.xinsu.moe.R.string.theme_frost_white_crimson_eye, com.xinsu.moe.R.drawable.theme_frost_white_crimson_eye, com.xinsu.moe.R.string.theme_frost_white_crimson_eye_summary, 1, ThemeGalleryMood.Minimal),
        token("blue-flame-cat-shadow", com.xinsu.moe.R.string.theme_blue_flame_cat_shadow, com.xinsu.moe.R.drawable.theme_blue_flame_cat_shadow, com.xinsu.moe.R.string.theme_blue_flame_cat_shadow_summary, 2, ThemeGalleryMood.Playful),
        ThemeTemplate("SakuraVN", com.xinsu.moe.R.string.theme_preset_sakuravn, KawaiiPalette.SakuraVN, BackgroundStyle.Gradient, preferredColorMode = 1),
        ThemeTemplate("Snow", com.xinsu.moe.R.string.theme_preset_snow, KawaiiPalette.Snow, BackgroundStyle.Gradient, preferredColorMode = 1),
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
