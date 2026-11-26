package com.slowdown.app.watch

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.slowdown.app.logic.FgEvent
import com.slowdown.app.logic.FgEventKind
import com.slowdown.app.logic.ForegroundTracker

/** Reads UsageStatsManager events since the last poll and keeps the current foreground package. */
class UsageEventsReader(context: Context) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val tracker = ForegroundTracker()
    private var lastQueryMs: Long? = null

    /** Foreground package now, or null when none, the screen is off or access is missing. */
    fun poll(now: Long): String? {
        val from = lastQueryMs ?: (now - SEED_WINDOW_MS)
        lastQueryMs = now
        val events = mutableListOf<FgEvent>()
        try {
            val result = usm.queryEvents(from, now)
            val e = UsageEvents.Event()
            while (result.hasNextEvent()) {
                result.getNextEvent(e)
                val kind = when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> FgEventKind.RESUMED
                    UsageEvents.Event.ACTIVITY_PAUSED -> FgEventKind.PAUSED
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> FgEventKind.SCREEN_OFF
                    else -> FgEventKind.OTHER
                }
                if (kind != FgEventKind.OTHER) events += FgEvent(e.packageName ?: "", kind)
            }
        } catch (_: SecurityException) {
            return null
        }
        tracker.apply(events)
        return tracker.current
    }

    private companion object {
        const val SEED_WINDOW_MS = 10 * 60_000L
    }
}
