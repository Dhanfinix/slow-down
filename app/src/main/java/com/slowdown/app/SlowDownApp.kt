package com.slowdown.app

import android.app.Application
import android.content.Context
import com.slowdown.app.data.AppsRepository
import com.slowdown.app.data.PermissionRepository
import com.slowdown.app.data.SettingsRepository
import com.slowdown.app.data.StatsRepository
import com.slowdown.app.logic.ThrottleCoordinator
import com.slowdown.app.logic.WatchEngine
import com.slowdown.app.watch.Notifier
import com.slowdown.app.watch.WatcherService
import com.slowdown.throttle.ThrottleControllers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-wired dependencies. One instance per process. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsRepository(context)
    val stats = StatsRepository(context)
    val apps = AppsRepository(context)
    val permissions = PermissionRepository(context)
    val notifier = Notifier(context)
    val controller = ThrottleControllers.get(context)
    val coordinator = ThrottleCoordinator(controller, stats, notifier, appScope)
    val watchEngine = WatchEngine(coordinator, stats)

    /** Turns Watching on or off and starts or stops the foreground service. Call from the UI while in front. */
    suspend fun setWatching(on: Boolean) {
        settings.setWatching(on)
        if (on) {
            WatcherService.start(appContext)
        } else {
            WatcherService.stop(appContext)
            coordinator.stopAuto()
        }
    }
}

class SlowDownApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // First run: pre-select popular apps, but only those that are installed.
        container.appScope.launch {
            if (!container.settings.current().watchedInitialized) {
                container.settings.setWatchedApps(container.apps.defaultsInstalled())
            }
        }
    }
}
