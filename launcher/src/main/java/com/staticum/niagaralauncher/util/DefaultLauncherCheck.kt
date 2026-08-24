package com.staticum.niagaralauncher.util

import android.content.Context
import android.content.Intent

fun isDefaultLauncher(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(homeIntent, 0) ?: return false
    return resolved.activityInfo.packageName == context.packageName
}
