package com.slowdown.app.ui.common

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.slowdown.app.data.PermissionRepository
import com.slowdown.throttle.VpnPermission

class PermissionActions(
    val requestVpn: () -> Unit,
    val requestNotifications: () -> Unit,
    val openUsageAccess: () -> Unit,
)

/**
 * Launchers for the three permission steps.
 * [onVpnGranted] runs after the user accepts the system VPN dialog.
 */
@Composable
fun rememberPermissionActions(onChanged: () -> Unit, onVpnGranted: () -> Unit = {}): PermissionActions {
    val ctx = LocalContext.current
    val changed by rememberUpdatedState(onChanged)
    val vpnGranted by rememberUpdatedState(onVpnGranted)
    val vpn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        changed()
        if (r.resultCode == Activity.RESULT_OK) vpnGranted()
    }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { changed() }
    return remember {
        PermissionActions(
            requestVpn = {
                val i = VpnPermission.prepareIntent(ctx)
                if (i == null) { changed(); vpnGranted() } else vpn.launch(i)
            },
            requestNotifications = {
                if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else changed()
            },
            openUsageAccess = {
                try {
                    ctx.startActivity(PermissionRepository.usageAccessIntent(ctx))
                } catch (_: android.content.ActivityNotFoundException) {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            },
        )
    }
}
