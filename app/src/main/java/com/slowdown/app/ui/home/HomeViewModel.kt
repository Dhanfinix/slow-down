package com.slowdown.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowdown.app.AppContainer
import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.DayStats
import com.slowdown.app.data.PermissionState
import com.slowdown.app.logic.SlowSession
import com.slowdown.throttle.ThrottleState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HeroMode { OFF, WATCHING, SLOWED }

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val permissions: PermissionState = PermissionState(),
    val mode: HeroMode = HeroMode.OFF,
    val session: SlowSession? = null,
    val throttle: ThrottleState = ThrottleState.Idle,
    val today: DayStats = DayStats(""),
)

fun computeHeroMode(settings: AppSettings, perms: PermissionState, session: SlowSession?, throttle: ThrottleState): HeroMode = when {
    session != null || throttle is ThrottleState.Active || throttle is ThrottleState.Starting -> HeroMode.SLOWED
    settings.watching && perms.usageAccess -> HeroMode.WATCHING
    else -> HeroMode.OFF
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        c.settings.settings,
        c.permissions.state,
        c.coordinator.session,
        c.coordinator.throttleState,
        c.stats.days,
    ) { s, p, session, th, days ->
        HomeUiState(
            settings = s,
            permissions = p,
            mode = computeHeroMode(s, p, session, th),
            session = session,
            throttle = th,
            today = days.firstOrNull { it.date == LocalDate.now().toString() } ?: DayStats(LocalDate.now().toString()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshPermissions() = c.permissions.refresh()

    /** The caller makes sure VPN consent is granted first. */
    fun startManual() {
        viewModelScope.launch { c.coordinator.startManual(c.settings.current()) }
    }

    fun stopSlowing() = c.coordinator.stop()

    fun setManualSpeed(kbps: Int) = viewModelScope.launch { c.settings.setManualSpeed(kbps) }.let { }
    fun setManualTimer(min: Int) = viewModelScope.launch { c.settings.setManualTimer(min) }.let { }
    fun setWatching(on: Boolean) = viewModelScope.launch { c.setWatching(on) }.let { }
}
