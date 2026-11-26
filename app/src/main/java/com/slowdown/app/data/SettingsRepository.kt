package com.slowdown.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val watching = booleanPreferencesKey("watching")
        val threshold = intPreferencesKey("threshold_min")
        val speed = intPreferencesKey("speed_kbps")
        val scope = stringPreferencesKey("scope")
        val apps = stringSetPreferencesKey("watched_apps")
        val appsInit = booleanPreferencesKey("watched_init")
        val cooldown = intPreferencesKey("cooldown_min")
        val quietOn = booleanPreferencesKey("quiet_on")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val manualSpeed = intPreferencesKey("manual_speed")
        val manualTimer = intPreferencesKey("manual_timer")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            onboardingDone = p[K.onboarding] ?: d.onboardingDone,
            watching = p[K.watching] ?: d.watching,
            thresholdMin = p[K.threshold] ?: d.thresholdMin,
            speedKbps = p[K.speed] ?: d.speedKbps,
            scope = p[K.scope]?.let { runCatching { ThrottleScope.valueOf(it) }.getOrNull() } ?: d.scope,
            watchedApps = p[K.apps] ?: d.watchedApps,
            watchedInitialized = p[K.appsInit] ?: false,
            cooldownMin = p[K.cooldown] ?: d.cooldownMin,
            quietEnabled = p[K.quietOn] ?: d.quietEnabled,
            quietStartMin = p[K.quietStart] ?: d.quietStartMin,
            quietEndMin = p[K.quietEnd] ?: d.quietEndMin,
            manualSpeedKbps = p[K.manualSpeed] ?: d.manualSpeedKbps,
            manualTimerMin = p[K.manualTimer] ?: d.manualTimerMin,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(v: Boolean) { store.edit { it[K.onboarding] = v } }
    suspend fun setWatching(v: Boolean) { store.edit { it[K.watching] = v } }
    suspend fun setThreshold(min: Int) {
        store.edit { it[K.threshold] = min.coerceIn(AppSettings.MIN_THRESHOLD, AppSettings.MAX_THRESHOLD) }
    }
    suspend fun setSpeed(kbps: Int) {
        store.edit { it[K.speed] = kbps.coerceIn(AppSettings.MIN_SPEED, AppSettings.MAX_SPEED) }
    }
    suspend fun setScope(v: ThrottleScope) { store.edit { it[K.scope] = v.name } }
    suspend fun setCooldown(min: Int) {
        store.edit { it[K.cooldown] = min.coerceIn(AppSettings.MIN_COOLDOWN, AppSettings.MAX_COOLDOWN) }
    }
    suspend fun setQuietEnabled(v: Boolean) { store.edit { it[K.quietOn] = v } }
    suspend fun setQuietHours(startMin: Int, endMin: Int) {
        store.edit {
            it[K.quietStart] = startMin.coerceIn(0, 1439)
            it[K.quietEnd] = endMin.coerceIn(0, 1439)
        }
    }
    suspend fun setManualSpeed(kbps: Int) {
        store.edit { it[K.manualSpeed] = kbps.coerceIn(AppSettings.MIN_SPEED, AppSettings.MAX_SPEED) }
    }
    suspend fun setManualTimer(min: Int) { store.edit { it[K.manualTimer] = min } }
    suspend fun setWatchedApps(apps: Set<String>) {
        store.edit {
            it[K.apps] = apps
            it[K.appsInit] = true
        }
    }
    suspend fun toggleWatchedApp(pkg: String) {
        store.edit {
            val now = it[K.apps] ?: emptySet()
            it[K.apps] = if (pkg in now) now - pkg else now + pkg
            it[K.appsInit] = true
        }
    }
}
