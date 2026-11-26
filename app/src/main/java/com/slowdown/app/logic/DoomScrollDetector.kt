package com.slowdown.app.logic

data class DetectorConfig(
    val thresholdMs: Long,
    val cooldownMs: Long,
    /** A gap shorter than this does not end a session. */
    val gapMs: Long = 2 * 60_000L,
)

enum class Decision { NONE, START, STOP }

/**
 * Pure session logic. Call [onTick] about every 15 s.
 *
 * A session starts when a watched app is first seen and continues across gaps shorter than
 * [DetectorConfig.gapMs]. When the session lasts [DetectorConfig.thresholdMs] the result is START.
 * While throttling, the user must be away for [DetectorConfig.cooldownMs] before STOP.
 */
class DoomScrollDetector {
    var sessionStartMs: Long? = null
        private set
    private var lastWatchedMs: Long? = null
    var throttling: Boolean = false
        private set
    private var suppressed = false

    fun sessionMs(now: Long): Long = sessionStartMs?.let { now - it } ?: 0L

    /** Call when the throttle ended for another reason (user pressed Stop, timer, error). */
    fun onThrottleEnded() {
        throttling = false
        suppressed = true
    }

    fun onTick(now: Long, inWatched: Boolean, quiet: Boolean, cfg: DetectorConfig): Decision {
        val last = lastWatchedMs
        if (inWatched) {
            var decision = Decision.NONE
            val limit = if (throttling) cfg.cooldownMs else cfg.gapMs
            if (sessionStartMs == null || last == null || now - last > limit) {
                if (throttling) decision = Decision.STOP
                reset()
                sessionStartMs = now
            }
            lastWatchedMs = now
            if (decision == Decision.NONE && !throttling && !suppressed && !quiet &&
                now - (sessionStartMs ?: now) >= cfg.thresholdMs
            ) {
                throttling = true
                return Decision.START
            }
            return decision
        }
        if (last == null) return Decision.NONE
        val away = now - last
        if (throttling && away >= cfg.cooldownMs) {
            reset()
            return Decision.STOP
        }
        if (!throttling && away > cfg.gapMs) reset()
        return Decision.NONE
    }

    private fun reset() {
        sessionStartMs = null
        lastWatchedMs = null
        throttling = false
        suppressed = false
    }
}

/** Quiet hours may wrap past midnight (22:00-07:00). */
fun isQuiet(enabled: Boolean, minuteOfDay: Int, startMin: Int, endMin: Int): Boolean {
    if (!enabled || startMin == endMin) return false
    return if (startMin < endMin) minuteOfDay in startMin until endMin
    else minuteOfDay >= startMin || minuteOfDay < endMin
}
