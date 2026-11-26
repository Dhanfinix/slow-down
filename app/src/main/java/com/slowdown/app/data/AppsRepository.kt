package com.slowdown.app.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val pkg: String, val label: String)

/** Lists launchable apps. Needs the LAUNCHER `<queries>` entry in the manifest (no QUERY_ALL_PACKAGES). */
class AppsRepository(private val context: Context) {
    suspend fun installed(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun defaultsInstalled(): Set<String> {
        val have = installed().map { it.pkg }.toSet()
        return DEFAULT_WATCHED_PACKAGES.filter { it in have }.toSet()
    }
}
