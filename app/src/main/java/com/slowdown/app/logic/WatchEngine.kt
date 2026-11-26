package com.slowdown.app.logic

import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.StatsSink

/**
 * One poll of the watcher. Feeds the foreground package into [DoomScrollDetector] and acts on its
 * decision through [ThrottleCoordinator]. No Android classes, so it runs in JVM tests.
 */
class WatchEngine(
    private val coordinator: ThrottleCoordinator,
    private val stats: StatsSink,
    val detector: DoomScrollDetector = DoomScrollDetector(),
) {
    private var lastTickMs: Long? = null

    /** Minutes of the current continuous session, for the UI and notification. */
    fun sessionMinutes(now: Long): Int = (detector.sessionMs(now) / 60_000L).toInt()

    suspend fun tick(now: Long, foregroundPkg: String?, minuteOfDay: Int, s: AppSettings) {
        val inWatched = foregroundPkg != null && foregroundPkg in s.watchedApps
        val last = lastTickMs
        lastTickMs = now
        if (inWatched && last != null) stats.addScroll((now - last).coerceIn(0, MAX_STEP_MS))

        // The throttle may have ended without us (Stop action, VPN revoked). Do not restart it in this session.
        if (detector.throttling && coordinator.session.value == null) detector.onThrottleEnded()

        val cfg = DetectorConfig(s.thresholdMin * 60_000L, s.cooldownMin * 60_000L)
        val quiet = isQuiet(s.quietEnabled, minuteOfDay, s.quietStartMin, s.quietEndMin)
        when (detector.onTick(now, inWatched, quiet, cfg)) {
            Decision.START -> coordinator.startAuto(s, sessionMinutes(now))
            Decision.STOP -> coordinator.stopAuto()
            Decision.NONE -> Unit
        }
    }

    private companion object {
        const val MAX_STEP_MS = 30_000L
    }
}
