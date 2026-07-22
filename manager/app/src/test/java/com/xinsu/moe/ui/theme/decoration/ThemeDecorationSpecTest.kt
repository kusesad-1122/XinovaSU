package com.xinsu.moe.ui.theme.decoration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThemeDecorationSpecTest {
    @Test
    fun `card roles are stable and complete`() {
        assertEquals(
            listOf("Hero", "Monitor", "Function", "Standard", "Compact"),
            DecoratedCardRole.entries.map { it.name },
        )
    }

    @Test
    fun `all dedicated motifs exist`() {
        assertEquals(
            listOf(
                "InkPanels", "HeartRibbon", "FrostScarf", "IronWind", "CloudSlope", "SakuraStreet",
                "SakuraCrown", "CatCourtyard", "MintPull", "InkPoster", "SilverMoon", "CobaltDress",
                "SkyRibbon", "Windfield", "CrimsonFocus", "CreamStreet", "BlackRose", "SeaBreeze",
                "PinkMist", "FrostCrimson", "BlueFlameCat", "VisualNovel", "SnowWindow", "MoonOrbit",
                "SakuraFan", "MintVine", "LavenderCrystal", "CyberCircuit", "ObsidianFacet",
                "MicaLayer", "EmberForge", "JadeCloud",
            ),
            OrnamentMotif.entries.map { it.name },
        )
    }

    @Test
    fun `accent lists are snapshotted at construction`() {
        val topLevelAccents = mutableListOf(AccentRole.Primary, AccentRole.Outline)
        val energyAccents = mutableListOf(AccentRole.Primary, AccentRole.SurfaceGlow)
        val spec = sampleSpec(
            topLevelAccents = topLevelAccents,
            energyAccents = energyAccents,
        )

        topLevelAccents += AccentRole.Inverse
        energyAccents.clear()

        assertEquals(listOf(AccentRole.Primary, AccentRole.Outline), spec.accents)
        assertEquals(listOf(AccentRole.Primary, AccentRole.SurfaceGlow), spec.energy.accents)
    }

    @Test
    fun `structural fingerprint excludes colors but includes geometry`() {
        val base = sampleSpec()
        val recolored = base.copy(
            themeId = "renamed",
            accents = base.accents.reversed(),
            frame = base.frame.copy(accent = AccentRole.Inverse),
            iconPedestal = base.iconPedestal.copy(accent = AccentRole.SurfaceGlow),
            energy = base.energy.copy(accents = listOf(AccentRole.Tertiary)),
        )
        assertEquals(
            base.structuralFingerprint(),
            recolored.structuralFingerprint(),
        )
        val structuralVariants = listOf(
            base.copy(frame = base.frame.copy(lineCount = base.frame.lineCount + 1)),
            base.copy(iconPedestal = base.iconPedestal.copy(ringCount = base.iconPedestal.ringCount + 1)),
            base.copy(ambient = base.ambient.copy(amplitude = base.ambient.amplitude + 0.1f)),
            base.copy(energy = base.energy.copy(direction = EnergyDirection.EndToStart)),
            base.copy(layout = base.layout.copy(safeInsetFraction = 0.2f)),
        )
        structuralVariants.forEach { variant ->
            assertNotEquals(base.structuralFingerprint(), variant.structuralFingerprint())
        }
    }

    private fun sampleSpec(
        topLevelAccents: List<AccentRole> = listOf(AccentRole.Primary, AccentRole.Outline),
        energyAccents: List<AccentRole> = listOf(AccentRole.Primary, AccentRole.SurfaceGlow),
    ) = ThemeDecorationSpec(
        themeId = "ink-white-companions",
        motif = OrnamentMotif.InkPanels,
        frame = FrameRecipe(FrameStyle.PanelSplit, 2, 1, true, AccentRole.Outline),
        iconPedestal = IconPedestalRecipe(PedestalStyle.InkStamp, 1, AccentRole.Primary),
        ambient = AmbientRecipe(AmbientStyle.Halftone, DriftAxis.Horizontal, 0.2f),
        energy = EnergyRecipe(
            EnergyPathStyle.BrushSweep,
            ParticleStyle.InkDot,
            EnergyDirection.StartToEnd,
            10,
            0f,
            energyAccents,
        ),
        layout = CardLayoutRecipe(TitleRailStyle.CaptionStrip, BadgeAnchor.TopEnd, 0.14f),
        accents = topLevelAccents,
    )
}
