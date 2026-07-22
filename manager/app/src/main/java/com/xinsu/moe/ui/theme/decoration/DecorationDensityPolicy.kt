package com.xinsu.moe.ui.theme.decoration

data class DecorationDensityPolicy(
    val maxParticles: Int,
    val ambientMotion: Boolean,
    val highlightDurationMillis: Int,
) {
    companion object {
        fun forRole(
            role: DecoratedCardRole,
            lowRam: Boolean,
            powerSave: Boolean,
            reduceMotion: Boolean,
        ): DecorationDensityPolicy {
            val baseParticles = when (role) {
                DecoratedCardRole.Hero -> 14
                DecoratedCardRole.Monitor,
                DecoratedCardRole.Function -> 8
                DecoratedCardRole.Standard -> 6
                DecoratedCardRole.Compact -> 4
            }

            if (reduceMotion) return DecorationDensityPolicy(
                maxParticles = 0,
                ambientMotion = false,
                highlightDurationMillis = REDUCED_MOTION_HIGHLIGHT_MILLIS,
            )

            val lowRamBudget = if (lowRam) baseParticles / 2 else baseParticles
            val maxParticles = if (powerSave) {
                minOf(lowRamBudget, POWER_SAVE_MAX_PARTICLES)
            } else {
                lowRamBudget
            }
            return DecorationDensityPolicy(
                maxParticles = maxParticles.coerceIn(0, baseParticles),
                ambientMotion = !lowRam && !powerSave,
                highlightDurationMillis = 0,
            )
        }

        private const val POWER_SAVE_MAX_PARTICLES = 4
        private const val REDUCED_MOTION_HIGHLIGHT_MILLIS = 120
    }
}
