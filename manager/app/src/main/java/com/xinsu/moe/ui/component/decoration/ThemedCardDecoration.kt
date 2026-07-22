package com.xinsu.moe.ui.component.decoration

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.decoration.ActivePulsePolicy
import com.xinsu.moe.ui.theme.decoration.AmbientMotionPolicy
import com.xinsu.moe.ui.theme.decoration.BadgeAnchor
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.DecorationDensityPolicy
import com.xinsu.moe.ui.theme.decoration.EnergyTimeline
import com.xinsu.moe.ui.theme.decoration.LogicalLayoutDirection
import com.xinsu.moe.ui.theme.decoration.ThemeDecorationSpec

@Composable
fun DecoratedCardContent(
    role: DecoratedCardRole,
    colors: DecorationColors,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val spec = LocalThemeDecorationSpec.current
    val pulseHost = remember { EnergyPulseHostState() }
    val fontScale = LocalDensity.current.fontScale
    val layoutDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Ltr -> LogicalLayoutDirection.Ltr
        LayoutDirection.Rtl -> LogicalLayoutDirection.Rtl
    }
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    val policy = remember(role, lowRam, powerSave, reduceMotion) {
        DecorationDensityPolicy.forRole(
            role = role,
            lowRam = lowRam,
            powerSave = powerSave,
            reduceMotion = reduceMotion,
        )
    }
    val ambientProgress = remember(spec.themeId) { Animatable(0f) }
    LaunchedEffect(policy.ambientMotion, spec.ambient.style) {
        if (!policy.ambientMotion) {
            ambientProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val cycleMillis = AmbientMotionPolicy.cycleDurationMillis(spec.ambient.style)
        while (true) {
            ambientProgress.snapTo(0f)
            ambientProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = cycleMillis, easing = LinearEasing),
            )
        }
    }

    CompositionLocalProvider(LocalEnergyPulseHost provides pulseHost) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .drawWithCache {
                    val geometry = ornamentGeometry(
                        size = size,
                        spec = spec,
                        role = role,
                        fontScale = fontScale,
                        layoutDirection = layoutDirection,
                    )
                    val particleSeeds = EnergyParticleSeeds.create(spec, policy.maxParticles)
                    onDrawWithContent {
                        val pulseProgress = pulseHost.progress.coerceIn(0f, 1f)
                        val elapsedMillis = EnergyTimeline.elapsedMillis(pulseProgress)
                        val activeBaselineAlpha = ActivePulsePolicy.baselineAlpha(active)
                        val transientPulseAlpha = ActivePulsePolicy.transientAlpha(active, pulseProgress)
                        drawCardAmbientAndFrame(
                            spec = spec,
                            colors = colors,
                            geometry = geometry,
                            ambientPhase = ambientProgress.value,
                            ambientMotion = policy.ambientMotion,
                        )
                        drawContent()
                        val safe = geometry.contentSafeRect
                        clipRect(
                            left = safe.left,
                            top = safe.top,
                            right = safe.right,
                            bottom = safe.bottom,
                            clipOp = ClipOp.Difference,
                        ) {
                            drawCardForeground(
                                spec = spec,
                                colors = colors,
                                geometry = geometry,
                                active = active,
                                pulseProgress = pulseProgress,
                            )
                            if (activeBaselineAlpha > 0f) {
                                drawEnergyOrnament(
                                    spec = spec,
                                    colors = colors,
                                    trackProgress = 1f,
                                    particleProgress = 0f,
                                    particleAlpha = 0f,
                                    overallAlpha = activeBaselineAlpha,
                                    policy = policy,
                                    geometry = geometry,
                                    particleSeeds = particleSeeds,
                                )
                            }
                            if (transientPulseAlpha > 0f) {
                                drawEnergyOrnament(
                                    spec = spec,
                                    colors = colors,
                                    trackProgress = EnergyTimeline.trackProgressAt(elapsedMillis),
                                    particleProgress = EnergyTimeline.particleProgressAt(elapsedMillis),
                                    particleAlpha = EnergyTimeline.particleAlphaAt(elapsedMillis),
                                    overallAlpha = transientPulseAlpha,
                                    policy = policy,
                                    geometry = geometry,
                                    particleSeeds = particleSeeds,
                                )
                            }
                        }
                    }
                },
            content = content,
        )
    }
}

private fun DrawScope.drawCardAmbientAndFrame(
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
    ambientPhase: Float,
    ambientMotion: Boolean,
) {
    val frameColor = colors.forAccent(spec.frame.accent)
    val cut = geometry.corner * spec.frame.cutCorners.coerceIn(0, 2) * 0.22f
    val frameAlpha = 0.16f + if (spec.frame.insetStroke) 0.05f else 0f
    repeat(spec.frame.lineCount.coerceIn(1, 3)) { index ->
        val inset = geometry.edgeInset + index * 1.8f
        when (geometry.frameRhythm) {
            FrameRhythm.Horizontal -> {
                drawLinePrimitive(
                    Offset(inset + cut, inset),
                    Offset(geometry.width - inset - cut, inset),
                    frameColor,
                    alpha = frameAlpha,
                )
                drawLinePrimitive(
                    Offset(inset + cut, geometry.height - inset),
                    Offset(geometry.width - inset - cut, geometry.height - inset),
                    frameColor,
                    alpha = frameAlpha * 0.75f,
                )
            }
            FrameRhythm.Vertical -> {
                drawLinePrimitive(
                    Offset(inset, inset + cut),
                    Offset(inset, geometry.height - inset - cut),
                    frameColor,
                    alpha = frameAlpha,
                )
                drawLinePrimitive(
                    Offset(geometry.width - inset, inset + cut),
                    Offset(geometry.width - inset, geometry.height - inset - cut),
                    frameColor,
                    alpha = frameAlpha * 0.75f,
                )
            }
            FrameRhythm.CornerArc -> {
                drawArcPrimitive(geometry.topStartCorner, geometry.corner * 0.7f, 180f, 90f, frameColor, alpha = frameAlpha)
                drawArcPrimitive(geometry.bottomEndCorner, geometry.corner * 0.7f, 0f, 90f, frameColor, alpha = frameAlpha)
            }
            FrameRhythm.Segmented -> repeat(4) { segment ->
                val start = geometry.edgeInset + segment * geometry.width * 0.24f
                drawLinePrimitive(
                    Offset(start, inset),
                    Offset((start + geometry.width * 0.14f).coerceAtMost(geometry.width - inset), inset),
                    frameColor,
                    alpha = frameAlpha,
                )
            }
        }
    }

    val motionScale = if (ambientMotion) 1f else 0f
    val driftX = AmbientMotionPolicy.offsetX(
        axis = spec.ambient.driftAxis,
        amplitude = spec.ambient.amplitude,
        phase = ambientPhase,
        layoutDirection = geometry.layoutDirection,
    ) * geometry.corner * motionScale
    val driftY = AmbientMotionPolicy.offsetY(
        axis = spec.ambient.driftAxis,
        amplitude = spec.ambient.amplitude,
        phase = ambientPhase,
    ) * geometry.corner * motionScale
    withTransform({ translate(driftX, driftY) }) {
        drawRecipeAmbient(spec, colors, geometry)
        if (geometry.showSecondaryOrnaments) {
            drawArtworkOrnament(spec.motif, spec, colors, geometry)
            drawPaletteOrnament(spec.motif, spec, colors, geometry)
        }
    }
}

private fun DrawScope.drawRecipeAmbient(
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
) {
    val color = colors.forAccent(spec.accents.firstOrNull() ?: spec.frame.accent)
    val alpha = (0.12f + spec.ambient.amplitude * 0.48f).coerceIn(0.12f, 0.28f)
    when (geometry.ambientGlyph) {
        AmbientGlyph.Dots -> drawHalftonePrimitive(
            geometry.bottomEndCorner - Offset(geometry.corner, geometry.corner),
            3,
            geometry.corner * 0.16f,
            color,
            alpha,
        )
        AmbientGlyph.Wave -> drawWavePrimitive(
            Offset(geometry.edgeInset, geometry.energyLaneY),
            geometry.corner * 2.2f,
            geometry.corner * 0.12f,
            color,
            2,
            alpha,
        )
        AmbientGlyph.Spark -> drawSparkPrimitive(
            geometry.topEndCorner,
            geometry.corner * 0.22f,
            color,
            alpha,
        )
        AmbientGlyph.Facet -> drawDiamondPrimitive(
            geometry.bottomStartCorner,
            geometry.corner * 0.24f,
            color,
            alpha,
        )
    }
}

private fun DrawScope.drawCardForeground(
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
    active: Boolean,
    pulseProgress: Float,
) {
    val emphasis = (
        0.42f + ActivePulsePolicy.baselineAlpha(active) +
            ActivePulsePolicy.transientAlpha(active, pulseProgress) * 0.32f
    ).coerceIn(0f, 1f)
    val pedestal = colors.forAccent(spec.iconPedestal.accent)
    val pedestalCenter = when (spec.layout.badgeAnchor) {
        BadgeAnchor.TopStart -> geometry.topStartCorner
        BadgeAnchor.TopEnd -> geometry.topEndCorner
        BadgeAnchor.CenterStart -> Offset(geometry.topStartCorner.x, geometry.height * 0.5f)
        BadgeAnchor.CenterEnd -> Offset(geometry.topEndCorner.x, geometry.height * 0.5f)
        BadgeAnchor.BottomStart -> geometry.bottomStartCorner
        BadgeAnchor.BottomEnd -> geometry.bottomEndCorner
    }
    repeat(spec.iconPedestal.ringCount.coerceIn(1, 3)) { index ->
        val radius = geometry.corner * (0.16f + index * 0.08f)
        when (geometry.pedestalGlyph) {
            PedestalGlyph.Ring -> drawCirclePrimitive(pedestalCenter, radius, pedestal, emphasis / (index + 1), filled = false)
            PedestalGlyph.Diamond -> drawDiamondPrimitive(pedestalCenter, radius, pedestal, emphasis / (index + 1))
            PedestalGlyph.Petal -> drawPetalPrimitive(pedestalCenter, radius, pedestal, index * 24f, emphasis / (index + 1))
            PedestalGlyph.Crystal -> drawCrystalPrimitive(pedestalCenter, radius, pedestal, emphasis / (index + 1))
        }
    }
    drawTitleRail(spec, colors, geometry, emphasis)
    if (geometry.showSecondaryOrnaments) {
        drawSparkPrimitive(
            center = if (pedestalCenter.x < geometry.width * 0.5f) {
                geometry.topEndCorner
            } else {
                geometry.bottomStartCorner
            },
            radius = geometry.corner * 0.22f,
            color = colors.forAccent(spec.frame.accent),
            alpha = emphasis,
        )
    }
}

private fun DrawScope.drawTitleRail(
    spec: ThemeDecorationSpec,
    colors: DecorationColors,
    geometry: OrnamentGeometry,
    alpha: Float,
) {
    val start = Offset(geometry.topStartCorner.x, geometry.edgeInset + geometry.corner * 0.08f)
    val end = Offset(geometry.topEndCorner.x, geometry.edgeInset + geometry.corner * 0.08f)
    val color = colors.forAccent(spec.frame.accent)
    when (geometry.titleRailGlyph) {
        TitleRailGlyph.Line -> drawLinePrimitive(start, end, color, 1.4f, alpha * 0.55f)
        TitleRailGlyph.Ribbon -> drawRibbonPrimitive(start, end, color, geometry.corner * 0.16f, alpha * 0.52f)
        TitleRailGlyph.Gems -> {
            drawDiamondPrimitive(start, geometry.corner * 0.12f, color, alpha * 0.55f, filled = true)
            drawDiamondPrimitive(end, geometry.corner * 0.12f, color, alpha * 0.55f, filled = true)
        }
        TitleRailGlyph.Wave -> drawWavePrimitive(start, end.x - start.x, geometry.corner * 0.08f, color, 2, alpha * 0.50f)
    }
}
