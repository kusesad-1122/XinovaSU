package com.xinsu.moe.ui.theme.decoration

data class EnergyTransitionPlan(
    val from: Float,
    val target: Float,
    val durationMillis: Int,
    val emitParticles: Boolean,
    val resonateCard: Boolean,
    val initializeOnly: Boolean,
)

object EnergyTransitionReducer {
    fun next(
        previousChecked: Boolean?,
        checked: Boolean,
        currentProgress: Float,
        enabled: Boolean,
        reduceMotion: Boolean,
        highlightDurationMillis: Int = REDUCED_MOTION_DURATION_MILLIS,
    ): EnergyTransitionPlan {
        val from = currentProgress.coerceIn(0f, 1f)
        val target = if (checked) 1f else 0f

        if (previousChecked == null) return synchronizedPlan(from, target, initializeOnly = true)
        if (!enabled || previousChecked == checked) return synchronizedPlan(from, target, initializeOnly = false)
        if (reduceMotion) return EnergyTransitionPlan(
            from = from,
            target = target,
            durationMillis = highlightDurationMillis.coerceAtLeast(0),
            emitParticles = false,
            resonateCard = false,
            initializeOnly = false,
        )

        return EnergyTransitionPlan(
            from = from,
            target = target,
            durationMillis = if (checked) ACTIVATION_DURATION_MILLIS else DEACTIVATION_DURATION_MILLIS,
            emitParticles = checked,
            resonateCard = checked,
            initializeOnly = false,
        )
    }

    private fun synchronizedPlan(from: Float, target: Float, initializeOnly: Boolean) = EnergyTransitionPlan(
        from = from,
        target = target,
        durationMillis = 0,
        emitParticles = false,
        resonateCard = false,
        initializeOnly = initializeOnly,
    )

    private const val ACTIVATION_DURATION_MILLIS = 900
    private const val DEACTIVATION_DURATION_MILLIS = 360
    private const val REDUCED_MOTION_DURATION_MILLIS = 120
}
