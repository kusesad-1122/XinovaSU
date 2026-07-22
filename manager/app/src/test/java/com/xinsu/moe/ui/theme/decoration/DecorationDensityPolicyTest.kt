package com.xinsu.moe.ui.theme.decoration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecorationDensityPolicyTest {
    @Test
    fun `normal mode assigns the authored particle budgets`() {
        val expected = mapOf(
            DecoratedCardRole.Hero to 14,
            DecoratedCardRole.Monitor to 8,
            DecoratedCardRole.Function to 8,
            DecoratedCardRole.Standard to 6,
            DecoratedCardRole.Compact to 4,
        )

        expected.forEach { (role, particles) ->
            val policy = DecorationDensityPolicy.forRole(role, lowRam = false, powerSave = false, reduceMotion = false)

            assertEquals(particles, policy.maxParticles, role.name)
            assertTrue(policy.ambientMotion, role.name)
            assertEquals(0, policy.highlightDurationMillis, role.name)
        }
    }

    @Test
    fun `low ram halves particles and disables ambient motion`() {
        val policy = DecorationDensityPolicy.forRole(
            DecoratedCardRole.Hero,
            lowRam = true,
            powerSave = false,
            reduceMotion = false,
        )

        assertEquals(7, policy.maxParticles)
        assertFalse(policy.ambientMotion)
        assertEquals(0, policy.highlightDurationMillis)
    }

    @Test
    fun `power save limits particles to four and disables ambient motion`() {
        val policy = DecorationDensityPolicy.forRole(
            DecoratedCardRole.Hero,
            lowRam = false,
            powerSave = true,
            reduceMotion = false,
        )

        assertEquals(4, policy.maxParticles)
        assertFalse(policy.ambientMotion)
        assertEquals(0, policy.highlightDurationMillis)
    }

    @Test
    fun `low ram budget is halved before power save caps every role`() {
        val expected = mapOf(
            DecoratedCardRole.Hero to 4,
            DecoratedCardRole.Monitor to 4,
            DecoratedCardRole.Function to 4,
            DecoratedCardRole.Standard to 3,
            DecoratedCardRole.Compact to 2,
        )

        expected.forEach { (role, particles) ->
            val policy = DecorationDensityPolicy.forRole(
                role,
                lowRam = true,
                powerSave = true,
                reduceMotion = false,
            )

            assertEquals(particles, policy.maxParticles, role.name)
            assertFalse(policy.ambientMotion, role.name)
            assertEquals(0, policy.highlightDurationMillis, role.name)
        }
    }

    @Test
    fun `reduced motion disables particles and retains only a 120 ms highlight`() {
        val policy = DecorationDensityPolicy.forRole(
            DecoratedCardRole.Hero,
            lowRam = false,
            powerSave = false,
            reduceMotion = true,
        )

        assertEquals(0, policy.maxParticles)
        assertFalse(policy.ambientMotion)
        assertEquals(120, policy.highlightDurationMillis)
    }

    @Test
    fun `every constraint combination stays within its role budget`() {
        DecoratedCardRole.entries.forEach { role ->
            val base = DecorationDensityPolicy.forRole(role, lowRam = false, powerSave = false, reduceMotion = false)
            listOf(false, true).forEach { lowRam ->
                listOf(false, true).forEach { powerSave ->
                    listOf(false, true).forEach { reduceMotion ->
                        val policy = DecorationDensityPolicy.forRole(role, lowRam, powerSave, reduceMotion)

                        assertTrue(policy.maxParticles in 0..base.maxParticles, "$role/$lowRam/$powerSave/$reduceMotion")
                        if (lowRam || powerSave || reduceMotion) assertFalse(policy.ambientMotion)
                    }
                }
            }
        }
    }
}
