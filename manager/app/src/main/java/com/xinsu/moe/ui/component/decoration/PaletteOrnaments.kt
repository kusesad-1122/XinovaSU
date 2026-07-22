package com.xinsu.moe.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.xinsu.moe.ui.theme.decoration.OrnamentMotif
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpec

internal fun DrawScope.drawPaletteOrnament(
    motif: OrnamentMotif,
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
) {
    when (motif) {
        OrnamentMotif.InkPanels,
        OrnamentMotif.HeartRibbon,
        OrnamentMotif.FrostScarf,
        OrnamentMotif.IronWind,
        OrnamentMotif.CloudSlope,
        OrnamentMotif.SakuraStreet,
        OrnamentMotif.SakuraCrown,
        OrnamentMotif.CatCourtyard,
        OrnamentMotif.MintPull,
        OrnamentMotif.InkPoster,
        OrnamentMotif.SilverMoon,
        OrnamentMotif.CobaltDress,
        OrnamentMotif.SkyRibbon,
        OrnamentMotif.Windfield,
        OrnamentMotif.CrimsonFocus,
        OrnamentMotif.CreamStreet,
        OrnamentMotif.BlackRose,
        OrnamentMotif.SeaBreeze,
        OrnamentMotif.PinkMist,
        OrnamentMotif.FrostCrimson,
        OrnamentMotif.BlueFlameCat -> Unit
        OrnamentMotif.VisualNovel -> drawVisualNovel(colors, geometry, spec)
        OrnamentMotif.SnowWindow -> drawSnowWindow(colors, geometry, spec)
        OrnamentMotif.MoonOrbit -> drawMoonOrbit(colors, geometry, spec)
        OrnamentMotif.SakuraFan -> drawSakuraFan(colors, geometry, spec)
        OrnamentMotif.MintVine -> drawMintVine(colors, geometry, spec)
        OrnamentMotif.LavenderCrystal -> drawLavenderCrystal(colors, geometry, spec)
        OrnamentMotif.CyberCircuit -> drawCyberCircuit(colors, geometry, spec)
        OrnamentMotif.ObsidianFacet -> drawObsidianFacet(colors, geometry, spec)
        OrnamentMotif.MicaLayer -> drawMicaLayer(colors, geometry, spec)
        OrnamentMotif.EmberForge -> drawEmberForge(colors, geometry, spec)
        OrnamentMotif.JadeCloud -> drawJadeCloud(colors, geometry, spec)
    }
}

private fun paletteAlpha(spec: ThemeDecorationSpec): Float =
    (0.22f + spec.ambient.amplitude * 0.82f).coerceIn(0.26f, 0.48f)

private fun DrawScope.drawVisualNovel(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawRibbonPrimitive(g.bottomStartCorner, g.bottomEndCorner, colors.primary, -g.corner * 0.22f, paletteAlpha(spec))
    drawPetalPrimitive(g.topEndCorner, g.corner * 0.32f, colors.secondary, 32f, 0.42f)
    drawLinePrimitive(Offset(g.corner, g.height - g.edgeInset), Offset(g.width - g.corner, g.height - g.edgeInset), colors.outline, 1.4f, 0.34f)
}

private fun DrawScope.drawSnowWindow(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawCrystalPrimitive(g.topStartCorner, g.corner * 0.40f, colors.tertiary, paletteAlpha(spec))
    drawSparkPrimitive(g.bottomEndCorner, g.corner * 0.25f, colors.inverse, 0.46f)
    drawHalftonePrimitive(g.topEndCorner - Offset(g.corner, 0f), 4, g.corner * 0.18f, colors.primary, 0.22f)
}

private fun DrawScope.drawMoonOrbit(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawArcPrimitive(g.topEndCorner, g.corner * 0.58f, 12f, 320f, colors.primary, 1.4f, paletteAlpha(spec))
    drawCirclePrimitive(g.topEndCorner + Offset(g.corner * 0.52f, 0f), g.corner * 0.12f, colors.secondary, 0.42f)
    drawSparkPrimitive(g.bottomStartCorner, g.corner * 0.20f, colors.surfaceGlow, 0.40f)
}

private fun DrawScope.drawSakuraFan(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawArcPrimitive(g.bottomEndCorner, g.corner * 0.62f, 190f, 135f, colors.tertiary, alpha = paletteAlpha(spec))
    drawPetalPrimitive(g.bottomEndCorner - Offset(g.corner * 0.30f, g.corner * 0.45f), g.corner * 0.28f, colors.primary, -25f, 0.46f)
    drawRibbonPrimitive(g.topStartCorner, g.topEndCorner, colors.secondary, g.corner * 0.28f, 0.30f)
}

private fun DrawScope.drawMintVine(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.edgeInset, g.height - g.corner * 0.55f), g.corner * 2.3f, g.corner * 0.16f, colors.primary, 2, paletteAlpha(spec))
    drawLeafPrimitive(g.bottomStartCorner, g.corner * 0.75f, colors.tertiary, -32f, 0.44f)
    drawCirclePrimitive(g.topEndCorner, g.corner * 0.15f, colors.surfaceGlow, 0.36f, filled = false)
}

private fun DrawScope.drawLavenderCrystal(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawCrystalPrimitive(g.topEndCorner, g.corner * 0.46f, colors.primary, paletteAlpha(spec))
    drawDiamondPrimitive(g.bottomStartCorner, g.corner * 0.26f, colors.secondary, 0.42f, filled = true)
    drawSparkPrimitive(g.topStartCorner, g.corner * 0.22f, colors.tertiary, 0.38f)
}

private fun DrawScope.drawCyberCircuit(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(g.topStartCorner, Offset(g.width - g.corner, g.topStartCorner.y), colors.primary, 1.6f, paletteAlpha(spec))
    drawDiamondPrimitive(g.topEndCorner, g.corner * 0.22f, colors.tertiary, 0.44f)
    drawHalftonePrimitive(g.bottomStartCorner - Offset(0f, g.corner), 5, g.corner * 0.16f, colors.outline, 0.24f)
}

private fun DrawScope.drawObsidianFacet(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawDiamondPrimitive(g.topStartCorner, g.corner * 0.48f, colors.inverse, paletteAlpha(spec))
    drawLinePrimitive(g.topStartCorner, g.bottomEndCorner, colors.outline, alpha = 0.22f)
    drawCrystalPrimitive(g.bottomEndCorner, g.corner * 0.31f, colors.tertiary, 0.40f)
}

private fun DrawScope.drawMicaLayer(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawRibbonPrimitive(g.topStartCorner, g.topEndCorner, colors.surfaceGlow, g.corner * 0.12f, paletteAlpha(spec))
    drawCirclePrimitive(g.bottomStartCorner, g.corner * 0.28f, colors.primary, 0.34f, filled = false)
    drawHalftonePrimitive(g.bottomEndCorner - Offset(g.corner, g.corner), 4, g.corner * 0.19f, colors.secondary, 0.26f)
}

private fun DrawScope.drawEmberForge(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawLinePrimitive(Offset(g.edgeInset, g.height - g.edgeInset), Offset(g.width - g.edgeInset, g.height - g.edgeInset), colors.outline, 2f, paletteAlpha(spec))
    drawSparkPrimitive(g.bottomEndCorner, g.corner * 0.34f, colors.primary, 0.50f)
    drawWavePrimitive(Offset(g.topStartCorner.x, g.edgeInset), g.corner * 1.5f, g.corner * 0.22f, colors.tertiary, 2, 0.34f)
}

private fun DrawScope.drawJadeCloud(colors: DecorationColors, g: OrnamentGeometry, spec: ThemeDecorationSpec) {
    drawWavePrimitive(Offset(g.edgeInset, g.corner), g.width - g.edgeInset * 2f, g.corner * 0.14f, colors.primary, 3, paletteAlpha(spec))
    drawArcPrimitive(g.bottomStartCorner, g.corner * 0.56f, 210f, 115f, colors.tertiary, alpha = 0.38f)
    drawDiamondPrimitive(g.topEndCorner, g.corner * 0.24f, colors.secondary, 0.40f, filled = true)
}
