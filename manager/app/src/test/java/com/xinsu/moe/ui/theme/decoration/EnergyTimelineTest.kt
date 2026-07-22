package com.xinsu.moe.ui.theme.decoration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnergyTimelineTest {
    @Test
    fun `activation boundaries are the authored millisecond values`() {
        assertEquals(120, EnergyTimeline.CompressionEndMillis)
        assertEquals(300, EnergyTimeline.ChargeEndMillis)
        assertEquals(500, EnergyTimeline.InfusionEndMillis)
        assertEquals(720, EnergyTimeline.ResonanceEndMillis)
        assertEquals(900, EnergyTimeline.SettleEndMillis)
    }

    @Test
    fun `outer animation progress is a linear wall clock fraction`() {
        assertEquals(0f, EnergyTimeline.elapsedMillis(0f))
        assertEquals(225f, EnergyTimeline.elapsedMillis(0.25f))
        assertEquals(450f, EnergyTimeline.elapsedMillis(0.5f))
        assertEquals(675f, EnergyTimeline.elapsedMillis(0.75f))
        assertEquals(900f, EnergyTimeline.elapsedMillis(1f))
    }

    @Test
    fun `track particle and settle curves have independent boundaries`() {
        assertEquals(0f, EnergyTimeline.trackProgressAt(300f))
        assertEquals(1f, EnergyTimeline.trackProgressAt(500f))
        assertEquals(1f, EnergyTimeline.trackProgressAt(900f))

        assertEquals(0f, EnergyTimeline.particleProgressAt(500f))
        assertTrue(EnergyTimeline.particleProgressAt(720f) in 0f..0.9999f)
        assertEquals(1f, EnergyTimeline.particleProgressAt(900f))

        assertEquals(0f, EnergyTimeline.particleAlphaAt(500f))
        assertEquals(1f, EnergyTimeline.particleAlphaAt(720f))
        assertEquals(0f, EnergyTimeline.particleAlphaAt(900f))

        assertEquals(0f, EnergyTimeline.settleProgressAt(720f))
        assertEquals(1f, EnergyTimeline.settleProgressAt(900f))
    }

    @Test
    fun `representative mid preview is inside infusion`() {
        val elapsed = EnergyTimeline.MidPreviewMillis.toFloat()

        assertTrue(elapsed in 301f..499f)
        assertTrue(EnergyTimeline.trackProgressAt(elapsed) in 0f..1f)
        assertEquals(0f, EnergyTimeline.particleAlphaAt(elapsed))
    }

    @Test
    fun `active baseline does not suppress a transient pulse`() {
        val baseline = ActivePulsePolicy.baselineAlpha(active = true)
        val pulse = ActivePulsePolicy.transientAlpha(active = true, pulseProgress = 0.6f)

        assertTrue(baseline > 0f)
        assertTrue(pulse > 0f)
        assertEquals(baseline, ActivePulsePolicy.baselineAlpha(active = true))
        assertTrue(baseline + pulse > baseline)
    }

    @Test
    fun `active and inactive pulse envelopes are continuous bounded and zero at both ends`() {
        val epsilon = 0.0001f
        listOf(false, true).forEach { active ->
            val scale = if (active) 0.82f else 1f
            val start = ActivePulsePolicy.transientAlpha(active, 0f)
            val nearStart = ActivePulsePolicy.transientAlpha(active, epsilon)
            val peak = ActivePulsePolicy.transientAlpha(active, 0.5f)
            val nearEnd = ActivePulsePolicy.transientAlpha(active, 1f - epsilon)
            val end = ActivePulsePolicy.transientAlpha(active, 1f)

            assertEquals(0f, start, 0f, "active=$active start")
            assertEquals(0f, end, 0f, "active=$active end")
            assertTrue(nearStart > 0f, "active=$active epsilon")
            assertTrue(nearStart < peak, "active=$active rising")
            assertEquals(nearStart, nearEnd, 0.0001f, "active=$active symmetric")
            assertEquals(scale, peak, 0.0001f, "active=$active peak")
            listOf(start, nearStart, peak, nearEnd, end).forEach { value ->
                assertTrue(value in 0f..scale, "active=$active value=$value")
            }
        }
    }

    @Test
    fun `reduced motion highlight is alpha only over fixed geometry`() {
        assertEquals(0f, EnergyTimeline.reducedMotionHighlightAlpha(0f))
        assertTrue(EnergyTimeline.reducedMotionHighlightAlpha(0.5f) > 0f)
        assertEquals(
            EnergyTimeline.reducedMotionHighlightAlpha(1f),
            EnergyTimeline.reducedMotionHighlightAlpha(2f),
        )
    }
}
