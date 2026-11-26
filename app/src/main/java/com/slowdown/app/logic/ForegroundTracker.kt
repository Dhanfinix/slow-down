package com.slowdown.app.logic

enum class FgEventKind { RESUMED, PAUSED, SCREEN_OFF, OTHER }

data class FgEvent(val pkg: String, val kind: FgEventKind)

/** Turns a time-ordered stream of usage events into "which package is in front now". */
class ForegroundTracker {
    var current: String? = null
        private set

    fun apply(events: List<FgEvent>) {
        for (e in events) when (e.kind) {
            FgEventKind.RESUMED -> current = e.pkg
            FgEventKind.PAUSED -> if (current == e.pkg) current = null
            FgEventKind.SCREEN_OFF -> current = null
            FgEventKind.OTHER -> Unit
        }
    }
}
