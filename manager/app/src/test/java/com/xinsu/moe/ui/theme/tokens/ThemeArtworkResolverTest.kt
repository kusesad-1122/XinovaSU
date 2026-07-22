package com.xinsu.moe.ui.theme.tokens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeArtworkResolverTest {
    @Test
    fun `every artwork covers phone viewport and preserves critical subject region`() {
        BuiltInThemeCatalog.all.forEach { theme ->
            val placement = ThemeArtworkResolver.resolve(
                artwork = theme.artwork,
                viewportWidth = 390f,
                viewportHeight = 844f,
            )

            assertTrue(placement.coversViewport, theme.id)
            assertTrue(placement.keepsSafeRectVisible, theme.id)
        }
    }

    @Test
    fun `local scrim rails do not cover any theme safe regions`() {
        BuiltInThemeCatalog.all.forEach { theme ->
            val placement = ThemeArtworkResolver.resolve(theme.artwork, 390f, 844f)
            theme.artwork.scrimRails.forEach { rail ->
                val scrim = ThemeArtworkResolver.scrimRect(rail, 390f, 844f)
                assertFalse(scrim.intersects(placement.safeRect), "${theme.id}: $rail")
            }
        }
    }

    @Test
    fun `crop resolves both landscape and portrait without corner thumbnail behavior`() {
        val landscape = BuiltInThemeCatalog.byId("moonlit-silver-blue")!!.artwork
        val portrait = BuiltInThemeCatalog.byId("sea-breeze-song")!!.artwork

        assertTrue(ThemeArtworkResolver.resolve(landscape, 390f, 844f).coversViewport)
        assertTrue(ThemeArtworkResolver.resolve(portrait, 390f, 844f).coversViewport)
    }
}
