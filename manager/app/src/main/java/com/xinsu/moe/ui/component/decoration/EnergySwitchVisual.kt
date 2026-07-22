package com.xinsu.moe.ui.component.decoration

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.xinsu.moe.ui.theme.LocalThemeDecorationSpec
import com.xinsu.moe.ui.theme.decoration.DecoratedCardRole
import com.xinsu.moe.ui.theme.decoration.DecorationDensityPolicy
import com.xinsu.moe.ui.theme.decoration.EnergyTimeline
import com.xinsu.moe.ui.theme.decoration.EnergyTransitionReducer
import com.xinsu.moe.ui.theme.decoration.LogicalLayoutDirection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal val LocalEnergyPreviewElapsedMillis = compositionLocalOf<Int?> { null }

@Composable
fun EnergySwitchVisual(
    checked: Boolean,
    enabled: Boolean,
    colors: DecorationColors,
    modifier: Modifier = Modifier,
    switch: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val spec = LocalThemeDecorationSpec.current
    val pulseHost = LocalEnergyPulseHost.current
    val previewElapsedMillis = LocalEnergyPreviewElapsedMillis.current?.coerceIn(
        0,
        EnergyTimeline.SettleEndMillis,
    )
    val fontScale = LocalDensity.current.fontScale
    val layoutDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Ltr -> LogicalLayoutDirection.Ltr
        LayoutDirection.Rtl -> LogicalLayoutDirection.Rtl
    }
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    var previousChecked by remember { mutableStateOf<Boolean?>(null) }
    var particleOwner by remember { mutableLongStateOf(NoParticleOwner) }
    val pulseOwner = remember(pulseHost) { mutableLongStateOf(NoParticleOwner) }

    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    val densityPolicy = remember(lowRam, powerSave, reduceMotion) {
        DecorationDensityPolicy.forRole(
            role = DecoratedCardRole.Compact,
            lowRam = lowRam,
            powerSave = powerSave,
            reduceMotion = reduceMotion,
        )
    }
    val trackOnlyPolicy = remember(densityPolicy) {
        densityPolicy.copy(maxParticles = 0)
    }

    DisposableEffect(pulseHost) {
        onDispose {
            pulseHost?.release(pulseOwner.longValue)
            pulseOwner.longValue = NoParticleOwner
        }
    }

    LaunchedEffect(
        checked,
        enabled,
        reduceMotion,
        previewElapsedMillis,
        densityPolicy.highlightDurationMillis,
        pulseHost,
    ) {
        val owner = nextEnergyPulseOwner()
        val retainedPulse = pulseHost?.handoff(owner) ?: 0f
        if (pulseHost != null) pulseOwner.longValue = owner
        try {
            if (previewElapsedMillis != null) {
                previousChecked = checked
                particleOwner = NoParticleOwner
                coroutineScope {
                    launch {
                        progress.snapTo(
                            previewElapsedMillis.toFloat() / EnergyTimeline.SettleEndMillis,
                        )
                    }
                    if (pulseHost != null && retainedPulse.isTransientPulse()) {
                        launch {
                            animateRetainedPulseToRest(
                                pulseHost = pulseHost,
                                owner = owner,
                                retainedProgress = retainedPulse,
                                durationMillis = pulseDecayDurationMillis(
                                    reduceMotion = reduceMotion,
                                    highlightDurationMillis = densityPolicy.highlightDurationMillis,
                                ),
                            )
                        }
                    }
                }
                return@LaunchedEffect
            }

            val plan = EnergyTransitionReducer.next(
                previousChecked = previousChecked,
                checked = checked,
                currentProgress = progress.value,
                enabled = enabled,
                reduceMotion = reduceMotion,
                highlightDurationMillis = densityPolicy.highlightDurationMillis,
            )
            previousChecked = checked
            particleOwner = if (plan.emitParticles) owner else NoParticleOwner

            coroutineScope {
                launch {
                    if (plan.durationMillis == 0) {
                        progress.snapTo(plan.target)
                    } else {
                        progress.animateTo(
                            targetValue = plan.target,
                            animationSpec = tween(
                                durationMillis = plan.durationMillis,
                                easing = LinearEasing,
                            ),
                        ) {
                            if (plan.resonateCard) {
                                pulseHost?.publish(owner, value)
                            }
                        }
                        if (plan.resonateCard) {
                            pulseHost?.publish(owner, progress.value)
                        }
                    }
                }
                if (!plan.resonateCard && pulseHost != null && retainedPulse.isTransientPulse()) {
                    launch {
                        animateRetainedPulseToRest(
                            pulseHost = pulseHost,
                            owner = owner,
                            retainedProgress = retainedPulse,
                            durationMillis = if (plan.durationMillis > 0) {
                                plan.durationMillis
                            } else {
                                pulseDecayDurationMillis(
                                    reduceMotion = reduceMotion,
                                    highlightDurationMillis = densityPolicy.highlightDurationMillis,
                                )
                            },
                        )
                    }
                }
            }
        } finally {
            if (particleOwner == owner) {
                particleOwner = NoParticleOwner
            }
            val released = pulseHost?.releaseIfSettled(owner) == true
            if (released && pulseOwner.longValue == owner) {
                pulseOwner.longValue = NoParticleOwner
            }
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            val geometry = ornamentGeometry(
                size = size,
                spec = spec,
                role = DecoratedCardRole.Compact,
                fontScale = fontScale,
                layoutDirection = layoutDirection,
            )
            val particleSeeds = EnergyParticleSeeds.create(spec, densityPolicy.maxParticles)
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val minDimension = size.minDimension

            onDrawWithContent {
                val contentDrawScope = this
                val value = progress.value.coerceIn(0f, 1f)
                val elapsedMillis = previewElapsedMillis?.toFloat()
                    ?: EnergyTimeline.elapsedMillis(value)
                val compression = if (reduceMotion) {
                    0f
                } else {
                    EnergyTimeline.compressionAt(elapsedMillis)
                }
                val charge = EnergyTimeline.chargeAt(elapsedMillis)
                val trackProgress = EnergyTimeline.trackProgressAt(elapsedMillis)
                val resonance = EnergyTimeline.resonanceProgressAt(elapsedMillis)
                val particleProgress = EnergyTimeline.particleProgressAt(elapsedMillis)
                val particleAlpha = EnergyTimeline.particleAlphaAt(elapsedMillis)
                val settle = EnergyTimeline.settleProgressAt(elapsedMillis)
                val glowRadius = minDimension * (0.34f + settle * 0.16f)
                val contentScale = 1f - compression * 0.055f
                val enabledAlpha = if (enabled) 1f else DisabledVisualAlpha
                val ornamentPolicy =
                    if (particleOwner != NoParticleOwner) densityPolicy else trackOnlyPolicy

                if (!reduceMotion && value > 0f) {
                    drawCircle(
                        color = colors.surfaceGlow,
                        radius = glowRadius,
                        center = center,
                        alpha = enabledAlpha * (charge * 0.12f + settle * 0.10f),
                    )
                }
                scale(contentScale, contentScale, center) {
                    contentDrawScope.drawContent()
                }
                if (reduceMotion) {
                    drawReducedMotionHighlight(
                        center = center,
                        radius = minDimension * ReducedMotionHighlightRadiusFraction,
                        color = colors.surfaceGlow,
                        alpha = EnergyTimeline.reducedMotionHighlightAlpha(value) * enabledAlpha,
                    )
                } else {
                    if (trackProgress > 0f || particleAlpha > 0f) {
                        drawEnergyOrnament(
                            spec = spec,
                            colors = colors,
                            trackProgress = trackProgress,
                            particleProgress = particleProgress,
                            particleAlpha = particleAlpha,
                            overallAlpha = enabledAlpha,
                            policy = ornamentPolicy,
                            geometry = geometry,
                            particleSeeds = particleSeeds,
                        )
                    }
                    if (resonance > 0f && resonance < 1f) {
                        drawCircle(
                            color = colors.primary,
                            radius = minDimension * (0.38f + resonance * 0.26f),
                            center = center,
                            alpha = enabledAlpha * (1f - resonance) * 0.18f,
                        )
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        switch()
    }
}

private suspend fun animateRetainedPulseToRest(
    pulseHost: EnergyPulseHostState,
    owner: Long,
    retainedProgress: Float,
    durationMillis: Int,
) {
    val decay = Animatable(retainedProgress.coerceIn(0f, 1f))
    decay.animateTo(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = durationMillis.coerceAtLeast(1),
            easing = LinearEasing,
        ),
    ) {
        pulseHost.publish(owner, value)
    }
    pulseHost.publish(owner, 0f)
}

private fun Float.isTransientPulse(): Boolean = this > 0f && this < 1f

private fun pulseDecayDurationMillis(
    reduceMotion: Boolean,
    highlightDurationMillis: Int,
): Int = if (reduceMotion) {
    highlightDurationMillis.coerceAtLeast(1)
} else {
    DefaultPulseDecayMillis
}

private fun DrawScope.drawReducedMotionHighlight(
    center: Offset,
    radius: Float,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    if (alpha <= 0f) return
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        alpha = alpha.coerceIn(0f, 1f),
    )
}

private const val ReducedMotionHighlightRadiusFraction = 0.48f
private const val DisabledVisualAlpha = 0.38f
private const val NoParticleOwner = Long.MIN_VALUE
private const val DefaultPulseDecayMillis = 360
