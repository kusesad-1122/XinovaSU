package com.xinsu.moe.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.xinsu.moe.ui.theme.decoration.DecorationDensityPolicy
import com.xinsu.moe.ui.theme.decoration.EnergyDirection
import com.xinsu.moe.ui.theme.decoration.EnergyPathStyle
import com.xinsu.moe.ui.theme.decoration.LogicalLayoutDirection
import com.xinsu.moe.ui.theme.decoration.OrnamentMotif
import com.xinsu.moe.ui.theme.decoration.ParticleStyle
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpec
import com.xinsu.moe.ui.theme.decoration.horizontalEnergyFraction
import com.xinsu.moe.ui.theme.decoration.withRenderCoordinatesAt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class EnergyParticleSeeds private constructor(
    val count: Int,
    private val values: FloatArray,
) {
    fun value(particleIndex: Int, salt: Int): Float =
        values[particleIndex * SeedSlots + salt]

    companion object {
        fun create(spec: ThemeDecorationSpec, maxParticles: Int): EnergyParticleSeeds {
            val count = minOf(spec.energy.baseParticles, maxParticles).coerceAtLeast(0)
            val values = FloatArray(count * SeedSlots)
            repeat(count) { particleIndex ->
                repeat(SeedSlots) { salt ->
                    values[particleIndex * SeedSlots + salt] = stableUnit(
                        spec.themeId,
                        particleIndex,
                        salt,
                    )
                }
            }
            return EnergyParticleSeeds(count, values)
        }

        private const val SeedSlots = 5
    }
}

private enum class ParticleColorSlot { Primary, Secondary, Tertiary }

internal fun resolvedEnergyPath(spec: ThemeDecorationSpec): EnergyPathStyle {
    val motifPath = when (spec.motif) {
        OrnamentMotif.InkPanels -> EnergyPathStyle.BrushSweep
        OrnamentMotif.HeartRibbon -> EnergyPathStyle.TwinConverge
        OrnamentMotif.FrostScarf -> EnergyPathStyle.CrystalGrow
        OrnamentMotif.IronWind -> EnergyPathStyle.RailCharge
        OrnamentMotif.CloudSlope -> EnergyPathStyle.RisingArc
        OrnamentMotif.SakuraStreet -> EnergyPathStyle.PetalUpdraft
        OrnamentMotif.SakuraCrown -> EnergyPathStyle.CrownOrbit
        OrnamentMotif.CatCourtyard -> EnergyPathStyle.PawSteps
        OrnamentMotif.MintPull -> EnergyPathStyle.ElasticConverge
        OrnamentMotif.InkPoster -> EnergyPathStyle.SlashCut
        OrnamentMotif.SilverMoon -> EnergyPathStyle.LunarOrbit
        OrnamentMotif.CobaltDress -> EnergyPathStyle.BeamDrop
        OrnamentMotif.SkyRibbon -> EnergyPathStyle.RibbonFlip
        OrnamentMotif.Windfield -> EnergyPathStyle.FieldSweep
        OrnamentMotif.CrimsonFocus -> EnergyPathStyle.FocusBurst
        OrnamentMotif.CreamStreet -> EnergyPathStyle.LampMarch
        OrnamentMotif.BlackRose -> EnergyPathStyle.ThornGrow
        OrnamentMotif.SeaBreeze -> EnergyPathStyle.TideFill
        OrnamentMotif.PinkMist -> EnergyPathStyle.MistGather
        OrnamentMotif.FrostCrimson -> EnergyPathStyle.CrystalConverge
        OrnamentMotif.BlueFlameCat -> EnergyPathStyle.FlameSpiral
        OrnamentMotif.VisualNovel -> EnergyPathStyle.ChoiceConfirm
        OrnamentMotif.SnowWindow -> EnergyPathStyle.FrostBloom
        OrnamentMotif.MoonOrbit -> EnergyPathStyle.PlanetOrbit
        OrnamentMotif.SakuraFan -> EnergyPathStyle.PetalFountain
        OrnamentMotif.MintVine -> EnergyPathStyle.VineGrow
        OrnamentMotif.LavenderCrystal -> EnergyPathStyle.PrismConnect
        OrnamentMotif.CyberCircuit -> EnergyPathStyle.DataPulse
        OrnamentMotif.ObsidianFacet -> EnergyPathStyle.CoreFracture
        OrnamentMotif.MicaLayer -> EnergyPathStyle.LayerSweep
        OrnamentMotif.EmberForge -> EnergyPathStyle.FuseBurn
        OrnamentMotif.JadeCloud -> EnergyPathStyle.QiFlow
    }
    return if (spec.energy.path == motifPath) spec.energy.path else motifPath
}

internal fun DrawScope.drawEnergyOrnament(
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    trackProgress: Float,
    particleProgress: Float,
    particleAlpha: Float,
    overallAlpha: Float,
    policy: DecorationDensityPolicy,
    geometry: OrnamentGeometry,
    particleSeeds: EnergyParticleSeeds,
) {
    val track = trackProgress.coerceIn(0f, 1f)
    val particle = particleProgress.coerceIn(0f, 1f)
    val boundedOverallAlpha = overallAlpha.coerceIn(0f, 1f)
    val boundedParticleAlpha = particleAlpha.coerceIn(0f, 1f) * boundedOverallAlpha
    if (track <= 0f && boundedParticleAlpha <= 0f) return

    val particleCount = minOf(spec.energy.baseParticles, policy.maxParticles, particleSeeds.count)
    val resolvedTrack = geometry.energyPath.toEnergyTrack()
    val firstAccent = spec.energy.accents.firstOrNull()
    val accent = if (firstAccent == null) colors.primary else colors.forAccent(firstAccent)
    val trackAlpha = boundedOverallAlpha
    if (track > 0f) {
        drawEnergyTrack(resolvedTrack, geometry, colors, accent, track, trackAlpha)
    }

    val bounds = geometry.particleBounds ?: return
    if (boundedParticleAlpha <= 0f) return
    repeat(particleCount) { particleIndex ->
        val phase = (
            particleSeeds.value(particleIndex, 0) +
                spec.energy.phaseOffset +
                particle
            ) % 1f
        val drift = particleSeeds.value(particleIndex, 1) - 0.5f
        val towardEnd = particleSeeds.value(particleIndex, 4) >= 0.5f
        val point = directedParticlePoint(
            direction = spec.energy.direction,
            phase = phase,
            drift = drift,
            towardEnd = towardEnd,
            bounds = bounds,
            corner = geometry.corner,
            layoutDirection = geometry.layoutDirection,
        )
        val particleColor = when (ParticleColorSlot.entries[particleIndex % ParticleColorSlot.entries.size]) {
            ParticleColorSlot.Primary -> accent
            ParticleColorSlot.Secondary -> colors.secondary
            ParticleColorSlot.Tertiary -> colors.tertiary
        }
        drawEnergyParticle(
            style = spec.energy.particle,
            center = point,
            radius = geometry.corner * (0.08f + particleSeeds.value(particleIndex, 2) * 0.08f),
            color = particleColor,
            alpha = boundedParticleAlpha *
                (0.30f + particle * 0.58f) *
                (0.72f + particleSeeds.value(particleIndex, 3) * 0.28f),
        )
    }
}

private fun EnergyPathStyle.toEnergyTrack(): EnergyTrack = when (this) {
    EnergyPathStyle.BrushSweep -> EnergyTrack.BrushSweep
    EnergyPathStyle.TwinConverge -> EnergyTrack.TwinConverge
    EnergyPathStyle.CrystalGrow -> EnergyTrack.CrystalGrow
    EnergyPathStyle.RailCharge -> EnergyTrack.RailCharge
    EnergyPathStyle.RisingArc -> EnergyTrack.RisingArc
    EnergyPathStyle.PetalUpdraft -> EnergyTrack.PetalUpdraft
    EnergyPathStyle.CrownOrbit -> EnergyTrack.CrownOrbit
    EnergyPathStyle.PawSteps -> EnergyTrack.PawSteps
    EnergyPathStyle.ElasticConverge -> EnergyTrack.ElasticConverge
    EnergyPathStyle.SlashCut -> EnergyTrack.SlashCut
    EnergyPathStyle.LunarOrbit -> EnergyTrack.LunarOrbit
    EnergyPathStyle.BeamDrop -> EnergyTrack.BeamDrop
    EnergyPathStyle.RibbonFlip -> EnergyTrack.RibbonFlip
    EnergyPathStyle.FieldSweep -> EnergyTrack.FieldSweep
    EnergyPathStyle.FocusBurst -> EnergyTrack.FocusBurst
    EnergyPathStyle.LampMarch -> EnergyTrack.LampMarch
    EnergyPathStyle.ThornGrow -> EnergyTrack.ThornGrow
    EnergyPathStyle.TideFill -> EnergyTrack.TideFill
    EnergyPathStyle.MistGather -> EnergyTrack.MistGather
    EnergyPathStyle.CrystalConverge -> EnergyTrack.CrystalConverge
    EnergyPathStyle.FlameSpiral -> EnergyTrack.FlameSpiral
    EnergyPathStyle.ChoiceConfirm -> EnergyTrack.ChoiceConfirm
    EnergyPathStyle.FrostBloom -> EnergyTrack.FrostBloom
    EnergyPathStyle.PlanetOrbit -> EnergyTrack.PlanetOrbit
    EnergyPathStyle.PetalFountain -> EnergyTrack.PetalFountain
    EnergyPathStyle.VineGrow -> EnergyTrack.VineGrow
    EnergyPathStyle.PrismConnect -> EnergyTrack.PrismConnect
    EnergyPathStyle.DataPulse -> EnergyTrack.DataPulse
    EnergyPathStyle.CoreFracture -> EnergyTrack.CoreFracture
    EnergyPathStyle.LayerSweep -> EnergyTrack.LayerSweep
    EnergyPathStyle.FuseBurn -> EnergyTrack.FuseBurn
    EnergyPathStyle.QiFlow -> EnergyTrack.QiFlow
}

private enum class EnergyTrack {
    BrushSweep, TwinConverge, CrystalGrow, RailCharge, RisingArc, PetalUpdraft,
    CrownOrbit, PawSteps, ElasticConverge, SlashCut, LunarOrbit, BeamDrop,
    RibbonFlip, FieldSweep, FocusBurst, LampMarch, ThornGrow, TideFill,
    MistGather, CrystalConverge, FlameSpiral, ChoiceConfirm, FrostBloom,
    PlanetOrbit, PetalFountain, VineGrow, PrismConnect, DataPulse, CoreFracture,
    LayerSweep, FuseBurn, QiFlow,
}

private fun DrawScope.drawEnergyTrack(
    track: EnergyTrack,
    g: OrnamentGeometry,
    colors: DecorationColors,
    accent: Color,
    progress: Float,
    overallAlpha: Float,
) {
    val alpha = (0.20f + progress * 0.48f) * overallAlpha
    g.horizontalTrack.withRenderCoordinatesAt(progress) { horizontalStartX, horizontalEndX, horizontalDeltaX, horizontalDirection ->
        val verticalHeight = (g.height - g.edgeInset * 2f) * progress
        when (track) {
            EnergyTrack.BrushSweep -> drawWavePrimitive(Offset(horizontalStartX, g.energyLaneY), horizontalDeltaX, g.corner * 0.12f, accent, 2, alpha)
            EnergyTrack.TwinConverge -> {
                drawLinePrimitive(
                    Offset(horizontalStartX, g.bottomStartCorner.y),
                    Offset(horizontalEndX, g.height - g.edgeInset),
                    accent,
                    2f,
                    alpha,
                )
                drawLinePrimitive(
                    Offset(g.width - horizontalStartX, g.bottomEndCorner.y),
                    Offset(g.width - horizontalEndX, g.height - g.edgeInset),
                    colors.secondary,
                    2f,
                    alpha,
                )
            }
            EnergyTrack.CrystalGrow -> drawCrystalPrimitive(Offset(g.width * 0.5f, g.height - verticalHeight * 0.5f), g.corner * (0.2f + progress * 0.32f), accent, alpha)
            EnergyTrack.RailCharge -> drawLinePrimitive(Offset(horizontalStartX, g.energyLaneY), Offset(horizontalEndX, g.energyLaneY), accent, 2.2f, alpha)
            EnergyTrack.RisingArc -> drawArcPrimitive(g.bottomStartCorner, g.corner * (0.5f + progress), 205f, 120f * progress, accent, 2f, alpha)
            EnergyTrack.PetalUpdraft -> drawRibbonPrimitive(g.bottomStartCorner, Offset(g.bottomStartCorner.x + g.corner, g.bottomStartCorner.y - verticalHeight), accent, -g.corner * 0.5f, alpha)
            EnergyTrack.CrownOrbit -> drawArcPrimitive(g.topEndCorner, g.corner * 0.64f, -90f, 360f * progress, accent, 2f, alpha)
            EnergyTrack.PawSteps -> drawCirclePrimitive(Offset(horizontalEndX, g.height - g.corner * 0.38f), g.corner * 0.18f, accent, alpha, filled = false)
            EnergyTrack.ElasticConverge -> drawRibbonPrimitive(Offset(horizontalStartX, g.topStartCorner.y), Offset(horizontalEndX, g.topEndCorner.y), accent, g.corner * (0.6f - progress * 0.4f), alpha)
            EnergyTrack.SlashCut -> drawLinePrimitive(Offset(horizontalStartX, g.height - g.edgeInset), Offset(horizontalEndX, g.edgeInset), accent, 2.4f, alpha)
            EnergyTrack.LunarOrbit -> drawArcPrimitive(g.topStartCorner, g.corner * 0.58f, 45f, 300f * progress, accent, 1.8f, alpha)
            EnergyTrack.BeamDrop -> drawLinePrimitive(Offset(g.width - g.corner, g.edgeInset), Offset(g.width - g.corner, g.edgeInset + verticalHeight), accent, 2.2f, alpha)
            EnergyTrack.RibbonFlip -> drawRibbonPrimitive(Offset(horizontalStartX, g.topStartCorner.y), Offset(horizontalEndX, g.topEndCorner.y), accent, -g.corner * 0.42f, alpha)
            EnergyTrack.FieldSweep -> drawWavePrimitive(Offset(horizontalStartX, g.energyLaneY), horizontalDeltaX, g.corner * 0.16f, accent, 3, alpha)
            EnergyTrack.FocusBurst -> drawSparkPrimitive(g.topEndCorner, g.corner * (0.18f + progress * 0.5f), accent, alpha)
            EnergyTrack.LampMarch -> drawHalftonePrimitive(Offset(horizontalStartX, g.height - g.corner * 0.52f), (1 + progress * 4f).toInt(), g.corner * 0.32f, accent, alpha, horizontalDirection = horizontalDirection)
            EnergyTrack.ThornGrow -> drawWavePrimitive(Offset(horizontalStartX, g.energyLaneY), horizontalDeltaX, g.corner * 0.10f, accent, 5, alpha)
            EnergyTrack.TideFill -> drawWavePrimitive(Offset(horizontalStartX, g.height - g.corner * (0.2f + progress)), horizontalDeltaX, g.corner * 0.14f, accent, 3, alpha)
            EnergyTrack.MistGather -> drawCirclePrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * (1.2f - progress * 0.7f), accent, alpha, filled = false)
            EnergyTrack.CrystalConverge -> drawDiamondPrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * (0.2f + progress * 0.46f), accent, alpha)
            EnergyTrack.FlameSpiral -> drawArcPrimitive(g.bottomEndCorner, g.corner * (0.3f + progress * 0.55f), 30f, -330f * progress, accent, 2.2f, alpha)
            EnergyTrack.ChoiceConfirm -> drawLinePrimitive(Offset(horizontalStartX, g.bottomStartCorner.y), Offset(horizontalEndX, g.bottomStartCorner.y), accent, 2f, alpha)
            EnergyTrack.FrostBloom -> drawSparkPrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * (0.25f + progress * 0.55f), accent, alpha)
            EnergyTrack.PlanetOrbit -> drawArcPrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * 0.85f, -90f, 360f * progress, accent, 1.8f, alpha)
            EnergyTrack.PetalFountain -> drawRibbonPrimitive(g.bottomEndCorner, Offset(g.bottomEndCorner.x - g.corner, g.bottomEndCorner.y - verticalHeight), accent, g.corner * 0.55f, alpha)
            EnergyTrack.VineGrow -> drawWavePrimitive(Offset(horizontalStartX, g.height - g.edgeInset), horizontalDeltaX, g.corner * 0.18f, accent, 2, alpha)
            EnergyTrack.PrismConnect -> drawLinePrimitive(g.bottomStartCorner, Offset(g.width * 0.5f, g.edgeInset + verticalHeight * 0.3f), accent, 2f, alpha)
            EnergyTrack.DataPulse -> drawLinePrimitive(Offset(horizontalStartX, g.topStartCorner.y), Offset(horizontalEndX, g.topStartCorner.y), accent, 2.6f, alpha)
            EnergyTrack.CoreFracture -> drawDiamondPrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * progress, accent, alpha)
            EnergyTrack.LayerSweep -> drawRibbonPrimitive(Offset(horizontalStartX, g.topStartCorner.y), Offset(horizontalEndX, g.topStartCorner.y), accent, g.corner * 0.14f, alpha)
            EnergyTrack.FuseBurn -> drawSparkPrimitive(Offset(horizontalEndX, g.height - g.edgeInset), g.corner * 0.26f, accent, alpha)
            EnergyTrack.QiFlow -> drawArcPrimitive(Offset(g.width * 0.5f, g.height * 0.5f), g.corner * 0.74f, -90f, 360f * progress, accent, 2f, alpha)
        }
    }
}

private fun directedParticlePoint(
    direction: EnergyDirection,
    phase: Float,
    drift: Float,
    towardEnd: Boolean,
    bounds: ParticleBounds,
    corner: Float,
    layoutDirection: LogicalLayoutDirection,
): Offset {
    val width = bounds.right - bounds.left
    val height = bounds.bottom - bounds.top
    val center = Offset(
        (bounds.left + bounds.right) * 0.5f,
        (bounds.top + bounds.bottom) * 0.5f,
    )
    val radialAngle = phase * 2f * PI.toFloat()
    val point = when (direction) {
        EnergyDirection.StartToEnd,
        EnergyDirection.EndToStart -> Offset(
            bounds.left + width * horizontalEnergyFraction(direction, phase, towardEnd, layoutDirection),
            center.y + drift * corner * 1.8f,
        )
        EnergyDirection.CenterOut,
        EnergyDirection.OutsideIn -> Offset(
            bounds.left + width * horizontalEnergyFraction(direction, phase, towardEnd, layoutDirection),
            center.y + drift * corner * 1.6f,
        )
        EnergyDirection.BottomToTop -> Offset(center.x + drift * corner * 1.8f, bounds.bottom - height * phase)
        EnergyDirection.TopToBottom -> Offset(center.x + drift * corner * 1.8f, bounds.top + height * phase)
        EnergyDirection.Clockwise -> Offset(center.x + cos(radialAngle) * width * 0.38f, center.y + sin(radialAngle) * height * 0.38f)
        EnergyDirection.CounterClockwise -> Offset(center.x + cos(-radialAngle) * width * 0.38f, center.y + sin(-radialAngle) * height * 0.38f)
    }
    return Offset(
        point.x.coerceIn(bounds.left, bounds.right),
        point.y.coerceIn(bounds.top, bounds.bottom),
    )
}

private fun stableUnit(themeId: String, particleIndex: Int, salt: Int): Float {
    var hash = -0x7ee3623b
    themeId.forEach { character ->
        hash = (hash xor character.code) * 0x01000193
    }
    hash = (hash xor particleIndex) * 0x01000193
    hash = (hash xor salt) * 0x01000193
    return ((hash.toLong() and 0xffff_ffffL) / 0xffff_ffffL.toDouble()).toFloat()
}

private fun DrawScope.drawEnergyParticle(
    style: ParticleStyle,
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    when (style) {
        ParticleStyle.InkDot -> drawCirclePrimitive(center, radius, color, alpha)
        ParticleStyle.HeartSpark -> drawPetalPrimitive(center, radius, color, 45f, alpha)
        ParticleStyle.SnowFlake -> drawSparkPrimitive(center, radius, color, alpha)
        ParticleStyle.EmberSpark -> drawDiamondPrimitive(center, radius, color, alpha, filled = true)
        ParticleStyle.StarShard -> drawSparkPrimitive(center, radius * 1.2f, color, alpha)
        ParticleStyle.SakuraPetal -> drawPetalPrimitive(center, radius * 1.2f, color, 28f, alpha)
        ParticleStyle.JewelPetal -> drawPetalPrimitive(center, radius, color, -35f, alpha)
        ParticleStyle.CatGlint -> drawCirclePrimitive(center, radius * 0.75f, color, alpha, filled = false)
        ParticleStyle.MintLeaf -> drawLeafPrimitive(center, radius * 2.2f, color, 32f, alpha)
        ParticleStyle.GlyphShard -> drawLinePrimitive(center - Offset(radius, radius), center + Offset(radius, radius), color, 1.5f, alpha)
        ParticleStyle.SilverStar -> drawSparkPrimitive(center, radius * 1.35f, color, alpha)
        ParticleStyle.BlueLeaf -> drawLeafPrimitive(center, radius * 2f, color, -38f, alpha)
        ParticleStyle.SkySpark -> drawDiamondPrimitive(center, radius, color, alpha)
        ParticleStyle.GrassLeaf -> drawLeafPrimitive(center, radius * 2.4f, color, 72f, alpha)
        ParticleStyle.CrimsonShard -> drawCrystalPrimitive(center, radius, color, alpha)
        ParticleStyle.CreamSpark -> drawCirclePrimitive(center, radius, color, alpha, filled = false)
        ParticleStyle.RosePetal -> drawPetalPrimitive(center, radius * 1.3f, color, 90f, alpha)
        ParticleStyle.Bubble -> drawCirclePrimitive(center, radius * 1.15f, color, alpha, filled = false)
        ParticleStyle.PinkDroplet -> drawLeafPrimitive(center, radius * 2f, color, 90f, alpha)
        ParticleStyle.FrostShard -> drawCrystalPrimitive(center, radius * 1.1f, color, alpha)
        ParticleStyle.BlueFlame -> drawWavePrimitive(center - Offset(radius, 0f), radius * 2f, radius * 0.5f, color, 1, alpha)
        ParticleStyle.PetalCursor -> drawPetalPrimitive(center, radius, color, 0f, alpha)
        ParticleStyle.HexSnow -> drawSparkPrimitive(center, radius * 1.1f, color, alpha)
        ParticleStyle.Meteor -> drawLinePrimitive(center - Offset(radius * 2f, radius), center + Offset(radius, 0f), color, 1.4f, alpha)
        ParticleStyle.GoldPetal -> drawPetalPrimitive(center, radius * 1.2f, color, 18f, alpha)
        ParticleStyle.GlowLeaf -> drawLeafPrimitive(center, radius * 2.3f, color, -18f, alpha)
        ParticleStyle.CrystalShard -> drawDiamondPrimitive(center, radius * 1.2f, color, alpha, filled = true)
        ParticleStyle.DataPacket -> drawDiamondPrimitive(center, radius * 0.9f, color, alpha)
        ParticleStyle.SpectrumShard -> drawCrystalPrimitive(center, radius * 1.25f, color, alpha)
        ParticleStyle.IridescentFlake -> drawPetalPrimitive(center, radius, color, 62f, alpha)
        ParticleStyle.FireSpark -> drawSparkPrimitive(center, radius * 1.3f, color, alpha)
        ParticleStyle.JadeSpark -> drawDiamondPrimitive(center, radius, color, alpha, filled = true)
    }
}
