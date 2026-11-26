package com.slowdown.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowdown.app.AppContainer
import com.slowdown.app.data.AppSettings
import com.slowdown.app.data.ThrottleScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun setThreshold(v: Int) = run { c.settings.setThreshold(v) }
    fun setSpeed(v: Int) = run {
        c.settings.setSpeed(v)
        // Apply to a running automatic slow-down right away.
        if (c.coordinator.session.value?.origin == com.slowdown.app.logic.Origin.AUTO) c.controller.updateSpeed(v)
    }
    fun setScope(v: ThrottleScope) = run { c.settings.setScope(v) }
    fun setCooldown(v: Int) = run { c.settings.setCooldown(v) }
    fun setQuietEnabled(v: Boolean) = run { c.settings.setQuietEnabled(v) }
    fun setQuietHours(start: Int, end: Int) = run { c.settings.setQuietHours(start, end) }
    fun resetStats() = run { c.stats.reset() }
}
