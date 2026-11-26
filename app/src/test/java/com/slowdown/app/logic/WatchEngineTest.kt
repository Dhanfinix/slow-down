package com.slowdown.app.logic

import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.StatsSink
import com.slowdown.app.data.ThrottleScope
import com.slowdown.throttle.ThrottleConfig
import com.slowdown.throttle.ThrottleController
import com.slowdown.throttle.ThrottleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeController : ThrottleController {
    private val _state = MutableStateFlow<ThrottleState>(ThrottleState.Idle)
    override val state: StateFlow<ThrottleState> = _state
    val started = mutableListOf<ThrottleConfig>()
    var stops = 0

    override fun start(config: ThrottleConfig) {
        started += config
        _state.value = ThrottleState.Starting
        _state.value = ThrottleState.Active(config.downKbps, config.upKbps, 0, 0)
    }

    override fun stop() { stops++; _state.value = ThrottleState.Idle }
    override fun updateSpeed(kbps: Int) = Unit

    /** What the VPN service does when the user switches the VPN off in system UI. */
    fun revoke() { _state.value = ThrottleState.Idle }
}

class FakeStats : StatsSink {
    var scrollMs = 0L
    var slowStarts = 0
    var slowedMs = 0L
    override suspend fun addScroll(ms: Long) { scrollMs += ms }
    override suspend fun addSlowStart() { slowStarts++ }
    override suspend fun addSlowedTime(ms: Long) { slowedMs += ms }
}

class FakeNotifier : SlowNotifier {
    var startedMin: Int? = null
    var cleared = 0
    override fun autoStarted(scrollMin: Int) { startedMin = scrollMin }
    override fun cleared() { cleared++ }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WatchEngineTest {
    private val min = 60_000L
    private val settings = AppSettings(
        thresholdMin = 20, cooldownMin = 5, speedKbps = 200,
        scope = ThrottleScope.WATCHED_APPS, watchedApps = setOf("com.insta", "com.tok"),
    )

    private class Rig(scope: TestScope) {
        val controller = FakeController()
        val stats = FakeStats()
        val notifier = FakeNotifier()
        var now = 0L
        val coordinator = ThrottleCoordinator(controller, stats, notifier, scope.backgroundScope + UnconfinedTestDispatcher(scope.testScheduler)) { now }
        val engine = WatchEngine(coordinator, stats)
    }

    private suspend fun Rig.tickFor(durationMs: Long, pkg: String?, s: AppSettings) {
        val end = now + durationMs
        while (now < end) {
            now += 15_000
            engine.tick(now, pkg, 12 * 60, s)
        }
    }

    /** The invariant: scrolling past the threshold starts the throttle with the configured config, leaving stops it. */
    @Test fun longSessionStartsThrottleAndCooldownStopsIt() = runTest {
        val r = Rig(this)
        r.tickFor(19 * min, "com.insta", settings)
        assertTrue("no start before threshold", r.controller.started.isEmpty())

        r.tickFor(1 * min + 15_000, "com.insta", settings)
        assertEquals(1, r.controller.started.size)
        assertEquals(
            ThrottleConfig(downKbps = 200, upKbps = 200, targetPackages = setOf("com.insta", "com.tok")),
            r.controller.started.single(),
        )
        assertEquals(Origin.AUTO, r.coordinator.session.value?.origin)
        assertEquals(20, r.notifier.startedMin)
        assertEquals(0, r.controller.stops)

        r.tickFor(4 * min, null, settings) // away, still inside cooldown
        assertEquals(0, r.controller.stops)

        r.tickFor(1 * min, null, settings) // 5 min away
        assertEquals(1, r.controller.stops)
        assertNull(r.coordinator.session.value)
        assertEquals(1, r.stats.slowStarts)
        assertTrue(r.stats.slowedMs >= 5 * min)
        assertTrue(r.stats.scrollMs >= 19 * min)
    }

    @Test fun wholeDeviceScopeUsesEmptyTargets() = runTest {
        val r = Rig(this)
        r.tickFor(21 * min, "com.insta", settings.copy(scope = ThrottleScope.ALL_APPS))
        assertEquals(emptySet<String>(), r.controller.started.single().targetPackages)
    }

    @Test fun otherAppDoesNotCount() = runTest {
        val r = Rig(this)
        r.tickFor(60 * min, "com.maps", settings)
        assertTrue(r.controller.started.isEmpty())
    }

    @Test fun stopFromNotificationIsNotRestartedInSameSession() = runTest {
        val r = Rig(this)
        r.tickFor(21 * min, "com.insta", settings)
        r.coordinator.stop() // user taps Stop on the notification
        r.tickFor(10 * min, "com.insta", settings)
        assertEquals(1, r.controller.started.size)
    }

    @Test fun vpnRevokedClosesSession() = runTest {
        val r = Rig(this)
        r.tickFor(21 * min, "com.insta", settings)
        assertNotNull(r.coordinator.session.value)
        r.controller.revoke()
        assertNull(r.coordinator.session.value)
        assertEquals(0, r.controller.stops) // already stopped, do not call again
    }

    @Test fun manualTimerStopsThrottle() = runTest {
        val r = Rig(this)
        r.coordinator.startManual(settings.copy(manualTimerMin = 15, manualSpeedKbps = 64))
        assertEquals(64, r.controller.started.single().downKbps)
        advanceTimeBy(14 * min)
        assertEquals(0, r.controller.stops)
        advanceTimeBy(1 * min + 1)
        assertEquals(1, r.controller.stops)
    }

    @Test fun autoStopDoesNotEndManualSession() = runTest {
        val r = Rig(this)
        r.coordinator.startManual(settings)
        r.coordinator.stopAuto()
        assertEquals(0, r.controller.stops)
        assertNotNull(r.coordinator.session.value)
    }
}
