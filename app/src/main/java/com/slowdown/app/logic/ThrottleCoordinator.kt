package com.slowdown.app.logic

import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.StatsSink
import com.slowdown.app.data.ThrottleScope
import com.slowdown.throttle.ThrottleConfig
import com.slowdown.throttle.ThrottleController
import com.slowdown.throttle.ThrottleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Origin { MANUAL, AUTO }

data class SlowSession(val origin: Origin, val startedAt: Long, val endsAt: Long?)

/** Posts user-visible notices. The Android implementation lives in the watch package. */
interface SlowNotifier {
    fun autoStarted(scrollMin: Int)
    fun cleared()
}

/** The single owner of "are we slowing the user right now". Wraps [ThrottleController]. */
class ThrottleCoordinator(
    private val controller: ThrottleController,
    private val stats: StatsSink,
    private val notifier: SlowNotifier,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _session = MutableStateFlow<SlowSession?>(null)
    val session: StateFlow<SlowSession?> = _session.asStateFlow()
    val throttleState: StateFlow<ThrottleState> get() = controller.state
    private var timer: Job? = null

    init {
        // The VPN can end without us asking (user revoked it, start failed). Close the session then.
        scope.launch {
            controller.state.collect { s ->
                if ((s is ThrottleState.Idle || s is ThrottleState.Error) && _session.value != null) finish(callController = false)
            }
        }
    }

    /** Builds the engine config. Watched-apps scope with no watched apps falls back to all apps. */
    fun configFor(s: AppSettings, kbps: Int) = ThrottleConfig(
        downKbps = kbps,
        upKbps = kbps,
        targetPackages = if (s.scope == ThrottleScope.WATCHED_APPS) s.watchedApps else emptySet(),
    )

    fun startManual(s: AppSettings) {
        if (_session.value != null) return
        val now = clock()
        val ends = if (s.manualTimerMin > 0) now + s.manualTimerMin * 60_000L else null
        begin(SlowSession(Origin.MANUAL, now, ends), configFor(s, s.manualSpeedKbps))
        if (ends != null) {
            timer = scope.launch {
                delay(ends - now)
                stop()
            }
        }
    }

    /** No-op when the user already started a slow-down by hand. */
    fun startAuto(s: AppSettings, scrollMin: Int) {
        if (_session.value != null) return
        begin(SlowSession(Origin.AUTO, clock(), null), configFor(s, s.speedKbps))
        notifier.autoStarted(scrollMin)
    }

    /** Stops only an automatic slow-down. Manual ones end by timer or by the user. */
    fun stopAuto() {
        if (_session.value?.origin == Origin.AUTO) stop()
    }

    fun stop() {
        if (_session.value != null) finish(callController = true)
    }

    private fun begin(session: SlowSession, config: ThrottleConfig) {
        _session.value = session
        scope.launch { stats.addSlowStart() }
        controller.start(config)
    }

    private fun finish(callController: Boolean) {
        val s = _session.value ?: return
        _session.value = null
        timer?.cancel()
        timer = null
        if (callController) controller.stop()
        notifier.cleared()
        val ms = (clock() - s.startedAt).coerceAtLeast(0)
        scope.launch { stats.addSlowedTime(ms) }
    }
}
