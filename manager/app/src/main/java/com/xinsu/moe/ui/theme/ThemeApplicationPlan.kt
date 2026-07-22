package com.xinsu.moe.ui.theme

import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import kotlin.math.roundToInt

data class ThemeApplicationPlan(
    val themePreset: String,
    val backgroundStyle: String,
    val preferredColorMode: Int?,
    val artworkResourceName: String?,
    val backgroundImageAlpha: Int?,
    val backgroundImageAlign: Int?,
    val cardOpacity: Int?,
)

fun ThemeTemplate.toApplicationPlan(): ThemeApplicationPlan {
    val bundle = tokenBundleId?.let(BuiltInThemeCatalog::byId)
    val focalX = bundle?.artwork?.focalPoint?.x
    return ThemeApplicationPlan(
        themePreset = id,
        backgroundStyle = recommendedBackground.name,
        preferredColorMode = preferredColorMode,
        artworkResourceName = bundle?.artwork?.resourceName,
        backgroundImageAlpha = bundle?.let { 100 },
        backgroundImageAlign = focalX?.let {
            when {
                it < 0.4f -> 0
                it > 0.6f -> 2
                else -> 1
            }
        },
        cardOpacity = bundle?.atmosphere?.cardOpacity?.times(100)?.roundToInt(),
    )
}
