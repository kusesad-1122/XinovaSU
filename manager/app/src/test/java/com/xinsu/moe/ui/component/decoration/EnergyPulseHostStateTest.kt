package com.xinsu.moe.ui.component.decoration

import com.xinsu.moe.ui.theme.decoration.ActivePulsePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnergyPulseHostStateTest {
    @Test
    fun `publish half handoff decays through quarter to settled release`() {
        val host = EnergyPulseHostState()
        val oldOwner = 1L
        val replacementOwner = 2L

        assertEquals(0f, host.handoff(oldOwner))
        host.publish(oldOwner, 0.5f)
        assertEquals(0.5f, host.handoff(replacementOwner))

        host.release(oldOwner)
        host.publish(oldOwner, 0.9f)
        assertEquals(replacementOwner, host.owner)
        assertEquals(0.5f, host.progress)

        host.publish(replacementOwner, 0.25f)
        assertFalse(host.releaseIfSettled(replacementOwner))
        listOf(false, true).forEach { active ->
            assertTrue(ActivePulsePolicy.transientAlpha(active, host.progress) > 0f)
        }

        host.publish(replacementOwner, 0f)
        listOf(false, true).forEach { active ->
            assertEquals(0f, ActivePulsePolicy.transientAlpha(active, host.progress))
        }
        assertTrue(host.releaseIfSettled(replacementOwner))
        assertEquals(Long.MIN_VALUE, host.owner)
        assertEquals(0f, host.progress)
    }

    @Test
    fun `dispose release clears current owner without replacement`() {
        val host = EnergyPulseHostState()
        val owner = 7L

        host.handoff(owner)
        host.publish(owner, 0.5f)
        host.release(owner)

        assertEquals(Long.MIN_VALUE, host.owner)
        assertEquals(0f, host.progress)
    }
}
