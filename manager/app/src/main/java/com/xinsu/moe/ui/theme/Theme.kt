package com.xinsu.moe.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.xinsu.moe.ui.LocalUiMode
import com.xinsu.moe.ui.UiMode
import com.xinsu.moe.ui.screen.home.HomeCardId
import com.xinsu.moe.ui.screen.home.HomeCardShape
import com.xinsu.moe.ui.theme.tokens.ThemeTokenBundle

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK, DARK_AMOLED -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    /** Stable template id stored in preferences; artwork themes are not palette enum values. */
    val themePresetId: String = KawaiiPalette.None.name,
) {
    /** Compatibility palette used only by the original hand-authored color presets. */
    val themePreset: KawaiiPalette get() = KawaiiPalette.fromName(themePresetId)
}

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uiMode = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        var colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)

        if (uiMode == "miuix") {
            val miuixMonet = prefs.getBoolean("miuix_monet", false)
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = prefs.getInt("key_color", 0)
        val paletteStyleStr = prefs.getString("color_style", PaletteStyle.TonalSpot.name)
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr!!)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name)
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr!!)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.Default
        }
        val themePresetId = prefs.getString("theme_preset", KawaiiPalette.None.name)
            ?: KawaiiPalette.None.name

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec, themePresetId)
    }
}

@Composable
fun XinovaSUTheme(
    appSettings: AppSettings? = null,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)

    when (uiMode) {
        UiMode.Miuix -> MiuixXinovaSUTheme(
            appSettings = currentAppSettings,
            content = content
        )

        UiMode.Material -> MaterialXinovaSUTheme(
            appSettings = currentAppSettings,
            content = content
        )
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}


val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalThemePreset = staticCompositionLocalOf { KawaiiPalette.None }

val LocalThemeTokenBundle = staticCompositionLocalOf<ThemeTokenBundle?> { null }

val LocalBackgroundStyle = staticCompositionLocalOf { BackgroundStyle.None }

val LocalBackgroundImageUri = staticCompositionLocalOf { "" }

// Image alpha is a 0-100 percentage; alignment is 0=start, 1=center, 2=end.
val LocalBackgroundImageAlpha = staticCompositionLocalOf { 100 }

val LocalBackgroundImageAlign = staticCompositionLocalOf { 1 }

val LocalCardImageUri = staticCompositionLocalOf { "" }

val LocalCardImageAlpha = staticCompositionLocalOf { 100 }

val LocalCardImageAlign = staticCompositionLocalOf { 1 }

// 0-100 card container opacity: lets the app background show through cards for a glass look.
val LocalCardOpacity = staticCompositionLocalOf { 100 }

// Home dashboard card customization: the user-chosen order of the reorderable home cards, and a
// per-card corner-shape override.
val LocalHomeCardOrder = staticCompositionLocalOf { HomeCardId.DEFAULT_ORDER }

val LocalHomeCardShapes = staticCompositionLocalOf<Map<HomeCardId, HomeCardShape>> { emptyMap() }

// Set (per card, in the home render loop) to override the container corner radius of the very next
// card wrapper. Null keeps each renderer's default shape, so non-home cards are unaffected.
val LocalHomeCardCornerRadius = compositionLocalOf<Dp?> { null }

// "Glass cards": a backdrop capturing ONLY the app background layer (never the cards themselves,
// so no feedback), so a card can sample and frost what's behind it. Null when blur is off/unsupported.
val LocalCardBackdrop = compositionLocalOf<LayerBackdrop?> { null }

// App-wide setting: whether the user turned on frosted "glass cards".
val LocalGlassCardsSetting = staticCompositionLocalOf { false }

// Scoped flag the card wrappers actually read — provided true only around the home dashboard cards
// so glass stays off for the rest of the app even when the setting is on.
val LocalGlassCard = compositionLocalOf { false }

val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }
