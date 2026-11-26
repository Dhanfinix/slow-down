package com.slowdown.app.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.slowdown.app.SlowDownApp
import kotlinx.coroutines.launch

/** Handles notification actions: Stop slowing, Stop watching. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = (context.applicationContext as SlowDownApp).container
        when (intent.action) {
            Notifier.ACTION_STOP_THROTTLE -> c.coordinator.stop()
            Notifier.ACTION_STOP_WATCHING -> {
                val pending = goAsync()
                c.appScope.launch {
                    try {
                        c.settings.setWatching(false)
                        c.coordinator.stopAuto()
                        WatcherService.stop(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}

/**
 * Restores Watching after reboot. Starting a foreground service from the background can be refused,
 * so on failure we post a notification that asks the user to resume.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val c = (context.applicationContext as SlowDownApp).container
        val pending = goAsync()
        c.appScope.launch {
            try {
                if (c.settings.current().watching) {
                    try {
                        WatcherService.start(context)
                    } catch (_: Exception) {
                        c.notifier.askResume()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
