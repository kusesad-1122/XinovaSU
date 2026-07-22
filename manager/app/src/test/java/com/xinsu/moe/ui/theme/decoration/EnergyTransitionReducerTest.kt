package com.xinsu.moe.ui.theme.decoration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnergyTransitionReducerTest {
    @Test
    fun `first checked composition syncs without burst`() {
        val plan = EnergyTransitionReducer.next(null, true, 1f, enabled = true, reduceMotion = false)

        assertEquals(1f, plan.target)
        assertEquals(0, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
        assertTrue(plan.initializeOnly)
    }

    @Test
    fun `activation uses 900 ms and card resonance`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = true, reduceMotion = false)

        assertEquals(0f, plan.from)
        assertEquals(1f, plan.target)
        assertEquals(900, plan.durationMillis)
        assertTrue(plan.emitParticles)
        assertTrue(plan.resonateCard)
        assertFalse(plan.initializeOnly)
    }

    @Test
    fun `deactivation uses 360 ms without activation burst`() {
        val plan = EnergyTransitionReducer.next(true, false, 1f, enabled = true, reduceMotion = false)

        assertEquals(0f, plan.target)
        assertEquals(360, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
        assertFalse(plan.initializeOnly)
    }

    @Test
    fun `rapid reversal starts from current progress`() {
        val plan = EnergyTransitionReducer.next(true, false, 0.62f, enabled = true, reduceMotion = false)

        assertEquals(0.62f, plan.from)
        assertEquals(0f, plan.target)
    }

    @Test
    fun `disabled changes synchronize without emission`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = false, reduceMotion = false)

        assertEquals(1f, plan.target)
        assertEquals(0, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
        assertFalse(plan.initializeOnly)
    }

    @Test
    fun `reduced motion uses 120 ms static highlight`() {
        val plan = EnergyTransitionReducer.next(false, true, 0f, enabled = true, reduceMotion = true)

        assertEquals(120, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
        assertFalse(plan.initializeOnly)
    }

    @Test
    fun `reduced motion consumes the density highlight duration`() {
        val plan = EnergyTransitionReducer.next(
            previousChecked = false,
            checked = true,
            currentProgress = 0f,
            enabled = true,
            reduceMotion = true,
            highlightDurationMillis = 84,
        )

        assertEquals(84, plan.durationMillis)
        assertFalse(plan.emitParticles)
        assertFalse(plan.resonateCard)
    }
}
