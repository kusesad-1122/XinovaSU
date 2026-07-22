package com.xinsu.moe.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.rememberDynamicColorScheme
import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import com.xinsu.moe.ui.theme.tokens.toMaterialColorScheme
import com.xinsu.moe.ui.webui.MonetColorsProvider

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialXinovaSUTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = appSettings.colorMode.isAmoled
    val dynamicColor = appSettings.keyColor == 0
    val colorStyle = appSettings.paletteStyle
    val colorSpec = appSettings.colorSpec
    val glassSurfacesActive = LocalBackgroundStyle.current.isActive
    val surfaceOpacity = LocalCardOpacity.current / 100f
    val tokenBundle = BuiltInThemes.byId(appSettings.themePresetId)
        ?.tokenBundleId
        ?.let(BuiltInThemeCatalog::byId)

    val colorScheme = if (tokenBundle != null) {
        tokenBundle.toMaterialColorScheme(isDark = darkTheme)
    } else if (appSettings.themePreset.isActive) {
        appSettings.themePreset.materialColorScheme(isDark = darkTheme)
    } else if (dynamicColor) {
        val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = darkTheme,
            isAmoled = amoledMode,
            style = colorStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(appSettings.keyColor),
            isDark = darkTheme,
            isAmoled = amoledMode,
            style = colorStyle,
            specVersion = colorSpec,
        )
    }

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme.applyGlassSurfaces(
            active = glassSurfacesActive,
            opacity = surfaceOpacity,
        ),
        motionScheme = MotionScheme.expressive(),
        content = {
            MonetColorsProvider.UpdateCss()
            content()
        }
    )
}
