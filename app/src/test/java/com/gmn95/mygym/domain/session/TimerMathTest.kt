package com.gmn95.mygym.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerMathTest {
    @Test
    fun `active duration uses monotonic time on same boot`() {
        assertEquals(
            17_000,
            TimerMath.activeDurationMillis(
                accumulatedMillis = 5_000,
                anchorElapsedRealtime = 20_000,
                anchorBootCount = 4,
                nowElapsedRealtime = 32_000,
                currentBootCount = 4,
            ),
        )
    }

    @Test
    fun `active duration does not extrapolate across reboot`() {
        assertEquals(
            5_000,
            TimerMath.activeDurationMillis(
                accumulatedMillis = 5_000,
                anchorElapsedRealtime = 20_000,
                anchorBootCount = 4,
                nowElapsedRealtime = 2_000,
                currentBootCount = 5,
            ),
        )
    }

    @Test
    fun `rest countdown reaches zero without becoming negative`() {
        assertEquals(
            0,
            TimerMath.remainingMillis(
                remainingAtAnchorMillis = 10_000,
                anchorElapsedRealtime = 20_000,
                anchorBootCount = 4,
                nowElapsedRealtime = 35_000,
                currentBootCount = 4,
            ),
        )
    }

    @Test
    fun `paused rest keeps persisted remaining duration`() {
        assertEquals(
            9_000,
            TimerMath.remainingMillis(
                remainingAtAnchorMillis = 9_000,
                anchorElapsedRealtime = null,
                anchorBootCount = null,
                nowElapsedRealtime = 40_000,
                currentBootCount = 4,
            ),
        )
    }
}
