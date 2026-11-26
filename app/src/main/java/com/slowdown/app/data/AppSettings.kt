package com.slowdown.app.data

enum class ThrottleScope { WATCHED_APPS, ALL_APPS }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val watching: Boolean = false,
    val thresholdMin: Int = 20,
    val speedKbps: Int = 128,
    val scope: ThrottleScope = ThrottleScope.WATCHED_APPS,
    val watchedApps: Set<String> = emptySet(),
    val watchedInitialized: Boolean = false,
    val cooldownMin: Int = 5,
    val quietEnabled: Boolean = false,
    val quietStartMin: Int = 22 * 60,
    val quietEndMin: Int = 7 * 60,
    val manualSpeedKbps: Int = 128,
    /** 0 means no timer. */
    val manualTimerMin: Int = 0,
) {
    companion object {
        const val MIN_THRESHOLD = 5
        const val MAX_THRESHOLD = 120
        const val MIN_SPEED = 32
        const val MAX_SPEED = 1024
        const val MIN_COOLDOWN = 1
        const val MAX_COOLDOWN = 30
        val TIMERS = listOf(0, 15, 30, 60)
    }
}

data class SpeedPreset(val label: String, val kbps: Int)

val SPEED_PRESETS = listOf(
    SpeedPreset("Dial-up", 56),
    SpeedPreset("2G", 128),
    SpeedPreset("3G", 384),
    SpeedPreset("Slow 4G", 1024),
)

/** Packages we pre-select when installed. */
val DEFAULT_WATCHED_PACKAGES = listOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.google.android.youtube",
    "com.twitter.android",
    "com.facebook.katana",
    "com.reddit.frontpage",
    "com.snapchat.android",
    "com.pinterest",
    "com.instagram.barcelona",
)
