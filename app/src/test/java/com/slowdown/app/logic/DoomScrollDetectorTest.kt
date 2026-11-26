package com.slowdown.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoomScrollDetectorTest {
    private val min = 60_000L
    private val cfg = DetectorConfig(thresholdMs = 20 * min, cooldownMs = 5 * min)

    /** Ticks every 15 s from [from] to [to] (ms) and returns every non-NONE decision with its time. */
    private fun run(d: DoomScrollDetector, from: Long, to: Long, watched: Boolean, quiet: Boolean = false): List<Pair<Long, Decision>> {
        val out = mutableListOf<Pair<Long, Decision>>()
        var t = from
        while (t <= to) {
            val r = d.onTick(t, watched, quiet, cfg)
            if (r != Decision.NONE) out += t to r
            t += 15_000
        }
        return out
    }

    @Test fun startsExactlyAtThreshold() {
        val d = DoomScrollDetector()
        val r = run(d, 0, 25 * min, watched = true)
        assertEquals(listOf(20 * min to Decision.START), r)
    }

    @Test fun noStartBelowThreshold() {
        val d = DoomScrollDetector()
        assertTrue(run(d, 0, 19 * min, watched = true).isEmpty())
    }

    @Test fun shortGapKeepsSession() {
        val d = DoomScrollDetector()
        run(d, 0, 10 * min, true)
        run(d, 10 * min + 15_000, 11 * min, false) // 1 min away
        val r = run(d, 11 * min + 15_000, 25 * min, true)
        assertEquals(20 * min, r.single().first) // session start was t=0
    }

    @Test fun longGapResetsSession() {
        val d = DoomScrollDetector()
        run(d, 0, 10 * min, true)
        run(d, 10 * min + 15_000, 14 * min, false) // 4 min away, over the 2 min gap
        val r = run(d, 14 * min + 15_000, 40 * min, true)
        assertEquals(14 * min + 15_000 + 20 * min, r.single().first) // new session
    }

    @Test fun stopsAfterCooldownAway() {
        val d = DoomScrollDetector()
        run(d, 0, 20 * min, true) // START at 20 min
        val r = run(d, 20 * min + 15_000, 40 * min, false)
        assertEquals(listOf(25 * min to Decision.STOP), r) // last watched at 20 min, +5 min
        assertFalse(d.throttling)
    }

    @Test fun returningInsideCooldownKeepsThrottle() {
        val d = DoomScrollDetector()
        run(d, 0, 20 * min, true)
        run(d, 20 * min + 15_000, 23 * min, false) // 3 min away
        val r = run(d, 23 * min + 15_000, 30 * min, true)
        assertTrue(r.isEmpty()) // neither START again nor STOP
        assertTrue(d.throttling)
    }

    @Test fun quietHoursBlockStartButNotLater() {
        val d = DoomScrollDetector()
        assertTrue(run(d, 0, 30 * min, true, quiet = true).isEmpty())
        val r = run(d, 30 * min + 15_000, 31 * min, true, quiet = false)
        assertEquals(Decision.START, r.single().second) // session is already long
    }

    @Test fun afterManualStopDoesNotRestartInSameSession() {
        val d = DoomScrollDetector()
        run(d, 0, 20 * min, true)
        d.onThrottleEnded()
        assertTrue(run(d, 20 * min + 15_000, 40 * min, true).isEmpty())
    }

    @Test fun quietWrapsMidnight() {
        assertTrue(isQuiet(true, 23 * 60, 22 * 60, 7 * 60))
        assertTrue(isQuiet(true, 3 * 60, 22 * 60, 7 * 60))
        assertFalse(isQuiet(true, 12 * 60, 22 * 60, 7 * 60))
        assertFalse(isQuiet(false, 23 * 60, 22 * 60, 7 * 60))
        assertTrue(isQuiet(true, 13 * 60, 12 * 60, 14 * 60))
    }
}
