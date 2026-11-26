package com.slowdown.throttle.core

/**
 * Thread-safe token bucket measured in bytes. [nanoTime] is injectable so tests can drive a fake clock.
 */
class TokenBucket(
    rateBytesPerSec: Long,
    burstBytes: Long = burstFor(rateBytesPerSec),
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private var rate = rateBytesPerSec.coerceAtLeast(1)
    private var burst = burstBytes.coerceAtLeast(1)
    private var tokens = burst.toDouble()
    private var last = nanoTime()

    @Synchronized
    fun setRate(rateBytesPerSec: Long, burstBytes: Long = burstFor(rateBytesPerSec)) {
        refill()
        rate = rateBytesPerSec.coerceAtLeast(1)
        burst = burstBytes.coerceAtLeast(1)
        if (tokens > burst) tokens = burst.toDouble()
    }

    @Synchronized
    fun available(): Long { refill(); return tokens.toLong() }

    /** All or nothing. */
    @Synchronized
    fun tryTake(n: Long): Boolean {
        refill()
        if (tokens < n) return false
        tokens -= n
        return true
    }

    /** Takes up to [max] tokens and returns how many were taken. */
    @Synchronized
    fun takeUpTo(max: Long): Long {
        refill()
        val t = minOf(max, tokens.toLong())
        tokens -= t
        return t
    }

    /** Returns unused tokens, for example after a short read. */
    @Synchronized
    fun give(n: Long) { tokens = minOf(tokens + n, burst.toDouble()) }

    private fun refill() {
        val now = nanoTime()
        val dt = now - last
        if (dt > 0) {
            tokens = minOf(burst.toDouble(), tokens + dt * rate / 1e9)
            last = now
        }
    }

    companion object {
        /** 100 ms of traffic, but never less than two full-size segments, so one segment always fits. */
        fun burstFor(rateBytesPerSec: Long): Long = maxOf(rateBytesPerSec / 10, 3000L)
        fun bytesPerSec(kbps: Int): Long = kbps.coerceAtLeast(1) * 1000L / 8
    }
}
