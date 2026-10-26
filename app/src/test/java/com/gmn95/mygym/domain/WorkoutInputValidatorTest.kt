package com.gmn95.mygym.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutInputValidatorTest {
    @Test
    fun `accepts fixed repetitions`() {
        val configuration = WorkoutItemConfiguration(
            sets = 3,
            repetitionsMin = 10,
            repetitionsMax = null,
            restSeconds = 60,
        )

        assertTrue(WorkoutInputValidator.isValidConfiguration(configuration))
    }

    @Test
    fun `accepts repetition range`() {
        val configuration = WorkoutItemConfiguration(
            sets = 4,
            repetitionsMin = 8,
            repetitionsMax = 12,
            restSeconds = 90,
        )

        assertTrue(WorkoutInputValidator.isValidConfiguration(configuration))
    }

    @Test
    fun `rejects maximum repetitions below minimum`() {
        val configuration = WorkoutItemConfiguration(
            sets = 3,
            repetitionsMin = 12,
            repetitionsMax = 8,
            restSeconds = 60,
        )

        assertFalse(WorkoutInputValidator.isValidConfiguration(configuration))
    }

    @Test
    fun `rejects non-positive sets and repetitions or negative rest`() {
        assertFalse(
            WorkoutInputValidator.isValidConfiguration(
                WorkoutItemConfiguration(0, 10, null, 60),
            ),
        )
        assertFalse(
            WorkoutInputValidator.isValidConfiguration(
                WorkoutItemConfiguration(3, 0, null, 60),
            ),
        )
        assertFalse(
            WorkoutInputValidator.isValidConfiguration(
                WorkoutItemConfiguration(3, 10, null, -1),
            ),
        )
    }

    @Test
    fun `rejects blank names`() {
        assertFalse(WorkoutInputValidator.isValidName("   "))
        assertTrue(WorkoutInputValidator.isValidName("A — Superiores"))
    }
}
