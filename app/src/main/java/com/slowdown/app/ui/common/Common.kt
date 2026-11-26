package com.slowdown.app.ui.common

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slowdown.app.AppContainer
import com.slowdown.app.SlowDownApp
import java.util.Locale

@Composable
fun rememberContainer(): AppContainer {
    val ctx = LocalContext.current
    return remember { (ctx.applicationContext as SlowDownApp).container }
}

/** Creates a ViewModel that gets the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: (AppContainer) -> VM): VM {
    val c = rememberContainer()
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create(c) as T
    })
}

/** True when the user turned animations off in system settings. */
@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

object Format {
    fun duration(ms: Long): String {
        val totalMin = ms / 60_000
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun speed(kbps: Int): String =
        if (kbps >= 1000) String.format(Locale.US, "%.1f Mbps", kbps / 1000.0) else "$kbps kbps"

    fun bytes(b: Long): String = when {
        b >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", b / (1L shl 30).toDouble())
        b >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", b / (1L shl 20).toDouble())
        b >= 1L shl 10 -> String.format(Locale.US, "%.0f KB", b / 1024.0)
        else -> "$b B"
    }

    fun clock(minuteOfDay: Int): String = String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)
}
