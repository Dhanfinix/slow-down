package com.slowdown.app.watch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.slowdown.app.SlowDownApp
import com.slowdown.app.data.PermissionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Foreground service that polls usage events every 15 s while Watching is on.
 * Type: specialUse (subtype doom_scroll_watcher). Android has no better type for this job.
 */
class WatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as SlowDownApp
        val usage = PermissionRepository.isUsageAccessGranted(this)
        ServiceCompat.startForeground(
            this, Notifier.ID_WATCH, app.container.notifier.watcherNotification(usage),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        running = true
        if (loop?.isActive != true) loop = scope.launch { run(app) }
        return START_STICKY
    }

    private suspend fun run(app: SlowDownApp) {
        val c = app.container
        val reader = UsageEventsReader(this)
        while (scope.isActive) {
            val now = System.currentTimeMillis()
            val s = c.settings.current()
            val fg = reader.poll(now)
            val t = LocalTime.now()
            c.watchEngine.tick(now, fg, t.hour * 60 + t.minute, s)
            delay(POLL_MS)
        }
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val POLL_MS = 15_000L

        @Volatile
        var running = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WatcherService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatcherService::class.java))
        }
    }
}
