package com.xinsu.moe.ui.theme.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuiltInThemeCatalogTest {
    @Test
    fun `phase B catalog covers every checked in artwork exactly once`() {
        val themes = BuiltInThemeCatalog.all

        assertEquals(21, themes.size)
        assertEquals(21, themes.map { it.id }.toSet().size)
        assertEquals(21, themes.map { it.artwork.resourceName }.toSet().size)
        assertEquals(
            setOf(
                "ink-white-companions",
                "twin-peach-heartstrings",
                "winter-blue-scarf",
                "dusk-iron-wind",
                "cloud-slope-stars",
                "sakura-street-walk",
                "sakura-crown-overture",
                "golden-eye-cat-courtyard",
                "mint-pull",
                "ink-order-poster",
                "moonlit-silver-blue",
                "cobalt-night-dress",
                "clear-sky-blue-ribbon",
                "windfield-doll",
                "crimson-eye-jump",
                "cream-street-corner",
                "black-rose-stone-court",
                "sea-breeze-song",
                "pink-mist-night-window",
                "frost-white-crimson-eye",
                "blue-flame-cat-shadow",
            ),
            themes.map { it.id }.toSet(),
        )
    }

    @Test
    fun `every artwork theme passes light and dark contrast contracts`() {
        BuiltInThemeCatalog.all.forEach { theme ->
            assertTrue(theme.light.contrastResults().all { it.passes }, "${theme.id} light")
            assertTrue(theme.dark.contrastResults().all { it.passes }, "${theme.id} dark")
            assertEquals(setOf("primary", "secondary", "focus", "surface"), theme.provenance.roleNotes.keys)
        }
    }

    @Test
    fun `unknown theme id deliberately falls back outside the catalog`() {
        assertNull(BuiltInThemeCatalog.byId("not-a-theme"))
        assertNull(BuiltInThemeCatalog.byId(null))
    }
}
