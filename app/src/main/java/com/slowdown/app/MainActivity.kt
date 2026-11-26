package com.slowdown.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.slowdown.app.ui.AppNav
import com.slowdown.app.ui.theme.SlowDownTheme
import com.slowdown.app.watch.Notifier
import com.slowdown.app.watch.WatcherService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as SlowDownApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SlowDownTheme { AppNav() } }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        container.permissions.refresh()
    }

    /** The "resume watching" notification from the boot receiver opens us in front, where a service start is allowed. */
    private fun handle(i: Intent?) {
        // Also covers a force stop or app update: Watching stays on in settings but the service is gone.
        if (i?.action == Notifier.ACTION_RESUME_WATCHING || !WatcherService.running) {
            lifecycleScope.launch {
                if (container.settings.current().watching) container.setWatching(true)
            }
        }
    }
}
