package com.xinsu.moe.ui.theme

import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeTemplateTest {
    @Test
    fun `artwork templates do not expand the legacy palette enum`() {
        assertEquals(12, legacySelectableKawaiiPalettes.size)
        assertEquals(12, KawaiiPalette.entries.size)
    }

    @Test
    fun `all artwork templates resolve one renderer neutral bundle each`() {
        val templates = BuiltInThemes.all.filter { it.tokenBundleId != null }

        assertEquals(21, templates.size)
        templates.forEach { template ->
            val bundle = BuiltInThemeCatalog.byId(template.tokenBundleId)
            assertNotNull(bundle, template.id)
            assertEquals(KawaiiPalette.None, template.palette)
            assertEquals(bundle.id, template.id)
            assertEquals(BackgroundStyle.Stage, template.recommendedBackground)
            assertNotNull(template.artworkRes, "${template.id} artwork")
            assertNotNull(template.summaryRes, "${template.id} summary")
            assertTrue(template.galleryMood != ThemeGalleryMood.Legacy, template.id)
        }
    }

    @Test
    fun `application plan carries stable artwork and atmosphere recommendations`() {
        val moon = BuiltInThemes.byId("moonlit-silver-blue")!!.toApplicationPlan()
        val sakura = BuiltInThemes.byId("sakura-street-walk")!!.toApplicationPlan()
        val sea = BuiltInThemes.byId("sea-breeze-song")!!.toApplicationPlan()

        assertEquals("moonlit-silver-blue", moon.themePreset)
        assertEquals("theme_moonlit_silver_blue.jpg", moon.artworkResourceName)
        assertEquals(68, moon.cardOpacity)
        assertEquals(0, moon.backgroundImageAlign)
        assertEquals(2, moon.preferredColorMode)
        assertEquals(2, sakura.backgroundImageAlign)
        assertEquals(1, sea.backgroundImageAlign)
    }

    @Test
    fun `legacy templates deliberately keep the compatibility resolver`() {
        assertNull(BuiltInThemes.byId("Moonlit")?.tokenBundleId)
        assertNull(BuiltInThemes.byId("SakuraVN")?.tokenBundleId)
        assertEquals(12, legacySelectableKawaiiPalettes.size)
        assertEquals(KawaiiPalette.None, legacySelectableKawaiiPalettes.first())
        assertEquals(KawaiiPalette.Moonlit, legacySelectableKawaiiPalettes.last())
    }
}
