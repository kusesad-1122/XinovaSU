package com.xinsu.moe.ui.theme.decoration

import com.xinsu.moe.ui.theme.ColorMode
import com.xinsu.moe.ui.theme.KawaiiPalette
import java.util.Collections

object ThemeDecorationCatalog {
    val all: Map<String, ThemeDecorationSpec> by lazy {
        Collections.unmodifiableMap(
            AUTHORED_RECIPES.mapIndexed { index, recipe ->
                recipe.id to recipe.toSpec(index)
            }.toMap(linkedMapOf()),
        )
    }

    val defaultSpec: ThemeDecorationSpec by lazy {
        ThemeDecorationSpec(
            themeId = DEFAULT_THEME_ID,
            motif = OrnamentMotif.ObsidianFacet,
            frame = FrameRecipe(FrameStyle.CircuitScan, 2, 2, true, ACCENTS[0]),
            iconPedestal = IconPedestalRecipe(PedestalStyle.JadeMedallion, 2, ACCENTS[1]),
            ambient = AmbientRecipe(AmbientStyle.DarkRefraction, DriftAxis.Radial, 0.18f),
            energy = EnergyRecipe(
                path = EnergyPathStyle.QiFlow,
                particle = ParticleStyle.SpectrumShard,
                direction = EnergyDirection.Clockwise,
                baseParticles = 10,
                phaseOffset = 0f,
                accents = ACCENTS,
            ),
            layout = CardLayoutRecipe(TitleRailStyle.FacetLabel, BadgeAnchor.TopEnd, 0.12f),
            accents = ACCENTS,
        )
    }

    fun resolve(themeId: String?, keyColor: Int, colorMode: ColorMode): ThemeDecorationSpec {
        all[themeId]?.let { return it }
        return when {
            themeId == KawaiiPalette.None.name && keyColor != 0 -> customSpec(keyColor, colorMode)
            else -> defaultSpec
        }
    }

    private fun customSpec(keyColor: Int, colorMode: ColorMode): ThemeDecorationSpec {
        val unsignedColor = keyColor.toLong() and 0xFFFF_FFFFL
        val seed = unsignedColor * 31L + colorMode.value
        fun slot(offset: Int, size: Int): Int = ((seed + offset * 37L) % size).toInt()

        return ThemeDecorationSpec(
            themeId = "$CUSTOM_THEME_PREFIX${unsignedColor.toString(16)}-${colorMode.value}",
            motif = OrnamentMotif.entries[slot(0, OrnamentMotif.entries.size)],
            frame = FrameRecipe(
                style = FrameStyle.entries[slot(1, FrameStyle.entries.size)],
                lineCount = 2 + slot(2, 2),
                cutCorners = slot(3, 3),
                insetStroke = slot(4, 2) == 0,
                accent = ACCENTS[0],
            ),
            iconPedestal = IconPedestalRecipe(
                style = PedestalStyle.entries[slot(5, PedestalStyle.entries.size)],
                ringCount = 1 + slot(6, 3),
                accent = ACCENTS[1],
            ),
            ambient = AmbientRecipe(
                style = AmbientStyle.entries[slot(7, AmbientStyle.entries.size)],
                driftAxis = DriftAxis.entries[slot(8, DriftAxis.entries.size)],
                amplitude = 0.10f + slot(9, 5) * 0.04f,
            ),
            energy = EnergyRecipe(
                path = EnergyPathStyle.entries[slot(10, EnergyPathStyle.entries.size)],
                particle = ParticleStyle.entries[slot(11, ParticleStyle.entries.size)],
                direction = EnergyDirection.entries[slot(12, EnergyDirection.entries.size)],
                baseParticles = PARTICLE_COUNTS[slot(13, PARTICLE_COUNTS.size)],
                phaseOffset = slot(14, 32) / 32f,
                accents = ACCENTS,
            ),
            layout = CardLayoutRecipe(
                titleRail = TitleRailStyle.entries[slot(15, TitleRailStyle.entries.size)],
                badgeAnchor = BadgeAnchor.entries[slot(16, BadgeAnchor.entries.size)],
                safeInsetFraction = 0.10f + slot(17, 4) * 0.02f,
            ),
            accents = ACCENTS,
        )
    }

    private fun AuthoredRecipe.toSpec(index: Int): ThemeDecorationSpec = ThemeDecorationSpec(
        themeId = id,
        motif = motif,
        frame = FrameRecipe(
            style = frame,
            lineCount = 2 + index % 2,
            cutCorners = index % 3,
            insetStroke = index % 2 == 0,
            accent = ACCENTS[0],
        ),
        iconPedestal = IconPedestalRecipe(
            style = pedestal,
            ringCount = 1 + index % 3,
            accent = ACCENTS[1],
        ),
        ambient = AmbientRecipe(
            style = ambient,
            driftAxis = DriftAxis.entries[index % DriftAxis.entries.size],
            amplitude = 0.10f + (index % 5) * 0.04f,
        ),
        energy = EnergyRecipe(
            path = path,
            particle = particle,
            direction = direction,
            baseParticles = PARTICLE_COUNTS[index % PARTICLE_COUNTS.size],
            phaseOffset = index / 32f,
            accents = ACCENTS,
        ),
        layout = CardLayoutRecipe(
            titleRail = titleRail,
            badgeAnchor = BadgeAnchor.entries[index % BadgeAnchor.entries.size],
            safeInsetFraction = 0.10f + (index % 4) * 0.02f,
        ),
        accents = ACCENTS,
    )

    private data class AuthoredRecipe(
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

    private const val DEFAULT_THEME_ID = "xinovasu-default-decoration"
    private const val CUSTOM_THEME_PREFIX = "xinovasu-custom-decoration-"
    private val ACCENTS = listOf(AccentRole.Primary, AccentRole.Secondary, AccentRole.Tertiary)
    private val PARTICLE_COUNTS = listOf(10, 12, 8, 10, 8)

    private val AUTHORED_RECIPES = listOf(
        AuthoredRecipe("ink-white-companions", OrnamentMotif.InkPanels, FrameStyle.PanelSplit, PedestalStyle.InkStamp, AmbientStyle.Halftone, EnergyPathStyle.BrushSweep, ParticleStyle.InkDot, EnergyDirection.StartToEnd, TitleRailStyle.CaptionStrip),
        AuthoredRecipe("twin-peach-heartstrings", OrnamentMotif.HeartRibbon, FrameStyle.DoubleRibbon, PedestalStyle.HeartGem, AmbientStyle.SugarPearl, EnergyPathStyle.TwinConverge, ParticleStyle.HeartSpark, EnergyDirection.OutsideIn, TitleRailStyle.RibbonTab),
        AuthoredRecipe("winter-blue-scarf", OrnamentMotif.FrostScarf, FrameStyle.TartanFrost, PedestalStyle.IceGem, AmbientStyle.FineSnow, EnergyPathStyle.CrystalGrow, ParticleStyle.SnowFlake, EnergyDirection.CenterOut, TitleRailStyle.WovenLabel),
        AuthoredRecipe("dusk-iron-wind", OrnamentMotif.IronWind, FrameStyle.RivetRail, PedestalStyle.BoltPlate, AmbientStyle.WindSparks, EnergyPathStyle.RailCharge, ParticleStyle.EmberSpark, EnergyDirection.StartToEnd, TitleRailStyle.MetalPlate),
        AuthoredRecipe("cloud-slope-stars", OrnamentMotif.CloudSlope, FrameStyle.HorizonArc, PedestalStyle.StarMedallion, AmbientStyle.CloudBand, EnergyPathStyle.RisingArc, ParticleStyle.StarShard, EnergyDirection.BottomToTop, TitleRailStyle.HorizonTag),
        AuthoredRecipe("sakura-street-walk", OrnamentMotif.SakuraStreet, FrameStyle.WoodSakura, PedestalStyle.PetalSeal, AmbientStyle.SunlitPetals, EnergyPathStyle.PetalUpdraft, ParticleStyle.SakuraPetal, EnergyDirection.BottomToTop, TitleRailStyle.WoodPlaque),
        AuthoredRecipe("sakura-crown-overture", OrnamentMotif.SakuraCrown, FrameStyle.CrownFiligree, PedestalStyle.CrownJewel, AmbientStyle.RoyalPetals, EnergyPathStyle.CrownOrbit, ParticleStyle.JewelPetal, EnergyDirection.Clockwise, TitleRailStyle.CrownRibbon),
        AuthoredRecipe("golden-eye-cat-courtyard", OrnamentMotif.CatCourtyard, FrameStyle.FelineCut, PedestalStyle.PawSeal, AmbientStyle.LeafWindow, EnergyPathStyle.PawSteps, ParticleStyle.CatGlint, EnergyDirection.StartToEnd, TitleRailStyle.WhiskerTab),
        AuthoredRecipe("mint-pull", OrnamentMotif.MintPull, FrameStyle.PullCord, PedestalStyle.KnotGem, AmbientStyle.MintLeaves, EnergyPathStyle.ElasticConverge, ParticleStyle.MintLeaf, EnergyDirection.OutsideIn, TitleRailStyle.BowLabel),
        AuthoredRecipe("ink-order-poster", OrnamentMotif.InkPoster, FrameStyle.PosterCrop, PedestalStyle.TypeBlock, AmbientStyle.TornPaper, EnergyPathStyle.SlashCut, ParticleStyle.GlyphShard, EnergyDirection.StartToEnd, TitleRailStyle.PosterBar),
        AuthoredRecipe("moonlit-silver-blue", OrnamentMotif.SilverMoon, FrameStyle.LunarFiligree, PedestalStyle.MoonGem, AmbientStyle.StarDust, EnergyPathStyle.LunarOrbit, ParticleStyle.SilverStar, EnergyDirection.Clockwise, TitleRailStyle.MoonRibbon),
        AuthoredRecipe("cobalt-night-dress", OrnamentMotif.CobaltDress, FrameStyle.NightLace, PedestalStyle.Rosette, AmbientStyle.MoonLeaves, EnergyPathStyle.BeamDrop, ParticleStyle.BlueLeaf, EnergyDirection.TopToBottom, TitleRailStyle.LaceLabel),
        AuthoredRecipe("clear-sky-blue-ribbon", OrnamentMotif.SkyRibbon, FrameStyle.SkyKnot, PedestalStyle.RibbonGem, AmbientStyle.CloudCurl, EnergyPathStyle.RibbonFlip, ParticleStyle.SkySpark, EnergyDirection.StartToEnd, TitleRailStyle.SkyBanner),
        AuthoredRecipe("windfield-doll", OrnamentMotif.Windfield, FrameStyle.WindStitch, PedestalStyle.WindRose, AmbientStyle.GrassWave, EnergyPathStyle.FieldSweep, ParticleStyle.GrassLeaf, EnergyDirection.StartToEnd, TitleRailStyle.FieldTag),
        AuthoredRecipe("crimson-eye-jump", OrnamentMotif.CrimsonFocus, FrameStyle.FocusBracket, PedestalStyle.ReticleGem, AmbientStyle.SpeedLines, EnergyPathStyle.FocusBurst, ParticleStyle.CrimsonShard, EnergyDirection.OutsideIn, TitleRailStyle.CameraStrip),
        AuthoredRecipe("cream-street-corner", OrnamentMotif.CreamStreet, FrameStyle.StreetStamp, PedestalStyle.SunSeal, AmbientStyle.WarmGrid, EnergyPathStyle.LampMarch, ParticleStyle.CreamSpark, EnergyDirection.StartToEnd, TitleRailStyle.StreetLabel),
        AuthoredRecipe("black-rose-stone-court", OrnamentMotif.BlackRose, FrameStyle.ThornMasonry, PedestalStyle.RoseGem, AmbientStyle.StoneCracks, EnergyPathStyle.ThornGrow, ParticleStyle.RosePetal, EnergyDirection.StartToEnd, TitleRailStyle.GothicPlaque),
        AuthoredRecipe("sea-breeze-song", OrnamentMotif.SeaBreeze, FrameStyle.TideLine, PedestalStyle.ShellGem, AmbientStyle.WaterGlint, EnergyPathStyle.TideFill, ParticleStyle.Bubble, EnergyDirection.StartToEnd, TitleRailStyle.SailorRibbon),
        AuthoredRecipe("pink-mist-night-window", OrnamentMotif.PinkMist, FrameStyle.WindowMist, PedestalStyle.MistPearl, AmbientStyle.NightWindow, EnergyPathStyle.MistGather, ParticleStyle.PinkDroplet, EnergyDirection.OutsideIn, TitleRailStyle.WindowLabel),
        AuthoredRecipe("frost-white-crimson-eye", OrnamentMotif.FrostCrimson, FrameStyle.FrostGem, PedestalStyle.CrimsonGem, AmbientStyle.Rime, EnergyPathStyle.CrystalConverge, ParticleStyle.FrostShard, EnergyDirection.OutsideIn, TitleRailStyle.WhiteSeal),
        AuthoredRecipe("blue-flame-cat-shadow", OrnamentMotif.BlueFlameCat, FrameStyle.CatFlame, PedestalStyle.CatSigil, AmbientStyle.FlameHalo, EnergyPathStyle.FlameSpiral, ParticleStyle.BlueFlame, EnergyDirection.CounterClockwise, TitleRailStyle.NeonPaw),
        AuthoredRecipe("SakuraVN", OrnamentMotif.VisualNovel, FrameStyle.DialogFrame, PedestalStyle.ChoiceCursor, AmbientStyle.SakuraOverlay, EnergyPathStyle.ChoiceConfirm, ParticleStyle.PetalCursor, EnergyDirection.StartToEnd, TitleRailStyle.NamePlate),
        AuthoredRecipe("Snow", OrnamentMotif.SnowWindow, FrameStyle.SnowFacet, PedestalStyle.SnowCrystal, AmbientStyle.PowderSnow, EnergyPathStyle.FrostBloom, ParticleStyle.HexSnow, EnergyDirection.CenterOut, TitleRailStyle.FrostTab),
        AuthoredRecipe("Moonlit", OrnamentMotif.MoonOrbit, FrameStyle.OrbitRing, PedestalStyle.OrbitCore, AmbientStyle.Constellation, EnergyPathStyle.PlanetOrbit, ParticleStyle.Meteor, EnergyDirection.Clockwise, TitleRailStyle.OrbitLabel),
        AuthoredRecipe("Sakura", OrnamentMotif.SakuraFan, FrameStyle.PetalGold, PedestalStyle.FanSeal, AmbientStyle.SoftPetals, EnergyPathStyle.PetalFountain, ParticleStyle.GoldPetal, EnergyDirection.BottomToTop, TitleRailStyle.FanRibbon),
        AuthoredRecipe("Mint", OrnamentMotif.MintVine, FrameStyle.BotanicalGlass, PedestalStyle.Dewdrop, AmbientStyle.VineVein, EnergyPathStyle.VineGrow, ParticleStyle.GlowLeaf, EnergyDirection.StartToEnd, TitleRailStyle.LeafLabel),
        AuthoredRecipe("Lavender", OrnamentMotif.LavenderCrystal, FrameStyle.CrystalConstellation, PedestalStyle.Prism, AmbientStyle.LavenderStars, EnergyPathStyle.PrismConnect, ParticleStyle.CrystalShard, EnergyDirection.CenterOut, TitleRailStyle.MirrorTab),
        AuthoredRecipe("Cyber", OrnamentMotif.CyberCircuit, FrameStyle.CircuitScan, PedestalStyle.Chip, AmbientStyle.ScanGrid, EnergyPathStyle.DataPulse, ParticleStyle.DataPacket, EnergyDirection.StartToEnd, TitleRailStyle.HudRail),
        AuthoredRecipe("Obsidian", OrnamentMotif.ObsidianFacet, FrameStyle.ObsidianCrack, PedestalStyle.ObsidianShard, AmbientStyle.DarkRefraction, EnergyPathStyle.CoreFracture, ParticleStyle.SpectrumShard, EnergyDirection.CenterOut, TitleRailStyle.FacetLabel),
        AuthoredRecipe("Mica", OrnamentMotif.MicaLayer, FrameStyle.MicaSheet, PedestalStyle.MicaPearl, AmbientStyle.PearlDust, EnergyPathStyle.LayerSweep, ParticleStyle.IridescentFlake, EnergyDirection.StartToEnd, TitleRailStyle.FilmTab),
        AuthoredRecipe("Ember", OrnamentMotif.EmberForge, FrameStyle.ForgeEdge, PedestalStyle.EmberCore, AmbientStyle.FurnaceAsh, EnergyPathStyle.FuseBurn, ParticleStyle.FireSpark, EnergyDirection.StartToEnd, TitleRailStyle.ForgePlate),
        AuthoredRecipe("Jade", OrnamentMotif.JadeCloud, FrameStyle.JadeSeal, PedestalStyle.JadeMedallion, AmbientStyle.AuspiciousCloud, EnergyPathStyle.QiFlow, ParticleStyle.JadeSpark, EnergyDirection.Clockwise, TitleRailStyle.ScrollLabel),
    )
}
