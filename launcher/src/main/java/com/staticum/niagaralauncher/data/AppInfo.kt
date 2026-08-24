package com.staticum.niagaralauncher.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
    val isHidden: Boolean = false,
) {
    val key: String get() = "$packageName/$activityName"
}
