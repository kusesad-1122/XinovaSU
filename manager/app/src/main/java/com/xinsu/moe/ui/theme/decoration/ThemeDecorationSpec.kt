package com.xinsu.moe.ui.theme.decoration

import androidx.compose.runtime.Immutable
import java.util.Collections

@Immutable
data class ThemeDecorationSpec(
    val themeId: String,
    val motif: OrnamentMotif,
    val frame: FrameRecipe,
    val iconPedestal: IconPedestalRecipe,
    val ambient: AmbientRecipe,
    val energy: EnergyRecipe,
    val layout: CardLayoutRecipe,
    private val accentSnapshot: AccentSnapshot,
) {
    val accents: List<AccentRole>
        get() = accentSnapshot.values

    constructor(
        themeId: String,
        motif: OrnamentMotif,
        frame: FrameRecipe,
        iconPedestal: IconPedestalRecipe,
        ambient: AmbientRecipe,
        energy: EnergyRecipe,
        layout: CardLayoutRecipe,
        accents: List<AccentRole>,
    ) : this(
        themeId,
        motif,
        frame,
        iconPedestal,
        ambient,
        energy,
        layout,
        AccentSnapshot.from(accents),
    )

    fun copy(
        themeId: String = this.themeId,
        motif: OrnamentMotif = this.motif,
        frame: FrameRecipe = this.frame,
        iconPedestal: IconPedestalRecipe = this.iconPedestal,
        ambient: AmbientRecipe = this.ambient,
        energy: EnergyRecipe = this.energy,
        layout: CardLayoutRecipe = this.layout,
        accents: List<AccentRole>,
    ): ThemeDecorationSpec = ThemeDecorationSpec(
        themeId,
        motif,
        frame,
        iconPedestal,
        ambient,
        energy,
        layout,
        accents,
    )

    fun structuralFingerprint(): String = listOf(
        motif,
        frame.structuralFingerprint(),
        iconPedestal.structuralFingerprint(),
        ambient.structuralFingerprint(),
        energy.structuralFingerprint(),
        layout.structuralFingerprint(),
    ).joinToString("|")
}

enum class DecoratedCardRole { Hero, Monitor, Function, Standard, Compact }

enum class AccentRole { Primary, Secondary, Tertiary, Outline, Inverse, SurfaceGlow }

enum class EnergyDirection {
    StartToEnd,
    EndToStart,
    CenterOut,
    OutsideIn,
    BottomToTop,
    TopToBottom,
    Clockwise,
    CounterClockwise,
}

enum class OrnamentMotif {
    InkPanels, HeartRibbon, FrostScarf, IronWind, CloudSlope, SakuraStreet,
    SakuraCrown, CatCourtyard, MintPull, InkPoster, SilverMoon, CobaltDress,
    SkyRibbon, Windfield, CrimsonFocus, CreamStreet, BlackRose, SeaBreeze,
    PinkMist, FrostCrimson, BlueFlameCat, VisualNovel, SnowWindow, MoonOrbit,
    SakuraFan, MintVine, LavenderCrystal, CyberCircuit, ObsidianFacet,
    MicaLayer, EmberForge, JadeCloud,
}

enum class FrameStyle {
    PanelSplit, DoubleRibbon, TartanFrost, RivetRail, HorizonArc, WoodSakura,
    CrownFiligree, FelineCut, PullCord, PosterCrop, LunarFiligree, NightLace,
    SkyKnot, WindStitch, FocusBracket, StreetStamp, ThornMasonry, TideLine,
    WindowMist, FrostGem, CatFlame, DialogFrame, SnowFacet, OrbitRing, PetalGold,
    BotanicalGlass, CrystalConstellation, CircuitScan, ObsidianCrack, MicaSheet,
    ForgeEdge, JadeSeal,
}

enum class PedestalStyle {
    InkStamp, HeartGem, IceGem, BoltPlate, StarMedallion, PetalSeal, CrownJewel,
    PawSeal, KnotGem, TypeBlock, MoonGem, Rosette, RibbonGem, WindRose, ReticleGem,
    SunSeal, RoseGem, ShellGem, MistPearl, CrimsonGem, CatSigil, ChoiceCursor,
    SnowCrystal, OrbitCore, FanSeal, Dewdrop, Prism, Chip, ObsidianShard, MicaPearl,
    EmberCore, JadeMedallion,
}

enum class AmbientStyle {
    Halftone, SugarPearl, FineSnow, WindSparks, CloudBand, SunlitPetals,
    RoyalPetals, LeafWindow, MintLeaves, TornPaper, StarDust, MoonLeaves, CloudCurl,
    GrassWave, SpeedLines, WarmGrid, StoneCracks, WaterGlint, NightWindow, Rime,
    FlameHalo, SakuraOverlay, PowderSnow, Constellation, SoftPetals, VineVein,
    LavenderStars, ScanGrid, DarkRefraction, PearlDust, FurnaceAsh, AuspiciousCloud,
}

enum class EnergyPathStyle {
    BrushSweep, TwinConverge, CrystalGrow, RailCharge, RisingArc, PetalUpdraft,
    CrownOrbit, PawSteps, ElasticConverge, SlashCut, LunarOrbit, BeamDrop, RibbonFlip,
    FieldSweep, FocusBurst, LampMarch, ThornGrow, TideFill, MistGather,
    CrystalConverge, FlameSpiral, ChoiceConfirm, FrostBloom, PlanetOrbit,
    PetalFountain, VineGrow, PrismConnect, DataPulse, CoreFracture, LayerSweep,
    FuseBurn, QiFlow,
}

enum class ParticleStyle {
    InkDot, HeartSpark, SnowFlake, EmberSpark, StarShard, SakuraPetal, JewelPetal,
    CatGlint, MintLeaf, GlyphShard, SilverStar, BlueLeaf, SkySpark, GrassLeaf,
    CrimsonShard, CreamSpark, RosePetal, Bubble, PinkDroplet, FrostShard, BlueFlame,
    PetalCursor, HexSnow, Meteor, GoldPetal, GlowLeaf, CrystalShard, DataPacket,
    SpectrumShard, IridescentFlake, FireSpark, JadeSpark,
}

enum class DriftAxis { None, Horizontal, Vertical, Diagonal, Radial }

enum class TitleRailStyle {
    CaptionStrip, RibbonTab, WovenLabel, MetalPlate, HorizonTag, WoodPlaque,
    CrownRibbon, WhiskerTab, BowLabel, PosterBar, MoonRibbon, LaceLabel, SkyBanner,
    FieldTag, CameraStrip, StreetLabel, GothicPlaque, SailorRibbon, WindowLabel,
    WhiteSeal, NeonPaw, NamePlate, FrostTab, OrbitLabel, FanRibbon, LeafLabel,
    MirrorTab, HudRail, FacetLabel, FilmTab, ForgePlate, ScrollLabel,
}

enum class BadgeAnchor { TopStart, TopEnd, CenterStart, CenterEnd, BottomStart, BottomEnd }

@Immutable
data class FrameRecipe(
    val style: FrameStyle,
    val lineCount: Int,
    val cutCorners: Int,
    val insetStroke: Boolean,
    val accent: AccentRole,
) {
    fun structuralFingerprint(): String = listOf(style, lineCount, cutCorners, insetStroke).joinToString("|")
}

@Immutable
data class IconPedestalRecipe(
    val style: PedestalStyle,
    val ringCount: Int,
    val accent: AccentRole,
) {
    fun structuralFingerprint(): String = listOf(style, ringCount).joinToString("|")
}

@Immutable
data class AmbientRecipe(
    val style: AmbientStyle,
    val driftAxis: DriftAxis,
    val amplitude: Float,
) {
    fun structuralFingerprint(): String = listOf(style, driftAxis, amplitude).joinToString("|")
}

@Immutable
data class EnergyRecipe(
    val path: EnergyPathStyle,
    val particle: ParticleStyle,
    val direction: EnergyDirection,
    val baseParticles: Int,
    val phaseOffset: Float,
    private val accentSnapshot: AccentSnapshot,
) {
    val accents: List<AccentRole>
        get() = accentSnapshot.values

    constructor(
        path: EnergyPathStyle,
        particle: ParticleStyle,
        direction: EnergyDirection,
        baseParticles: Int,
        phaseOffset: Float,
        accents: List<AccentRole>,
    ) : this(
        path,
        particle,
        direction,
        baseParticles,
        phaseOffset,
        AccentSnapshot.from(accents),
    )

    fun copy(
        path: EnergyPathStyle = this.path,
        particle: ParticleStyle = this.particle,
        direction: EnergyDirection = this.direction,
        baseParticles: Int = this.baseParticles,
        phaseOffset: Float = this.phaseOffset,
        accents: List<AccentRole>,
    ): EnergyRecipe = EnergyRecipe(
        path,
        particle,
        direction,
        baseParticles,
        phaseOffset,
        accents,
    )

    fun structuralFingerprint(): String = listOf(
        path,
        particle,
        direction,
        baseParticles,
        phaseOffset,
    ).joinToString("|")
}

@Immutable
data class CardLayoutRecipe(
    val titleRail: TitleRailStyle,
    val badgeAnchor: BadgeAnchor,
    val safeInsetFraction: Float,
) {
    fun structuralFingerprint(): String = listOf(
        titleRail,
        badgeAnchor,
        safeInsetFraction,
    ).joinToString("|")
}

data class AccentSnapshot private constructor(
    val values: List<AccentRole>,
) {
    companion object {
        fun from(accents: List<AccentRole>): AccentSnapshot = AccentSnapshot(
            Collections.unmodifiableList(accents.toList()),
        )
    }
}
