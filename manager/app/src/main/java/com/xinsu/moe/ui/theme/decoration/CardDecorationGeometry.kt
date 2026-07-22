package com.xinsu.moe.ui.theme.decoration

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal enum class LogicalLayoutDirection { Ltr, Rtl }

internal data class DecorationPoint(
    val x: Float,
    val y: Float,
)

internal data class ContentSafeRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    fun contains(point: DecorationPoint): Boolean =
        point.x in left..right && point.y in top..bottom
}

internal data class CardDecorationGeometry(
    val width: Float,
    val height: Float,
    val contentSafeRect: ContentSafeRect,
    val frameScale: Float,
    val edgeInsetFraction: Float,
    val energyLaneFraction: Float,
    val showSecondaryOrnaments: Boolean,
    val layoutDirection: LogicalLayoutDirection,
    private val cornerRadius: Float,
) {
    fun structuralSignature(): String = listOf(
        contentSafeRect,
        frameScale,
        edgeInsetFraction,
        energyLaneFraction,
        showSecondaryOrnaments,
    ).joinToString("|")

    fun badgeCenter(anchor: BadgeAnchor): DecorationPoint {
        val shortEdge = min(width, height).coerceAtLeast(0f)
        val logicalStart = shortEdge * edgeInsetFraction + cornerRadius * 0.36f
        val logicalEnd = width - logicalStart
        val start = logicalX(logicalStart)
        val end = logicalX(logicalEnd)
        val top = shortEdge * edgeInsetFraction + cornerRadius * 0.36f
        val bottom = height - top
        return when (anchor) {
            BadgeAnchor.TopStart -> DecorationPoint(start, top)
            BadgeAnchor.TopEnd -> DecorationPoint(end, top)
            BadgeAnchor.CenterStart -> DecorationPoint(start, height * 0.5f)
            BadgeAnchor.CenterEnd -> DecorationPoint(end, height * 0.5f)
            BadgeAnchor.BottomStart -> DecorationPoint(start, bottom)
            BadgeAnchor.BottomEnd -> DecorationPoint(end, bottom)
        }
    }

    private fun logicalX(x: Float): Float = when (layoutDirection) {
        LogicalLayoutDirection.Ltr -> x
        LogicalLayoutDirection.Rtl -> width - x
    }
}

internal object CardDecorationGeometryPolicy {
    fun resolve(
        role: DecoratedCardRole,
        width: Float,
        height: Float,
        safeInsetFraction: Float,
        fontScale: Float,
        layoutDirection: LogicalLayoutDirection,
    ): CardDecorationGeometry {
        val boundedWidth = width.coerceAtLeast(0f)
        val boundedHeight = height.coerceAtLeast(0f)
        val profile = when (role) {
            DecoratedCardRole.Hero -> RoleProfile(0.12f, 0.18f, 0.16f, 0.12f, 1.35f, 0.025f, 0.88f, true)
            DecoratedCardRole.Monitor -> RoleProfile(0.08f, 0.08f, 0.18f, 0.16f, 0.72f, 0.018f, 0.09f, true)
            DecoratedCardRole.Function -> RoleProfile(0.12f, 0.24f, 0.14f, 0.12f, 1.05f, 0.024f, 0.86f, true)
            DecoratedCardRole.Standard -> RoleProfile(0.10f, 0.20f, 0.12f, 0.10f, 0.88f, 0.026f, 0.12f, true)
            DecoratedCardRole.Compact -> RoleProfile(0.08f, 0.28f, 0.09f, 0.08f, 0.55f, 0.018f, 0.50f, false)
        }
        val largeFont = fontScale >= LargeFontScale
        val contentExpansion = if (largeFont) LargeFontInsetMultiplier else 1f
        val logicalLeft = boundedWidth * profile.startInset * contentExpansion
        val logicalRight = boundedWidth * (1f - profile.endInset * contentExpansion)
        val physicalLeft = when (layoutDirection) {
            LogicalLayoutDirection.Ltr -> logicalLeft
            LogicalLayoutDirection.Rtl -> boundedWidth - logicalRight
        }
        val physicalRight = when (layoutDirection) {
            LogicalLayoutDirection.Ltr -> logicalRight
            LogicalLayoutDirection.Rtl -> boundedWidth - logicalLeft
        }
        val top = boundedHeight * profile.topInset * contentExpansion
        val bottom = boundedHeight * (1f - profile.bottomInset * contentExpansion)
        val shortEdge = min(boundedWidth, boundedHeight).coerceAtLeast(1f)
        val cornerRadius = (
            shortEdge * safeInsetFraction.coerceIn(0.08f, 0.18f) * profile.frameScale
        ).coerceAtLeast(4f)

        return CardDecorationGeometry(
            width = boundedWidth,
            height = boundedHeight,
            contentSafeRect = ContentSafeRect(
                left = minOf(physicalLeft, physicalRight).coerceIn(0f, boundedWidth),
                top = top.coerceIn(0f, boundedHeight),
                right = maxOf(physicalLeft, physicalRight).coerceIn(0f, boundedWidth),
                bottom = bottom.coerceIn(0f, boundedHeight),
            ),
            frameScale = profile.frameScale,
            edgeInsetFraction = profile.edgeInsetFraction,
            energyLaneFraction = profile.energyLaneFraction,
            showSecondaryOrnaments = profile.showSecondaryOrnaments && !largeFont,
            layoutDirection = layoutDirection,
            cornerRadius = cornerRadius,
        )
    }

    private const val LargeFontScale = 1.3f
    private const val LargeFontInsetMultiplier = 0.75f

    private data class RoleProfile(
        val startInset: Float,
        val endInset: Float,
        val topInset: Float,
        val bottomInset: Float,
        val frameScale: Float,
        val edgeInsetFraction: Float,
        val energyLaneFraction: Float,
        val showSecondaryOrnaments: Boolean,
    )
}

internal fun horizontalEnergyFraction(
    direction: EnergyDirection,
    phase: Float,
    towardEnd: Boolean,
    layoutDirection: LogicalLayoutDirection,
): Float {
    val boundedPhase = phase.coerceIn(0f, 1f)
    val logicalFraction = when (direction) {
        EnergyDirection.StartToEnd -> boundedPhase
        EnergyDirection.EndToStart -> 1f - boundedPhase
        EnergyDirection.CenterOut -> if (towardEnd) {
            0.5f + boundedPhase * 0.5f
        } else {
            0.5f - boundedPhase * 0.5f
        }
        EnergyDirection.OutsideIn -> if (towardEnd) {
            1f - boundedPhase * 0.5f
        } else {
            boundedPhase * 0.5f
        }
        EnergyDirection.BottomToTop,
        EnergyDirection.TopToBottom,
        EnergyDirection.Clockwise,
        EnergyDirection.CounterClockwise -> return boundedPhase
    }
    return when (layoutDirection) {
        LogicalLayoutDirection.Ltr -> logicalFraction
        LogicalLayoutDirection.Rtl -> 1f - logicalFraction
    }
}

internal data class HorizontalTrackGeometry(
    val startX: Float,
    private val targetX: Float,
) {
    fun endXAt(progress: Float): Float =
        startX + (targetX - startX) * progress.coerceIn(0f, 1f)

    fun deltaXAt(progress: Float): Float = endXAt(progress) - startX

    fun lengthAt(progress: Float): Float = abs(deltaXAt(progress))
}

internal inline fun HorizontalTrackGeometry.withRenderCoordinatesAt(
    progress: Float,
    block: (startX: Float, endX: Float, deltaX: Float, direction: Float) -> Unit,
) {
    val endX = endXAt(progress)
    val deltaX = endX - startX
    val direction = if (endXAt(1f) < startX) -1f else 1f
    block(startX, endX, deltaX, direction)
}

internal object HorizontalTrackPolicy {
    fun isHorizontal(path: EnergyPathStyle): Boolean = when (path) {
        EnergyPathStyle.BrushSweep,
        EnergyPathStyle.TwinConverge,
        EnergyPathStyle.RailCharge,
        EnergyPathStyle.PawSteps,
        EnergyPathStyle.ElasticConverge,
        EnergyPathStyle.SlashCut,
        EnergyPathStyle.RibbonFlip,
        EnergyPathStyle.FieldSweep,
        EnergyPathStyle.LampMarch,
        EnergyPathStyle.ThornGrow,
        EnergyPathStyle.TideFill,
        EnergyPathStyle.ChoiceConfirm,
        EnergyPathStyle.VineGrow,
        EnergyPathStyle.DataPulse,
        EnergyPathStyle.LayerSweep,
        EnergyPathStyle.FuseBurn -> true
        EnergyPathStyle.CrystalGrow,
        EnergyPathStyle.RisingArc,
        EnergyPathStyle.PetalUpdraft,
        EnergyPathStyle.CrownOrbit,
        EnergyPathStyle.LunarOrbit,
        EnergyPathStyle.BeamDrop,
        EnergyPathStyle.FocusBurst,
        EnergyPathStyle.MistGather,
        EnergyPathStyle.CrystalConverge,
        EnergyPathStyle.FlameSpiral,
        EnergyPathStyle.FrostBloom,
        EnergyPathStyle.PlanetOrbit,
        EnergyPathStyle.PetalFountain,
        EnergyPathStyle.PrismConnect,
        EnergyPathStyle.CoreFracture,
        EnergyPathStyle.QiFlow -> false
    }

    fun resolve(
        path: EnergyPathStyle,
        width: Float,
        edgeInset: Float,
        corner: Float,
        direction: EnergyDirection,
        layoutDirection: LogicalLayoutDirection,
    ): HorizontalTrackGeometry {
        val boundedWidth = width.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val boundedEdgeInset = edgeInset.takeIf { it.isFinite() }
            ?.coerceIn(0f, boundedWidth) ?: 0f
        val boundedCorner = corner.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val startInset = when (path) {
            EnergyPathStyle.TwinConverge,
            EnergyPathStyle.ElasticConverge,
            EnergyPathStyle.RibbonFlip,
            EnergyPathStyle.ChoiceConfirm -> boundedEdgeInset + boundedCorner * 0.36f
            EnergyPathStyle.BrushSweep,
            EnergyPathStyle.CrystalGrow,
            EnergyPathStyle.RailCharge,
            EnergyPathStyle.RisingArc,
            EnergyPathStyle.PetalUpdraft,
            EnergyPathStyle.CrownOrbit,
            EnergyPathStyle.PawSteps,
            EnergyPathStyle.SlashCut,
            EnergyPathStyle.LunarOrbit,
            EnergyPathStyle.BeamDrop,
            EnergyPathStyle.FieldSweep,
            EnergyPathStyle.FocusBurst,
            EnergyPathStyle.LampMarch,
            EnergyPathStyle.ThornGrow,
            EnergyPathStyle.TideFill,
            EnergyPathStyle.MistGather,
            EnergyPathStyle.CrystalConverge,
            EnergyPathStyle.FlameSpiral,
            EnergyPathStyle.FrostBloom,
            EnergyPathStyle.PlanetOrbit,
            EnergyPathStyle.PetalFountain,
            EnergyPathStyle.VineGrow,
            EnergyPathStyle.PrismConnect,
            EnergyPathStyle.DataPulse,
            EnergyPathStyle.CoreFracture,
            EnergyPathStyle.LayerSweep,
            EnergyPathStyle.FuseBurn,
            EnergyPathStyle.QiFlow -> boundedEdgeInset
        }.coerceIn(0f, boundedWidth)
        val reverse = when (direction) {
            EnergyDirection.EndToStart -> true
            EnergyDirection.StartToEnd,
            EnergyDirection.CenterOut,
            EnergyDirection.OutsideIn,
            EnergyDirection.BottomToTop,
            EnergyDirection.TopToBottom,
            EnergyDirection.Clockwise,
            EnergyDirection.CounterClockwise -> false
        }
        val logicalStart = if (reverse) boundedWidth - startInset else startInset
        val targetInset = if (path == EnergyPathStyle.ElasticConverge) {
            startInset
        } else {
            boundedEdgeInset
        }
        val logicalTarget = if (path == EnergyPathStyle.TwinConverge) {
            boundedWidth * 0.5f
        } else if (reverse) {
            targetInset
        } else {
            boundedWidth - targetInset
        }

        fun physicalX(logicalX: Float): Float = when (layoutDirection) {
            LogicalLayoutDirection.Ltr -> logicalX
            LogicalLayoutDirection.Rtl -> boundedWidth - logicalX
        }

        return HorizontalTrackGeometry(
            startX = physicalX(logicalStart),
            targetX = physicalX(logicalTarget),
        )
    }
}

internal object AmbientMotionPolicy {
    fun cycleDurationMillis(style: AmbientStyle): Int =
        (MinCycleMillis + style.ordinal % 7 * 1_000).coerceIn(MinCycleMillis, MaxCycleMillis)

    fun offsetX(
        axis: DriftAxis,
        amplitude: Float,
        phase: Float,
        layoutDirection: LogicalLayoutDirection,
    ): Float {
        val boundedAmplitude = amplitude.coerceIn(0f, 0.35f)
        val radians = phase.coerceIn(0f, 1f) * 2f * PI.toFloat()
        val logical = when (axis) {
            DriftAxis.None,
            DriftAxis.Vertical -> 0f
            DriftAxis.Horizontal,
            DriftAxis.Diagonal -> sin(radians) * boundedAmplitude
            DriftAxis.Radial -> cos(radians) * boundedAmplitude
        }
        return when (layoutDirection) {
            LogicalLayoutDirection.Ltr -> logical
            LogicalLayoutDirection.Rtl -> -logical
        }
    }

    fun offsetY(axis: DriftAxis, amplitude: Float, phase: Float): Float {
        val boundedAmplitude = amplitude.coerceIn(0f, 0.35f)
        val radians = phase.coerceIn(0f, 1f) * 2f * PI.toFloat()
        return when (axis) {
            DriftAxis.None,
            DriftAxis.Horizontal -> 0f
            DriftAxis.Vertical,
            DriftAxis.Diagonal -> sin(radians) * boundedAmplitude
            DriftAxis.Radial -> sin(radians) * boundedAmplitude
        }
    }

    private const val MinCycleMillis = 6_000
    private const val MaxCycleMillis = 12_000
}
