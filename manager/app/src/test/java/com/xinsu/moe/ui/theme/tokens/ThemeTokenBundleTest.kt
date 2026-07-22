package com.xinsu.moe.ui.theme.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ThemeTokenBundleTest {
    @Test
    fun `argb round trips without signed Int truncation`() {
        val color = ThemeColorMath.argb(alpha = 0x80, red = 0x12, green = 0x34, blue = 0x56)

        assertEquals(0x80123456L, color)
        assertEquals(0x80, ThemeColorMath.alpha(color))
        assertEquals(0x12, ThemeColorMath.red(color))
        assertEquals(0x34, ThemeColorMath.green(color))
        assertEquals(0x56, ThemeColorMath.blue(color))
    }

    @Test
    fun `contrast utility matches WCAG black white boundary`() {
        assertEquals(21.0, ThemeColorMath.contrastRatio(WHITE, BLACK), absoluteTolerance = 0.0001)
        assertTrue(ThemeColorMath.contrastRatio(0xFF777777L, WHITE) < 4.5)
        assertTrue(ThemeColorMath.contrastRatio(0xFF767676L, WHITE) >= 4.5)
    }

    @Test
    fun `semantic foreground colors must stay opaque`() {
        assertFailsWith<IllegalArgumentException> {
            readableTokens().copy(onPrimary = 0xCCFFFFFFL)
        }
    }

    @Test
    fun `normalized artwork rejects an inverted safe rectangle`() {
        assertFailsWith<IllegalArgumentException> {
            ThemeArtworkTokens(
                resourceName = "theme_test.jpg",
                width = 1080,
                height = 1920,
                focalPoint = NormalizedPoint(0.5f, 0.4f),
                subjectSafeRect = NormalizedRect(0.8f, 0.1f, 0.2f, 0.9f),
                scaleMode = ArtworkScaleMode.Crop,
            )
        }
    }

    @Test
    fun `bundle validates all documented contrast pairs`() {
        val bundle = sampleBundle()

        val results = bundle.light.contrastResults()
        assertEquals(12, results.size)
        assertTrue(results.all { it.ratio >= it.minimumRatio }, results.toString())
        assertEquals("moonlit-silver-blue", bundle.id)
    }

    @Test
    fun `material and miuix share the same semantic role values`() {
        val bundle = sampleBundle()
        val material = bundle.toMaterialColorScheme(isDark = true)
        val miuix = bundle.toMiuixColorScheme(isDark = true)

        assertEquals(material.primary, miuix.primary)
        assertEquals(material.onPrimary, miuix.onPrimary)
        assertEquals(material.primaryContainer, miuix.primaryContainer)
        assertEquals(material.secondary, miuix.secondary)
        assertEquals(material.surface, miuix.surface)
        assertEquals(material.onSurface, miuix.onSurface)
        assertEquals(material.outline, miuix.outline)
    }

    private fun sampleBundle() = ThemeTokenBundle(
        id = "moonlit-silver-blue",
        displayNameKey = "theme_moonlit_silver_blue",
        artwork = ThemeArtworkTokens(
            resourceName = "theme_moonlit_silver_blue.jpg",
            width = 1920,
            height = 1080,
            focalPoint = NormalizedPoint(0.36f, 0.46f),
            subjectSafeRect = NormalizedRect(0.08f, 0.08f, 0.58f, 0.96f),
            scaleMode = ArtworkScaleMode.Crop,
            scrimRails = listOf(
                ScrimRail(ScrimEdge.Bottom, 0.72f, 1f, 0.34f),
            ),
        ),
        light = readableTokens(),
        dark = readableTokens(),
        atmosphere = AtmosphereTokens(
            cardOpacity = 0.72f,
            chromeOpacity = 0.58f,
            readableOpacity = 0.9f,
            outlineOpacity = 0.32f,
            glowColor = 0xFF9FB8FFL,
            glowIntensity = 0.42f,
            localScrimStrength = 0.36f,
            cornerRadiusDp = 24f,
            blurRadiusDp = 30f,
            density = ThemeDensity.Comfortable,
            preferredMode = ThemeModePreference.Dark,
        ),
        provenance = ThemeProvenance(
            sourceFilename = "3C79AF894DA8613CACE6062AF1F9BCB4.jpg",
            sha256 = "2be9b5f89278f1fee3b5234d094b5b9688f5b2027dad96f27576de0b99dc43a8",
            rightsStatus = ThemeRightsStatus.UserProvidedPendingPublicationConfirmation,
            roleNotes = mapOf(
                "primary" to "moonlight blue",
                "secondary" to "violet eyes",
                "focus" to "moon rim",
                "surface" to "indigo uniform and night sky",
            ),
            pipelineVersion = "1.0.0",
            contrastValidated = true,
        ),
    )

    private fun readableTokens() = SemanticColorTokens(
        primary = BLACK,
        onPrimary = WHITE,
        primaryContainer = WHITE,
        onPrimaryContainer = BLACK,
        inversePrimary = WHITE,
        secondary = BLACK,
        onSecondary = WHITE,
        secondaryContainer = WHITE,
        onSecondaryContainer = BLACK,
        tertiary = BLACK,
        onTertiary = WHITE,
        tertiaryContainer = WHITE,
        onTertiaryContainer = BLACK,
        background = WHITE,
        onBackground = BLACK,
        surface = WHITE,
        onSurface = BLACK,
        surfaceVariant = WHITE,
        onSurfaceVariant = BLACK,
        surfaceContainerLowest = WHITE,
        surfaceContainerLow = WHITE,
        surfaceContainer = WHITE,
        surfaceContainerHigh = WHITE,
        surfaceContainerHighest = WHITE,
        surfaceBright = WHITE,
        surfaceDim = WHITE,
        outline = BLACK,
        outlineVariant = BLACK,
        inverseSurface = BLACK,
        inverseOnSurface = WHITE,
        error = BLACK,
        onError = WHITE,
        errorContainer = WHITE,
        onErrorContainer = BLACK,
        scrim = BLACK,
        focus = BLACK,
    )

    private companion object {
        const val WHITE = 0xFFFFFFFFL
        const val BLACK = 0xFF000000L
    }
}
