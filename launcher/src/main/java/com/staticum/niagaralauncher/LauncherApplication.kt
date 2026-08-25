package com.staticum.niagaralauncher

import android.app.Application
import com.staticum.niagaralauncher.util.CrashLogger

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
