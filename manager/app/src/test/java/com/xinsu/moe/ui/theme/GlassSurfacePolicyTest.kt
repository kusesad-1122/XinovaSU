package com.xinsu.moe.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

private const val COLOR_ALPHA_TOLERANCE = 1f / 255f

class GlassSurfacePolicyTest {
    @Test
    fun materialInactiveReturnsOriginalScheme() {
        val original = lightColorScheme(surface = Color.Red)

        assertEquals(original, original.applyGlassSurfaces(active = false, opacity = 0.4f))
    }

    @Test
    fun materialActiveMakesRootTransparentAndScalesOnlySurfaces() {
        val original = lightColorScheme(
            background = Color.White,
            surface = Color(0xFF102030),
            surfaceContainer = Color(0xFF405060),
            onSurface = Color(0xFFEEEEEE),
        )

        val result = original.applyGlassSurfaces(active = true, opacity = 0.5f)

        assertEquals(Color.Transparent, result.background)
        assertEquals(0.5f, result.surface.alpha, COLOR_ALPHA_TOLERANCE)
        assertEquals(0.5f, result.surfaceContainer.alpha, COLOR_ALPHA_TOLERANCE)
        assertEquals(1f, result.onSurface.alpha, COLOR_ALPHA_TOLERANCE)
    }

    @Test
    fun miuixActiveMakesRootTransparentAndKeepsContentOpaque() {
        val original = miuixLightColorScheme(
            background = Color.White,
            surface = Color(0xFF102030),
            surfaceContainer = Color(0xFF405060),
            onSurface = Color(0xFFEEEEEE),
        )

        val result = original.applyGlassSurfaces(active = true, opacity = 0.35f)

        assertEquals(Color.Transparent, result.background)
        assertEquals(0.35f, result.surface.alpha, COLOR_ALPHA_TOLERANCE)
        assertEquals(0.35f, result.surfaceContainer.alpha, COLOR_ALPHA_TOLERANCE)
        assertEquals(1f, result.onSurface.alpha, COLOR_ALPHA_TOLERANCE)
    }

    @Test
    fun opacityIsClampedAndMultipliesExistingAlpha() {
        assertEquals(
            0.25f,
            Color.Red.copy(alpha = 0.5f).multiplyAlpha(0.5f).alpha,
            COLOR_ALPHA_TOLERANCE,
        )
        assertEquals(0f, Color.Red.multiplyAlpha(-1f).alpha, COLOR_ALPHA_TOLERANCE)
        assertEquals(1f, Color.Red.multiplyAlpha(2f).alpha, COLOR_ALPHA_TOLERANCE)
    }
}
