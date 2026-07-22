package com.xinsu.moe.ui.theme.decoration

import kotlin.math.PI
import kotlin.math.sin

internal object EnergyTimeline {
    const val CompressionEndMillis = 120
    const val ChargeEndMillis = 300
    const val InfusionEndMillis = 500
    const val ResonanceEndMillis = 720
    const val SettleEndMillis = 900
    const val MidPreviewMillis = 450

    fun elapsedMillis(progress: Float): Float =
        progress.coerceIn(0f, 1f) * SettleEndMillis

    fun compressionAt(elapsedMillis: Float): Float = when {
        elapsedMillis <= CompressionEndMillis -> segment(elapsedMillis, 0, CompressionEndMillis)
        elapsedMillis < ChargeEndMillis -> 1f - segment(elapsedMillis, CompressionEndMillis, ChargeEndMillis)
        else -> 0f
    }

    fun chargeAt(elapsedMillis: Float): Float =
        segment(elapsedMillis, CompressionEndMillis, ChargeEndMillis)

    fun trackProgressAt(elapsedMillis: Float): Float =
        segment(elapsedMillis, ChargeEndMillis, InfusionEndMillis)

    fun resonanceProgressAt(elapsedMillis: Float): Float =
        segment(elapsedMillis, InfusionEndMillis, ResonanceEndMillis)

    fun particleProgressAt(elapsedMillis: Float): Float =
        segment(elapsedMillis, InfusionEndMillis, SettleEndMillis)

    fun particleAlphaAt(elapsedMillis: Float): Float = when {
        elapsedMillis <= InfusionEndMillis -> 0f
        elapsedMillis < ResonanceEndMillis -> segment(
            elapsedMillis,
            InfusionEndMillis,
            ResonanceEndMillis,
        )
        else -> 1f - segment(elapsedMillis, ResonanceEndMillis, SettleEndMillis)
    }

    fun settleProgressAt(elapsedMillis: Float): Float =
        segment(elapsedMillis, ResonanceEndMillis, SettleEndMillis)

    fun reducedMotionHighlightAlpha(progress: Float): Float =
        progress.coerceIn(0f, 1f) * ReducedMotionMaxAlpha

    private fun segment(value: Float, startMillis: Int, endMillis: Int): Float =
        ((value - startMillis) / (endMillis - startMillis).toFloat()).coerceIn(0f, 1f)

    private const val ReducedMotionMaxAlpha = 0.20f
}

internal object ActivePulsePolicy {
    fun baselineAlpha(active: Boolean): Float = if (active) ActiveBaselineAlpha else 0f

    fun transientAlpha(active: Boolean, pulseProgress: Float): Float {
        val pulse = pulseProgress.coerceIn(0f, 1f)
        if (pulse <= 0f || pulse >= 1f) return 0f
        val envelope = sin(pulse * PI).toFloat().coerceIn(0f, 1f)
        return envelope * if (active) ActivePulseScale else InactivePulseScale
    }

    private const val ActiveBaselineAlpha = 0.26f
    private const val ActivePulseScale = 0.82f
    private const val InactivePulseScale = 1f
}
