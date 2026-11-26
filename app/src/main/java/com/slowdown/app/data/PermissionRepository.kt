package com.slowdown.app.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.slowdown.throttle.VpnPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(val vpn: Boolean = false, val notifications: Boolean = false, val usageAccess: Boolean = false)

class PermissionRepository(private val context: Context) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    /** Call from onResume so the screens show live status after the user returns from system dialogs. */
    fun refresh() {
        _state.value = read()
    }

    private fun read() = PermissionState(
        vpn = VpnPermission.isGranted(context),
        notifications = notificationsGranted(context),
        usageAccess = isUsageAccessGranted(context),
    )

    companion object {
        fun notificationsGranted(context: Context): Boolean =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        fun isUsageAccessGranted(context: Context): Boolean {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid = Process.myUid()
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun usageAccessIntent(context: Context): Intent =
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
}
