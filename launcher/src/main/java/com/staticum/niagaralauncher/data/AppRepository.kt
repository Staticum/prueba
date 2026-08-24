package com.staticum.niagaralauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager get() = context.packageManager

    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val collator = Collator.getInstance()
        packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.key }
            .sortedWith(compareBy(collator) { it.label.lowercase() })
    }

    fun launchIntentFor(app: AppInfo): Intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = android.content.ComponentName(app.packageName, app.activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
