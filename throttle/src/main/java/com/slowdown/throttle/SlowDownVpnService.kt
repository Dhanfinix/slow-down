package com.slowdown.throttle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.slowdown.throttle.engine.RelayEngine
import com.slowdown.throttle.engine.SocketProtector
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.Socket

class SlowDownVpnService : VpnService(), SocketProtector {
    private var tun: ParcelFileDescriptor? = null
    private var engine: RelayEngine? = null

    override fun protect(socket: Socket): Boolean = super.protect(socket)
    override fun protect(socket: DatagramSocket): Boolean = super.protect(socket)

    override fun onCreate() {
        super.onCreate()
        ThrottleControllerImpl.service = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        val config = ThrottleControllerImpl.pendingConfig
        startInForeground(config)
        if (config == null) { // restarted by the system without a config: nothing to throttle
            shutdown()
            return START_NOT_STICKY
        }
        closeEngine()
        try {
            establish(config)
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            ThrottleControllerImpl.setState(ThrottleState.Error(e.message ?: "Could not start VPN"))
            closeEngine()
            stopForegroundCompat()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun establish(config: ThrottleConfig) {
        val b = Builder()
            .setSession("Slow Down")
            .setMtu(MTU)
            .addAddress("10.111.222.1", 32)
            .addRoute("0.0.0.0", 0)
            // IPv4 only: no IPv6 route in a non-bypassable VPN makes IPv6 connects fail at once (ENETUNREACH), so apps fall back to IPv4 and nothing leaks past the throttle.
        if (config.targetPackages.isNotEmpty()) {
            config.targetPackages.forEach { addApp(b, it, allow = true) }
        } else {
            addApp(b, packageName, allow = false)
            config.excludedPackages.forEach { addApp(b, it, allow = false) }
        }
        val pfd = b.establish() ?: throw IllegalStateException("VPN permission revoked or not granted")
        tun = pfd
        val e = RelayEngine(
            FileInputStream(pfd.fileDescriptor), FileOutputStream(pfd.fileDescriptor), this,
            RelayEngine.ThrottleConfigLite(config.downKbps, config.upKbps, config.extraLatencyMs),
            onStats = { d, u ->
                val c = ThrottleControllerImpl.currentConfig
                if (c != null) ThrottleControllerImpl.setState(ThrottleState.Active(c.downKbps, c.upKbps, d, u))
            },
            onStopped = { err ->
                if (engine != null) { // stopped by itself, not through shutdown()
                    ThrottleControllerImpl.setState(
                        if (err != null) ThrottleState.Error(err.message ?: "Engine stopped") else ThrottleState.Idle,
                    )
                    shutdown()
                }
            },
        )
        engine = e
        ThrottleControllerImpl.currentConfig = config
        ThrottleControllerImpl.setState(ThrottleState.Active(config.downKbps, config.upKbps, 0, 0))
        e.start()
    }

    private fun addApp(b: Builder, pkg: String, allow: Boolean) {
        try {
            if (allow) b.addAllowedApplication(pkg) else b.addDisallowedApplication(pkg)
        } catch (_: PackageManager.NameNotFoundException) {
            Log.w(TAG, "package not installed: $pkg")
        }
    }

    internal fun applySpeed(config: ThrottleConfig) {
        engine?.updateSpeed(config.downKbps, config.upKbps)
        ThrottleControllerImpl.setState(
            ThrottleState.Active(config.downKbps, config.upKbps, engine?.bytesDown?.get() ?: 0, engine?.bytesUp?.get() ?: 0),
        )
    }

    private fun closeEngine() {
        val e = engine
        engine = null
        e?.stop()
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    internal fun shutdown() {
        closeEngine()
        ThrottleControllerImpl.currentConfig = null
        ThrottleControllerImpl.pendingConfig = null
        if (ThrottleControllerImpl.state.value !is ThrottleState.Error) ThrottleControllerImpl.setState(ThrottleState.Idle)
        stopForegroundCompat()
        stopSelf()
    }

    override fun onRevoke() {
        shutdown()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeEngine()
        if (ThrottleControllerImpl.service === this) ThrottleControllerImpl.service = null
        if (ThrottleControllerImpl.state.value is ThrottleState.Active || ThrottleControllerImpl.state.value is ThrottleState.Starting) {
            ThrottleControllerImpl.setState(ThrottleState.Idle)
        }
        super.onDestroy()
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startInForeground(config: ThrottleConfig?) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Slow Down", NotificationManager.IMPORTANCE_LOW),
        )
        val stop = PendingIntent.getService(
            this, 0, Intent(this, SlowDownVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val text = if (config != null) "Internet limited to ${config.downKbps} kbps" else "Slow Down is on"
        val n = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_slowdown)
            .setContentTitle("Slow Down")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    companion object {
        const val ACTION_START = "com.slowdown.throttle.START"
        const val ACTION_STOP = "com.slowdown.throttle.STOP"
        private const val CHANNEL_ID = "slowdown_throttle"
        const val NOTIFICATION_ID = 4711
        private const val MTU = 1400
        private const val TAG = "SlowDownVpn"
    }
}
