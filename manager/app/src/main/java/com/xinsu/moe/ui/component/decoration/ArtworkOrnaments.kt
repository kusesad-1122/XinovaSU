package com.xinsu.moe.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.xinsu.moe.ui.theme.decoration.AccentRole
import com.xinsu.moe.ui.theme.decoration.CardDecorationGeometryPolicy
import com.xinsu.moe.ui.theme.decoration.ContentSafeRect
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.EnergyPathStyle
import com.xinsu.moe.ui.theme.decoration.HorizontalTrackGeometry
import com.xinsu.moe.ui.theme.decoration.HorizontalTrackPolicy
import com.xinsu.moe.ui.theme.decoration.LogicalLayoutDirection
import com.xinsu.moe.ui.theme.decoration.OrnamentMotif
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpec
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

internal data class OrnamentGeometry(
    val width: Float,
    val height: Float,
    val corner: Float,
    val edgeInset: Float,
    val contentSafeRect: ContentSafeRect,
    val energyLaneY: Float,
    val showSecondaryOrnaments: Boolean,
    val layoutDirection: LogicalLayoutDirection,
    val frameRhythm: FrameRhythm,
    val pedestalGlyph: PedestalGlyph,
    val ambientGlyph: AmbientGlyph,
    val titleRailGlyph: TitleRailGlyph,
    val particleBounds: ParticleBounds?,
    val energyPath: EnergyPathStyle,
    val horizontalTrack: HorizontalTrackGeometry,
) {
    private val logicalStartX = edgeInset + corner * 0.36f
    private val logicalEndX = width - logicalStartX
    private val physicalStartX = when (layoutDirection) {
        LogicalLayoutDirection.Ltr -> logicalStartX
        LogicalLayoutDirection.Rtl -> logicalEndX
    }
    private val physicalEndX = when (layoutDirection) {
        LogicalLayoutDirection.Ltr -> logicalEndX
        LogicalLayoutDirection.Rtl -> logicalStartX
    }
    val topStartCorner = Offset(physicalStartX, edgeInset + corner * 0.36f)
    val topEndCorner = Offset(physicalEndX, edgeInset + corner * 0.36f)
    val bottomStartCorner = Offset(physicalStartX, height - edgeInset - corner * 0.36f)
    val bottomEndCorner = Offset(physicalEndX, height - edgeInset - corner * 0.36f)
}

internal data class ParticleBounds(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
)

internal enum class FrameRhythm { Horizontal, Vertical, CornerArc, Segmented }
internal enum class PedestalGlyph { Ring, Diamond, Petal, Crystal }
internal enum class AmbientGlyph { Dots, Wave, Spark, Facet }
internal enum class TitleRailGlyph { Line, Ribbon, Gems, Wave }

internal fun ornamentGeometry(
    size: Size,
    spec: ThemeDecorationSpec,
    role: DecoratedCardRole,
    fontScale: Float,
    layoutDirection: LogicalLayoutDirection,
): OrnamentGeometry {
    val shortEdge = min(size.width, size.height).coerceAtLeast(1f)
    val policyGeometry = CardDecorationGeometryPolicy.resolve(
        role = role,
        width = size.width,
        height = size.height,
        safeInsetFraction = spec.layout.safeInsetFraction,
        fontScale = fontScale,
        layoutDirection = layoutDirection,
    )
    val safeCorner = shortEdge * spec.layout.safeInsetFraction.coerceIn(0.08f, 0.18f) *
        policyGeometry.frameScale
    val corner = safeCorner.coerceAtLeast(6f)
    val edgeInset = (shortEdge * policyGeometry.edgeInsetFraction).coerceAtLeast(1f)
    val particleInset = edgeInset + safeCorner * 0.24f
    val particleBounds = if (
        particleInset.isFinite() &&
        size.width.isFinite() &&
        size.height.isFinite() &&
        particleInset <= size.width - particleInset &&
        particleInset <= size.height - particleInset
    ) {
        ParticleBounds(
            left = particleInset,
            right = size.width - particleInset,
            top = particleInset,
            bottom = size.height - particleInset,
        )
    } else {
        null
    }
    val energyPath = resolvedEnergyPath(spec)
    return OrnamentGeometry(
        width = size.width,
        height = size.height,
        corner = corner,
        edgeInset = edgeInset,
        contentSafeRect = policyGeometry.contentSafeRect,
        energyLaneY = size.height * policyGeometry.energyLaneFraction,
        showSecondaryOrnaments = policyGeometry.showSecondaryOrnaments,
        layoutDirection = layoutDirection,
        frameRhythm = FrameRhythm.entries[spec.frame.style.ordinal % FrameRhythm.entries.size],
        pedestalGlyph = PedestalGlyph.entries[
            spec.iconPedestal.style.ordinal % PedestalGlyph.entries.size
        ],
        ambientGlyph = AmbientGlyph.entries[spec.ambient.style.ordinal % AmbientGlyph.entries.size],
        titleRailGlyph = TitleRailGlyph.entries[
            spec.layout.titleRail.ordinal % TitleRailGlyph.entries.size
        ],
        particleBounds = particleBounds,
        energyPath = energyPath,
        horizontalTrack = HorizontalTrackPolicy.resolve(
            path = energyPath,
            width = size.width,
            edgeInset = edgeInset,
            corner = corner,
            direction = spec.energy.direction,
            layoutDirection = layoutDirection,
        ),
    )
}

private val UnitDiamondPath = Path().apply {
    moveTo(0f, -1f)
    lineTo(1f, 0f)
    lineTo(0f, 1f)
    lineTo(-1f, 0f)
    close()
}

private val UnitLeafPath = Path().apply {
    moveTo(-0.5f, 0f)
    quadraticTo(0f, -0.42f, 0.5f, 0f)
    quadraticTo(0f, 0.42f, -0.5f, 0f)
    close()
}

private val UnitPetalPath = Path().apply {
    moveTo(0f, -1f)
    quadraticTo(1f, 0f, 0f, 1f)
    quadraticTo(-1f, 0f, 0f, -1f)
    close()
}

private val UnitPathStroke = Stroke(0.12f)

internal fun DecorationColors.forAccent(role: AccentRole): Color = when (role) {
    AccentRole.Primary -> primary
    AccentRole.Secondary -> secondary
    AccentRole.Tertiary -> tertiary
    AccentRole.Outline -> outline
    AccentRole.Inverse -> inverse
    AccentRole.SurfaceGlow -> surfaceGlow
}

private fun Color.withFraction(alpha: Float): Color = copy(
    alpha = this.alpha * alpha.coerceIn(0f, 1f),
)

internal fun DrawScope.drawLinePrimitive(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float = 1f,
    alpha: Float = 1f,
) {
    drawLine(color.withFraction(alpha), start, end, strokeWidth.coerceAtLeast(0.5f))
}

internal fun DrawScope.drawArcPrimitive(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    strokeWidth: Float = 1f,
    alpha: Float = 1f,
) {
    val r = radius.coerceAtLeast(1f)
    drawArc(
        color = color.withFraction(alpha),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(strokeWidth.coerceAtLeast(0.5f)),
    )
}

internal fun DrawScope.drawCirclePrimitive(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1f,
    filled: Boolean = true,
) {
    if (filled) {
        drawCircle(color.withFraction(alpha), radius.coerceAtLeast(0.5f), center)
    } else {
        drawCircle(
            color = color.withFraction(alpha),
            radius = radius.coerceAtLeast(0.5f),
            center = center,
            style = Stroke((radius * 0.15f).coerceAtLeast(0.5f)),
        )
    }
}

internal fun DrawScope.drawDiamondPrimitive(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1f,
    filled: Boolean = false,
) {
    val r = radius.coerceAtLeast(1f)
    withTransform({
        translate(center.x, center.y)
        scale(r, r, Offset.Zero)
    }) {
        drawPath(
            path = UnitDiamondPath,
            color = color.withFraction(alpha),
            style = if (filled) Fill else UnitPathStroke,
        )
    }
}

internal fun DrawScope.drawLeafPrimitive(
    center: Offset,
    length: Float,
    color: Color,
    rotation: Float = 0f,
    alpha: Float = 1f,
) {
    val l = length.coerceAtLeast(2f)
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, Offset.Zero)
        scale(l, l, Offset.Zero)
    }) {
        drawPath(UnitLeafPath, color.withFraction(alpha))
    }
    withTransform({ rotate(rotation, center) }) {
        drawLinePrimitive(
            Offset(center.x - l * 0.34f, center.y),
            Offset(center.x + l * 0.34f, center.y),
            color,
            alpha = alpha * 0.8f,
        )
    }
}

internal fun DrawScope.drawPetalPrimitive(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float = 0f,
    alpha: Float = 1f,
) {
    val r = radius.coerceAtLeast(1f)
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, Offset.Zero)
        scale(r, r, Offset.Zero)
    }) {
        drawPath(UnitPetalPath, color.withFraction(alpha))
    }
}

internal fun DrawScope.drawRibbonPrimitive(
    start: Offset,
    end: Offset,
    color: Color,
    bend: Float,
    alpha: Float = 1f,
) {
    val control = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f + bend)
    var previous = start
    repeat(12) { index ->
        val t = (index + 1) / 12f
        val inverse = 1f - t
        val next = Offset(
            x = inverse * inverse * start.x + 2f * inverse * t * control.x + t * t * end.x,
            y = inverse * inverse * start.y + 2f * inverse * t * control.y + t * t * end.y,
        )
        drawLinePrimitive(previous, next, color, strokeWidth = 1.6f, alpha = alpha)
        previous = next
    }
    drawCirclePrimitive(start, 1.5f, color, alpha * 0.75f)
    drawCirclePrimitive(end, 1.5f, color, alpha * 0.75f)
}

internal fun DrawScope.drawHalftonePrimitive(
    origin: Offset,
    columns: Int,
    spacing: Float,
    color: Color,
    alpha: Float = 1f,
    horizontalDirection: Float = 1f,
) {
    repeat(columns.coerceIn(1, 5)) { column ->
        repeat(3) { row ->
            drawCirclePrimitive(
                center = origin + Offset(column * spacing * horizontalDirection, row * spacing),
                radius = (spacing * (0.11f + 0.04f * row)).coerceAtLeast(0.7f),
                color = color,
                alpha = alpha * (1f - column * 0.12f),
            )
        }
    }
}

internal fun DrawScope.drawCrystalPrimitive(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1f,
) {
    drawDiamondPrimitive(center, radius, color, alpha)
    drawLinePrimitive(center, Offset(center.x, center.y + radius), color, alpha = alpha * 0.7f)
    drawLinePrimitive(center, Offset(center.x + radius, center.y), color, alpha = alpha * 0.45f)
}

internal fun DrawScope.drawSparkPrimitive(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1f,
) {
    val r = radius.coerceAtLeast(1f)
    drawLinePrimitive(Offset(center.x - r, center.y), Offset(center.x + r, center.y), color, alpha = alpha)
    drawLinePrimitive(Offset(center.x, center.y - r), Offset(center.x, center.y + r), color, alpha = alpha)
    drawDiamondPrimitive(center, r * 0.22f, color, alpha, filled = true)
}

internal fun DrawScope.drawWavePrimitive(
    start: Offset,
    width: Float,
    amplitude: Float,
    color: Color,
    cycles: Int = 2,
    alpha: Float = 1f,
) {
    val segments = 16
    var previous = start
    repeat(segments) { index ->
        val fraction = (index + 1) / segments.toFloat()
        val next = Offset(
            x = start.x + width * fraction,
            y = start.y + sin(fraction * cycles * 2f * PI).toFloat() * amplitude,
        )
        drawLinePrimitive(previous, next, color, alpha = alpha)
        previous = next
    }
}

internal fun DrawScope.drawArtworkOrnament(
    motif: OrnamentMotif,
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
) {
    when (motif) {
        OrnamentMotif.InkPanels -> drawInkPanels(colors, geometry, spec)
        OrnamentMotif.HeartRibbon -> drawHeartRibbon(colors, geometry, spec)
        OrnamentMotif.FrostScarf -> drawFrostScarf(colors, geometry, spec)
        OrnamentMotif.IronWind -> drawIronWind(colors, geometry, spec)
        OrnamentMotif.CloudSlope -> drawCloudSlope(colors, geometry, spec)
        OrnamentMotif.SakuraStreet -> drawSakuraStreet(colors, geometry, spec)
        OrnamentMotif.SakuraCrown -> drawSakuraCrown(colors, geometry, spec)
        OrnamentMotif.CatCourtyard -> drawCatCourtyard(colors, geometry, spec)
        OrnamentMotif.MintPull -> drawMintPull(colors, geometry, spec)
        OrnamentMotif.InkPoster -> drawInkPoster(colors, geometry, spec)
        OrnamentMotif.SilverMoon -> drawSilverMoon(colors, geometry, spec)
        OrnamentMotif.CobaltDress -> drawCobaltDress(colors, geometry, spec)
        OrnamentMotif.SkyRibbon -> drawSkyRibbon(colors, geometry, spec)
        OrnamentMotif.Windfield -> drawWindfield(colors, geometry, spec)
        OrnamentMotif.CrimsonFocus -> drawCrimsonFocus(colors, geometry, spec)
        OrnamentMotif.CreamStreet -> drawCreamStreet(colors, geometry, spec)
        OrnamentMotif.BlackRose -> drawBlackRose(colors, geometry, spec)
        OrnamentMotif.SeaBreeze -> drawSeaBreeze(colors, geometry, spec)
        OrnamentMotif.PinkMist -> drawPinkMist(colors, geometry, spec)
        OrnamentMotif.FrostCrimson -> drawFrostCrimson(colors, geometry, spec)
        OrnamentMotif.BlueFlameCat -> drawBlueFlameCat(colors, geometry, spec)
        OrnamentMotif.VisualNovel,
        OrnamentMotif.SnowWindow,
        OrnamentMotif.MoonOrbit,
        OrnamentMotif.SakuraFan,
        OrnamentMotif.MintVine,
        OrnamentMotif.LavenderCrystal,
        OrnamentMotif.CyberCircuit,
        OrnamentMotif.ObsidianFacet,
        OrnamentMotif.MicaLayer,
        OrnamentMotif.EmberForge,
        OrnamentMotif.JadeCloud -> Unit
    }
}

private fun motifAlpha(spec: ThemeDecorationSpec): Float =
    (0.20f + spec.ambient.amplitude * 0.9f).coerceIn(0.24f, 0.48f)

private fun DrawScope.drawInkPanels(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.edgeInset, g.corner), Offset(g.width - g.edgeInset, g.corner), colors.outline, alpha = motifAlpha(spec))
    drawDiamondPrimitive(g.topStartCorner, g.corner * 0.24f, colors.primary, alpha = 0.42f)
    drawHalftonePrimitive(g.bottomEndCorner - Offset(g.corner, g.corner), 4, g.corner * 0.18f, colors.secondary, 0.24f)
}

private fun DrawScope.drawHeartRibbon(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawRibbonPrimitive(g.topStartCorner, g.topEndCorner, colors.secondary, g.corner * 0.55f, motifAlpha(spec))
    drawPetalPrimitive(g.bottomEndCorner, g.corner * 0.28f, colors.primary, 42f, 0.42f)
    drawCirclePrimitive(g.bottomEndCorner - Offset(g.corner * 0.4f, 0f), g.corner * 0.12f, colors.tertiary, 0.35f)
}

private fun DrawScope.drawFrostScarf(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawCrystalPrimitive(g.topEndCorner, g.corner * 0.34f, colors.tertiary, motifAlpha(spec))
    drawRibbonPrimitive(g.bottomStartCorner, g.bottomEndCorner, colors.primary, -g.corner * 0.4f, 0.32f)
    drawSparkPrimitive(g.bottomStartCorner, g.corner * 0.20f, colors.inverse, 0.46f)
}

private fun DrawScope.drawIronWind(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.edgeInset, g.edgeInset), Offset(g.edgeInset, g.height - g.edgeInset), colors.outline, 2f, motifAlpha(spec))
    drawSparkPrimitive(g.topEndCorner, g.corner * 0.24f, colors.primary, 0.44f)
    drawWavePrimitive(Offset(g.width - g.corner * 2f, g.height - g.corner), g.corner * 1.5f, g.corner * 0.12f, colors.secondary, 3, 0.30f)
}

private fun DrawScope.drawCloudSlope(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawArcPrimitive(g.bottomStartCorner, g.corner * 0.75f, 205f, 120f, colors.primary, alpha = motifAlpha(spec))
    drawCirclePrimitive(g.topEndCorner, g.corner * 0.19f, colors.tertiary, 0.36f, filled = false)
    drawWavePrimitive(Offset(g.edgeInset, g.height - g.corner * 0.55f), g.corner * 2.2f, g.corner * 0.14f, colors.secondary, 2, 0.28f)
}

private fun DrawScope.drawSakuraStreet(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.edgeInset, g.height - g.edgeInset), Offset(g.width - g.edgeInset, g.height - g.edgeInset), colors.outline, alpha = motifAlpha(spec))
    drawPetalPrimitive(g.topStartCorner, g.corner * 0.30f, colors.primary, -28f, 0.42f)
    drawHalftonePrimitive(g.bottomEndCorner - Offset(g.corner * 1.2f, g.corner * 0.5f), 3, g.corner * 0.20f, colors.secondary, 0.22f)
}

private fun DrawScope.drawSakuraCrown(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawArcPrimitive(g.topEndCorner, g.corner * 0.62f, 170f, 190f, colors.tertiary, 1.4f, motifAlpha(spec))
    drawDiamondPrimitive(g.topEndCorner, g.corner * 0.25f, colors.primary, 0.45f, filled = true)
    drawPetalPrimitive(g.bottomStartCorner, g.corner * 0.28f, colors.secondary, 35f, 0.34f)
}

private fun DrawScope.drawCatCourtyard(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLeafPrimitive(g.topStartCorner, g.corner * 0.72f, colors.secondary, 28f, motifAlpha(spec))
    drawCirclePrimitive(g.bottomEndCorner, g.corner * 0.24f, colors.primary, 0.38f, filled = false)
    drawLinePrimitive(g.bottomEndCorner - Offset(g.corner * 0.25f, g.corner * 0.35f), g.bottomEndCorner, colors.outline, alpha = 0.40f)
}

private fun DrawScope.drawMintPull(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawRibbonPrimitive(g.topEndCorner - Offset(g.corner, 0f), g.bottomEndCorner, colors.primary, g.corner * 0.35f, motifAlpha(spec))
    drawLeafPrimitive(g.bottomStartCorner, g.corner * 0.68f, colors.tertiary, -38f, 0.42f)
    drawDiamondPrimitive(g.topStartCorner, g.corner * 0.20f, colors.secondary, 0.34f)
}

private fun DrawScope.drawInkPoster(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.corner, g.edgeInset), Offset(g.width - g.corner, g.edgeInset), colors.inverse, 2f, motifAlpha(spec))
    drawHalftonePrimitive(g.topStartCorner, 5, g.corner * 0.16f, colors.outline, 0.24f)
    drawRibbonPrimitive(g.bottomStartCorner, g.bottomEndCorner, colors.primary, g.corner * 0.18f, 0.32f)
}

private fun DrawScope.drawSilverMoon(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawArcPrimitive(g.topStartCorner, g.corner * 0.48f, 60f, 255f, colors.inverse, 1.5f, motifAlpha(spec))
    drawSparkPrimitive(g.topEndCorner, g.corner * 0.20f, colors.tertiary, 0.48f)
    drawLeafPrimitive(g.bottomEndCorner, g.corner * 0.65f, colors.secondary, 145f, 0.30f)
}

private fun DrawScope.drawCobaltDress(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawRibbonPrimitive(g.topStartCorner, g.bottomStartCorner, colors.primary, -g.corner * 0.42f, motifAlpha(spec))
    drawPetalPrimitive(g.topEndCorner, g.corner * 0.31f, colors.secondary, 90f, 0.40f)
    drawArcPrimitive(g.bottomEndCorner, g.corner * 0.55f, 190f, 110f, colors.outline, alpha = 0.30f)
}

private fun DrawScope.drawSkyRibbon(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.edgeInset, g.corner), g.width - g.edgeInset * 2f, g.corner * 0.12f, colors.primary, 2, motifAlpha(spec))
    drawRibbonPrimitive(g.topEndCorner, g.bottomEndCorner, colors.secondary, g.corner * 0.30f, 0.34f)
    drawSparkPrimitive(g.bottomStartCorner, g.corner * 0.18f, colors.surfaceGlow, 0.42f)
}

private fun DrawScope.drawWindfield(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.edgeInset, g.height - g.corner), g.corner * 2.5f, g.corner * 0.16f, colors.secondary, 3, motifAlpha(spec))
    drawLeafPrimitive(g.topEndCorner, g.corner * 0.74f, colors.primary, -20f, 0.38f)
    drawLinePrimitive(g.topStartCorner, g.topStartCorner + Offset(g.corner, g.corner * 0.24f), colors.outline, alpha = 0.30f)
}

private fun DrawScope.drawCrimsonFocus(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawCirclePrimitive(g.topEndCorner, g.corner * 0.43f, colors.primary, motifAlpha(spec), filled = false)
    drawLinePrimitive(g.topEndCorner - Offset(g.corner * 0.6f, 0f), g.topEndCorner + Offset(g.corner * 0.6f, 0f), colors.inverse, alpha = 0.38f)
    drawDiamondPrimitive(g.bottomStartCorner, g.corner * 0.24f, colors.tertiary, 0.38f)
}

private fun DrawScope.drawCreamStreet(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.edgeInset, g.corner * 0.55f), Offset(g.width - g.edgeInset, g.corner * 0.55f), colors.outline, alpha = motifAlpha(spec))
    drawCirclePrimitive(g.bottomStartCorner, g.corner * 0.22f, colors.surfaceGlow, 0.42f)
    drawHalftonePrimitive(g.topEndCorner - Offset(g.corner, 0f), 3, g.corner * 0.18f, colors.secondary, 0.24f)
}

private fun DrawScope.drawBlackRose(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawPetalPrimitive(g.topStartCorner, g.corner * 0.38f, colors.primary, 45f, motifAlpha(spec))
    drawLeafPrimitive(g.topStartCorner + Offset(g.corner * 0.45f, g.corner * 0.42f), g.corner * 0.72f, colors.tertiary, 42f, 0.34f)
    drawWavePrimitive(Offset(g.width - g.corner * 2.2f, g.height - g.corner * 0.5f), g.corner * 1.8f, g.corner * 0.10f, colors.outline, 4, 0.28f)
}

private fun DrawScope.drawSeaBreeze(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.edgeInset, g.height - g.corner * 0.45f), g.width - g.edgeInset * 2f, g.corner * 0.12f, colors.primary, 3, motifAlpha(spec))
    drawCirclePrimitive(g.topEndCorner, g.corner * 0.20f, colors.secondary, 0.34f, filled = false)
    drawRibbonPrimitive(g.topStartCorner, g.topEndCorner, colors.tertiary, -g.corner * 0.22f, 0.28f)
}

private fun DrawScope.drawPinkMist(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawHalftonePrimitive(g.topStartCorner, 4, g.corner * 0.20f, colors.secondary, motifAlpha(spec))
    drawArcPrimitive(g.bottomEndCorner, g.corner * 0.64f, 180f, 120f, colors.primary, alpha = 0.32f)
    drawPetalPrimitive(g.topEndCorner, g.corner * 0.26f, colors.surfaceGlow, -35f, 0.36f)
}

private fun DrawScope.drawFrostCrimson(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawCrystalPrimitive(g.bottomStartCorner, g.corner * 0.38f, colors.inverse, motifAlpha(spec))
    drawDiamondPrimitive(g.topEndCorner, g.corner * 0.26f, colors.primary, 0.44f, filled = true)
    drawLinePrimitive(Offset(g.edgeInset, g.edgeInset), Offset(g.width - g.edgeInset, g.edgeInset), colors.outline, alpha = 0.30f)
}

private fun DrawScope.drawBlueFlameCat(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.width - g.corner * 2f, g.edgeInset), g.corner * 1.6f, g.corner * 0.18f, colors.primary, 2, motifAlpha(spec))
    drawSparkPrimitive(g.bottomEndCorner, g.corner * 0.27f, colors.tertiary, 0.48f)
    drawCirclePrimitive(g.topStartCorner, g.corner * 0.22f, colors.inverse, 0.34f, filled = false)
}
