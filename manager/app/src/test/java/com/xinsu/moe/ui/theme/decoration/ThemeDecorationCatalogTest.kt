package com.xinsu.moe.ui.theme.decoration

import com.xinsu.moe.ui.theme.BuiltInThemes
import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette
import com.xinsu.moe.ui.theme.tokens.BuiltInThemeCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ThemeDecorationCatalogTest {
    @Test
    fun `catalog matches every built in theme exactly once`() {
        assertEquals(BuiltInThemes.all.map { it.id }.toSet(), ThemeDecorationCatalog.all.keys)
        assertEquals(32, ThemeDecorationCatalog.all.size)
    }

    @Test
    fun `every built in theme has its authored structural recipe`() {
        val expected = listOf(
            ExpectedRecipe("ink-white-companions", OrnamentMotif.InkPanels, FrameStyle.PanelSplit, PedestalStyle.InkStamp, AmbientStyle.Halftone, EnergyPathStyle.BrushSweep, ParticleStyle.InkDot, EnergyDirection.StartToEnd, TitleRailStyle.CaptionStrip),
            ExpectedRecipe("twin-peach-heartstrings", OrnamentMotif.HeartRibbon, FrameStyle.DoubleRibbon, PedestalStyle.HeartGem, AmbientStyle.SugarPearl, EnergyPathStyle.TwinConverge, ParticleStyle.HeartSpark, EnergyDirection.OutsideIn, TitleRailStyle.RibbonTab),
            ExpectedRecipe("winter-blue-scarf", OrnamentMotif.FrostScarf, FrameStyle.TartanFrost, PedestalStyle.IceGem, AmbientStyle.FineSnow, EnergyPathStyle.CrystalGrow, ParticleStyle.SnowFlake, EnergyDirection.CenterOut, TitleRailStyle.WovenLabel),
            ExpectedRecipe("dusk-iron-wind", OrnamentMotif.IronWind, FrameStyle.RivetRail, PedestalStyle.BoltPlate, AmbientStyle.WindSparks, EnergyPathStyle.RailCharge, ParticleStyle.EmberSpark, EnergyDirection.StartToEnd, TitleRailStyle.MetalPlate),
            ExpectedRecipe("cloud-slope-stars", OrnamentMotif.CloudSlope, FrameStyle.HorizonArc, PedestalStyle.StarMedallion, AmbientStyle.CloudBand, EnergyPathStyle.RisingArc, ParticleStyle.StarShard, EnergyDirection.BottomToTop, TitleRailStyle.HorizonTag),
            ExpectedRecipe("sakura-street-walk", OrnamentMotif.SakuraStreet, FrameStyle.WoodSakura, PedestalStyle.PetalSeal, AmbientStyle.SunlitPetals, EnergyPathStyle.PetalUpdraft, ParticleStyle.SakuraPetal, EnergyDirection.BottomToTop, TitleRailStyle.WoodPlaque),
            ExpectedRecipe("sakura-crown-overture", OrnamentMotif.SakuraCrown, FrameStyle.CrownFiligree, PedestalStyle.CrownJewel, AmbientStyle.RoyalPetals, EnergyPathStyle.CrownOrbit, ParticleStyle.JewelPetal, EnergyDirection.Clockwise, TitleRailStyle.CrownRibbon),
            ExpectedRecipe("golden-eye-cat-courtyard", OrnamentMotif.CatCourtyard, FrameStyle.FelineCut, PedestalStyle.PawSeal, AmbientStyle.LeafWindow, EnergyPathStyle.PawSteps, ParticleStyle.CatGlint, EnergyDirection.StartToEnd, TitleRailStyle.WhiskerTab),
            ExpectedRecipe("mint-pull", OrnamentMotif.MintPull, FrameStyle.PullCord, PedestalStyle.KnotGem, AmbientStyle.MintLeaves, EnergyPathStyle.ElasticConverge, ParticleStyle.MintLeaf, EnergyDirection.OutsideIn, TitleRailStyle.BowLabel),
            ExpectedRecipe("ink-order-poster", OrnamentMotif.InkPoster, FrameStyle.PosterCrop, PedestalStyle.TypeBlock, AmbientStyle.TornPaper, EnergyPathStyle.SlashCut, ParticleStyle.GlyphShard, EnergyDirection.StartToEnd, TitleRailStyle.PosterBar),
            ExpectedRecipe("moonlit-silver-blue", OrnamentMotif.SilverMoon, FrameStyle.LunarFiligree, PedestalStyle.MoonGem, AmbientStyle.StarDust, EnergyPathStyle.LunarOrbit, ParticleStyle.SilverStar, EnergyDirection.Clockwise, TitleRailStyle.MoonRibbon),
            ExpectedRecipe("cobalt-night-dress", OrnamentMotif.CobaltDress, FrameStyle.NightLace, PedestalStyle.Rosette, AmbientStyle.MoonLeaves, EnergyPathStyle.BeamDrop, ParticleStyle.BlueLeaf, EnergyDirection.TopToBottom, TitleRailStyle.LaceLabel),
            ExpectedRecipe("clear-sky-blue-ribbon", OrnamentMotif.SkyRibbon, FrameStyle.SkyKnot, PedestalStyle.RibbonGem, AmbientStyle.CloudCurl, EnergyPathStyle.RibbonFlip, ParticleStyle.SkySpark, EnergyDirection.StartToEnd, TitleRailStyle.SkyBanner),
            ExpectedRecipe("windfield-doll", OrnamentMotif.Windfield, FrameStyle.WindStitch, PedestalStyle.WindRose, AmbientStyle.GrassWave, EnergyPathStyle.FieldSweep, ParticleStyle.GrassLeaf, EnergyDirection.StartToEnd, TitleRailStyle.FieldTag),
            ExpectedRecipe("crimson-eye-jump", OrnamentMotif.CrimsonFocus, FrameStyle.FocusBracket, PedestalStyle.ReticleGem, AmbientStyle.SpeedLines, EnergyPathStyle.FocusBurst, ParticleStyle.CrimsonShard, EnergyDirection.OutsideIn, TitleRailStyle.CameraStrip),
            ExpectedRecipe("cream-street-corner", OrnamentMotif.CreamStreet, FrameStyle.StreetStamp, PedestalStyle.SunSeal, AmbientStyle.WarmGrid, EnergyPathStyle.LampMarch, ParticleStyle.CreamSpark, EnergyDirection.StartToEnd, TitleRailStyle.StreetLabel),
            ExpectedRecipe("black-rose-stone-court", OrnamentMotif.BlackRose, FrameStyle.ThornMasonry, PedestalStyle.RoseGem, AmbientStyle.StoneCracks, EnergyPathStyle.ThornGrow, ParticleStyle.RosePetal, EnergyDirection.StartToEnd, TitleRailStyle.GothicPlaque),
            ExpectedRecipe("sea-breeze-song", OrnamentMotif.SeaBreeze, FrameStyle.TideLine, PedestalStyle.ShellGem, AmbientStyle.WaterGlint, EnergyPathStyle.TideFill, ParticleStyle.Bubble, EnergyDirection.StartToEnd, TitleRailStyle.SailorRibbon),
            ExpectedRecipe("pink-mist-night-window", OrnamentMotif.PinkMist, FrameStyle.WindowMist, PedestalStyle.MistPearl, AmbientStyle.NightWindow, EnergyPathStyle.MistGather, ParticleStyle.PinkDroplet, EnergyDirection.OutsideIn, TitleRailStyle.WindowLabel),
            ExpectedRecipe("frost-white-crimson-eye", OrnamentMotif.FrostCrimson, FrameStyle.FrostGem, PedestalStyle.CrimsonGem, AmbientStyle.Rime, EnergyPathStyle.CrystalConverge, ParticleStyle.FrostShard, EnergyDirection.OutsideIn, TitleRailStyle.WhiteSeal),
            ExpectedRecipe("blue-flame-cat-shadow", OrnamentMotif.BlueFlameCat, FrameStyle.CatFlame, PedestalStyle.CatSigil, AmbientStyle.FlameHalo, EnergyPathStyle.FlameSpiral, ParticleStyle.BlueFlame, EnergyDirection.CounterClockwise, TitleRailStyle.NeonPaw),
            ExpectedRecipe("SakuraVN", OrnamentMotif.VisualNovel, FrameStyle.DialogFrame, PedestalStyle.ChoiceCursor, AmbientStyle.SakuraOverlay, EnergyPathStyle.ChoiceConfirm, ParticleStyle.PetalCursor, EnergyDirection.StartToEnd, TitleRailStyle.NamePlate),
            ExpectedRecipe("Snow", OrnamentMotif.SnowWindow, FrameStyle.SnowFacet, PedestalStyle.SnowCrystal, AmbientStyle.PowderSnow, EnergyPathStyle.FrostBloom, ParticleStyle.HexSnow, EnergyDirection.CenterOut, TitleRailStyle.FrostTab),
            ExpectedRecipe("Moonlit", OrnamentMotif.MoonOrbit, FrameStyle.OrbitRing, PedestalStyle.OrbitCore, AmbientStyle.Constellation, EnergyPathStyle.PlanetOrbit, ParticleStyle.Meteor, EnergyDirection.Clockwise, TitleRailStyle.OrbitLabel),
            ExpectedRecipe("Sakura", OrnamentMotif.SakuraFan, FrameStyle.PetalGold, PedestalStyle.FanSeal, AmbientStyle.SoftPetals, EnergyPathStyle.PetalFountain, ParticleStyle.GoldPetal, EnergyDirection.BottomToTop, TitleRailStyle.FanRibbon),
            ExpectedRecipe("Mint", OrnamentMotif.MintVine, FrameStyle.BotanicalGlass, PedestalStyle.Dewdrop, AmbientStyle.VineVein, EnergyPathStyle.VineGrow, ParticleStyle.GlowLeaf, EnergyDirection.StartToEnd, TitleRailStyle.LeafLabel),
            ExpectedRecipe("Lavender", OrnamentMotif.LavenderCrystal, FrameStyle.CrystalConstellation, PedestalStyle.Prism, AmbientStyle.LavenderStars, EnergyPathStyle.PrismConnect, ParticleStyle.CrystalShard, EnergyDirection.CenterOut, TitleRailStyle.MirrorTab),
            ExpectedRecipe("Cyber", OrnamentMotif.CyberCircuit, FrameStyle.CircuitScan, PedestalStyle.Chip, AmbientStyle.ScanGrid, EnergyPathStyle.DataPulse, ParticleStyle.DataPacket, EnergyDirection.StartToEnd, TitleRailStyle.HudRail),
            ExpectedRecipe("Obsidian", OrnamentMotif.ObsidianFacet, FrameStyle.ObsidianCrack, PedestalStyle.ObsidianShard, AmbientStyle.DarkRefraction, EnergyPathStyle.CoreFracture, ParticleStyle.SpectrumShard, EnergyDirection.CenterOut, TitleRailStyle.FacetLabel),
            ExpectedRecipe("Mica", OrnamentMotif.MicaLayer, FrameStyle.MicaSheet, PedestalStyle.MicaPearl, AmbientStyle.PearlDust, EnergyPathStyle.LayerSweep, ParticleStyle.IridescentFlake, EnergyDirection.StartToEnd, TitleRailStyle.FilmTab),
            ExpectedRecipe("Ember", OrnamentMotif.EmberForge, FrameStyle.ForgeEdge, PedestalStyle.EmberCore, AmbientStyle.FurnaceAsh, EnergyPathStyle.FuseBurn, ParticleStyle.FireSpark, EnergyDirection.StartToEnd, TitleRailStyle.ForgePlate),
            ExpectedRecipe("Jade", OrnamentMotif.JadeCloud, FrameStyle.JadeSeal, PedestalStyle.JadeMedallion, AmbientStyle.AuspiciousCloud, EnergyPathStyle.QiFlow, ParticleStyle.JadeSpark, EnergyDirection.Clockwise, TitleRailStyle.ScrollLabel),
        )

        expected.forEachIndexed { index, authored ->
            val actual = ThemeDecorationCatalog.all.getValue(authored.id)
            assertEquals(authored.motif, actual.motif, authored.id)
            assertEquals(authored.frame, actual.frame.style, authored.id)
            assertEquals(authored.pedestal, actual.iconPedestal.style, authored.id)
            assertEquals(authored.ambient, actual.ambient.style, authored.id)
            assertEquals(authored.path, actual.energy.path, authored.id)
            assertEquals(authored.particle, actual.energy.particle, authored.id)
            assertEquals(authored.direction, actual.energy.direction, authored.id)
            assertEquals(authored.titleRail, actual.layout.titleRail, authored.id)
            assertEquals(2 + index % 2, actual.frame.lineCount, authored.id)
            assertEquals(index % 3, actual.frame.cutCorners, authored.id)
            assertEquals(index % 2 == 0, actual.frame.insetStroke, authored.id)
            assertEquals(1 + index % 3, actual.iconPedestal.ringCount, authored.id)
            assertEquals(DriftAxis.entries[index % DriftAxis.entries.size], actual.ambient.driftAxis, authored.id)
            assertEquals(0.10f + (index % 5) * 0.04f, actual.ambient.amplitude, authored.id)
            assertEquals(listOf(10, 12, 8, 10, 8)[index % 5], actual.energy.baseParticles, authored.id)
            assertEquals(index / 32f, actual.energy.phaseOffset, authored.id)
            assertEquals(BadgeAnchor.entries[index % BadgeAnchor.entries.size], actual.layout.badgeAnchor, authored.id)
            assertEquals(0.10f + (index % 4) * 0.02f, actual.layout.safeInsetFraction, authored.id)
            assertEquals(AccentRole.Primary, actual.frame.accent, authored.id)
            assertEquals(AccentRole.Secondary, actual.iconPedestal.accent, authored.id)
            assertEquals(ACCENTS, actual.energy.accents, authored.id)
            assertEquals(ACCENTS, actual.accents, authored.id)
        }
    }

    @Test
    fun `every built in theme has a distinct structural fingerprint`() {
        assertEquals(32, ThemeDecorationCatalog.all.values.map { it.structuralFingerprint() }.toSet().size)
    }

    @Test
    fun `all artwork themes still own complete token bundles`() {
        val artwork = BuiltInThemes.all.filter { it.tokenBundleId != null }
        assertEquals(21, artwork.size)
        artwork.forEach { assertNotNull(BuiltInThemeCatalog.byId(it.id), it.id) }
    }

    @Test
    fun `custom fallback is deterministic and never aliases a built in id`() {
        val first = ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0xFF7A53C7.toInt(), ColorMode.DARK)
        val second = ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0xFF7A53C7.toInt(), ColorMode.DARK)
        assertEquals(first, second)
        assertFalse(first.themeId in ThemeDecorationCatalog.all)
    }

    @Test
    fun `resolve prioritizes built ins then custom and default fallbacks`() {
        val builtIn = ThemeDecorationCatalog.all.getValue("Jade")
        assertSame(builtIn, ThemeDecorationCatalog.resolve("Jade", 0xFF7A53C7.toInt(), ColorMode.DARK))

        val custom = ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0xFF7A53C7.toInt(), ColorMode.DARK)
        assertFalse(custom.themeId in ThemeDecorationCatalog.all)
        assertEquals(ThemeDecorationCatalog.defaultSpec, ThemeDecorationCatalog.resolve(KawaiiPalette.None.name, 0, ColorMode.DARK))
        assertEquals(ThemeDecorationCatalog.defaultSpec, ThemeDecorationCatalog.resolve("unknown", 0xFF7A53C7.toInt(), ColorMode.DARK))
        assertEquals(ThemeDecorationCatalog.defaultSpec, ThemeDecorationCatalog.resolve(null, 0xFF7A53C7.toInt(), ColorMode.DARK))
    }

    private data class ExpectedRecipe(
        val id: String,
        val motif: OrnamentMotif,
        val frame: FrameStyle,
        val pedestal: PedestalStyle,
        val ambient: AmbientStyle,
        val path: EnergyPathStyle,
        val particle: ParticleStyle,
        val direction: EnergyDirection,
        val titleRail: TitleRailStyle,
    )

    private companion object {
        val ACCENTS = listOf(AccentRole.Primary, AccentRole.Secondary, AccentRole.Tertiary)
    }
}
