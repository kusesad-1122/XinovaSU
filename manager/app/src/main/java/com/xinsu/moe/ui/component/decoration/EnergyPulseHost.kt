package com.xinsu.moe.ui.component.decoration

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicLong

@Stable
class EnergyPulseHostState internal constructor() {
    private var latestOwner = Long.MIN_VALUE

    var owner: Long by mutableLongStateOf(Long.MIN_VALUE)
        private set
    var progress: Float by mutableFloatStateOf(0f)
        private set

    fun handoff(owner: Long): Float {
        if (owner == this.owner || owner > latestOwner) {
            latestOwner = owner
            this.owner = owner
        }
        return progress
    }

    fun publish(owner: Long, progress: Float) {
        if (owner == this.owner) {
            this.progress = progress.coerceIn(0f, 1f)
        }
    }

    fun releaseIfSettled(owner: Long): Boolean {
        if (owner != this.owner || progress > 0f && progress < 1f) return false
        clearOwner()
        return true
    }

    fun release(owner: Long) {
        if (owner == this.owner) {
            clearOwner()
        }
    }

    private fun clearOwner() {
        owner = Long.MIN_VALUE
        progress = 0f
    }
}

private val energyPulseOwnerCounter = AtomicLong(0L)

internal fun nextEnergyPulseOwner(): Long = energyPulseOwnerCounter.incrementAndGet()

val LocalEnergyPulseHost = compositionLocalOf<EnergyPulseHostState?> { null }
