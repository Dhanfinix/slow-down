package com.slowdown.app.logic

import com.slowdown.app.data.DayStats
import com.slowdown.app.data.StatsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrackerAndStatsTest {
    @Test fun trackerFollowsResumeAndPause() {
        val t = ForegroundTracker()
        t.apply(listOf(FgEvent("a", FgEventKind.RESUMED)))
        assertEquals("a", t.current)
        t.apply(listOf(FgEvent("b", FgEventKind.RESUMED), FgEvent("a", FgEventKind.PAUSED)))
        assertEquals("b", t.current) // a pausing late must not clear b
        t.apply(listOf(FgEvent("b", FgEventKind.PAUSED)))
        assertNull(t.current)
    }

    @Test fun screenOffClearsForeground() {
        val t = ForegroundTracker()
        t.apply(listOf(FgEvent("a", FgEventKind.RESUMED), FgEvent("", FgEventKind.SCREEN_OFF)))
        assertNull(t.current)
    }

    @Test fun statsRoundTripAndUpdate() {
        val days = listOf(DayStats("2026-01-01", 5, 1, 7))
        assertEquals(days, StatsCodec.decode(StatsCodec.encode(days)))
        val u = StatsCodec.update(days, "2026-01-02") { it.copy(scrollMs = 9) }
        assertEquals(listOf("2026-01-01", "2026-01-02"), u.map { it.date })
        assertEquals(9, u.last().scrollMs)
        assertEquals(emptyList<DayStats>(), StatsCodec.decode("garbage;1,2"))
    }

    @Test fun lastDaysFillsGaps() {
        val w = StatsCodec.lastDays(listOf(DayStats("2026-01-05", 1, 0, 0)), LocalDate.of(2026, 1, 7))
        assertEquals(7, w.size)
        assertEquals("2026-01-01", w.first().date)
        assertEquals(1, w[4].scrollMs)
        assertEquals(0, w.last().scrollMs)
    }
}
