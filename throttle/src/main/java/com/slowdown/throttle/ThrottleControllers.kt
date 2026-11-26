package com.slowdown.throttle

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Entry point for the app: `ThrottleControllers.get(context)`. */
object ThrottleControllers {
    fun get(context: Context): ThrottleController {
        ThrottleControllerImpl.init(context.applicationContext)
        return ThrottleControllerImpl
    }
}

/** VPN consent helper. */
object VpnPermission {
    /** Null when permission is already granted, otherwise an intent for `startActivityForResult` / the Activity Result API. */
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)
    fun isGranted(context: Context): Boolean = VpnService.prepare(context) == null
}

internal object ThrottleControllerImpl : ThrottleController {
    private lateinit var appContext: Context
    private val _state = MutableStateFlow<ThrottleState>(ThrottleState.Idle)
    override val state: StateFlow<ThrottleState> = _state.asStateFlow()

    @Volatile var pendingConfig: ThrottleConfig? = null
    @Volatile var currentConfig: ThrottleConfig? = null
    @Volatile var service: SlowDownVpnService? = null

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context
        // A new process has no VPN (it died with the old one). If that process was killed while throttling,
        // Android keeps the dead service's foreground notification. Cancel does nothing on it, so ask the service to stop.
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        if (nm.activeNotifications.any { it.id == SlowDownVpnService.NOTIFICATION_ID }) {
            try {
                context.startService(Intent(context, SlowDownVpnService::class.java).setAction(SlowDownVpnService.ACTION_STOP))
            } catch (_: Exception) { /* background start refused; the notification stays until the app is opened */ }
        }
    }

    fun setState(s: ThrottleState) { _state.value = s }

    override fun start(config: ThrottleConfig) {
        if (config.downKbps <= 0 || config.upKbps <= 0) {
            _state.value = ThrottleState.Error("Speed must be above 0 kbps")
            return
        }
        if (VpnService.prepare(appContext) != null) {
            _state.value = ThrottleState.Error("VPN permission not granted")
            return
        }
        pendingConfig = config
        _state.value = ThrottleState.Starting
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, SlowDownVpnService::class.java).setAction(SlowDownVpnService.ACTION_START),
            )
        } catch (e: Exception) {
            _state.value = ThrottleState.Error(e.message ?: "Could not start service")
        }
    }

    override fun stop() {
        val s = service
        if (s != null) s.shutdown() else _state.value = ThrottleState.Idle
        pendingConfig = null
    }

    override fun updateSpeed(kbps: Int) {
        val cfg = currentConfig ?: return
        if (kbps <= 0) return
        val n = cfg.copy(downKbps = kbps, upKbps = kbps)
        currentConfig = n
        service?.applySpeed(n)
    }
}
