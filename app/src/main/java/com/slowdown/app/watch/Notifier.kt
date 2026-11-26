package com.slowdown.app.watch

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.slowdown.app.MainActivity
import com.slowdown.app.R
import com.slowdown.app.logic.SlowNotifier

class Notifier(private val context: Context) : SlowNotifier {
    private val nm = NotificationManagerCompat.from(context)

    init {
        val sys = context.getSystemService(NotificationManager::class.java)
        sys.createNotificationChannel(
            NotificationChannel(CH_WATCH, context.getString(R.string.channel_watch), NotificationManager.IMPORTANCE_MIN)
                .apply { description = context.getString(R.string.channel_watch_desc) },
        )
        sys.createNotificationChannel(
            NotificationChannel(CH_ALERT, context.getString(R.string.channel_alert), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_alert_desc); setSound(null, null) },
        )
    }

    private fun openApp(extra: String? = null): PendingIntent {
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (extra != null) i.action = extra
        return PendingIntent.getActivity(context, extra.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(act: String): PendingIntent = PendingIntent.getBroadcast(
        context, act.hashCode(),
        Intent(context, ActionReceiver::class.java).setAction(act),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The ongoing notification the foreground service needs. */
    fun watcherNotification(usageGranted: Boolean): Notification =
        NotificationCompat.Builder(context, CH_WATCH)
            .setSmallIcon(R.drawable.ic_stat_slowdown)
            .setContentTitle(context.getString(R.string.watch_notif_title))
            .setContentText(
                context.getString(if (usageGranted) R.string.watch_notif_text else R.string.watch_notif_no_usage),
            )
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, context.getString(R.string.action_stop_watching), action(ACTION_STOP_WATCHING))
            .build()

    private fun canPost() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

    private fun post(id: Int, n: Notification) {
        if (canPost()) {
            try {
                nm.notify(id, n)
            } catch (_: SecurityException) {
                // Permission was revoked between the check and the call.
            }
        }
    }

    override fun autoStarted(scrollMin: Int) {
        post(
            ID_ALERT,
            NotificationCompat.Builder(context, CH_ALERT)
                .setSmallIcon(R.drawable.ic_stat_slowdown)
                .setContentTitle(context.getString(R.string.alert_title))
                .setContentText(context.resources.getString(R.string.alert_text, scrollMin))
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.action_stop_slowing), action(ACTION_STOP_THROTTLE))
                .build(),
        )
    }

    override fun cleared() = nm.cancel(ID_ALERT)

    /** Used by the boot receiver when the service could not start. */
    fun askResume() {
        post(
            ID_RESUME,
            NotificationCompat.Builder(context, CH_ALERT)
                .setSmallIcon(R.drawable.ic_stat_slowdown)
                .setContentTitle(context.getString(R.string.resume_title))
                .setContentText(context.getString(R.string.resume_text))
                .setContentIntent(openApp(ACTION_RESUME_WATCHING))
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val CH_WATCH = "slowdown_watch"
        const val CH_ALERT = "slowdown_alert"
        const val ID_WATCH = 1001
        const val ID_ALERT = 1002
        const val ID_RESUME = 1003
        const val ACTION_STOP_THROTTLE = "com.slowdown.app.STOP_THROTTLE"
        const val ACTION_STOP_WATCHING = "com.slowdown.app.STOP_WATCHING"
        const val ACTION_RESUME_WATCHING = "com.slowdown.app.RESUME_WATCHING"
    }
}
