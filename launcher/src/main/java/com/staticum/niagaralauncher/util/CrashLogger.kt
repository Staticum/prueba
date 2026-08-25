package com.staticum.niagaralauncher.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Appends every uncaught crash to a plain-text file in app-internal storage, so a
 * crash can be inspected/shared without needing `adb logcat` on the device. Testing-
 * phase tool only - install() chains to the platform's default handler afterward, so
 * the crash still terminates the app exactly as it would otherwise. */
object CrashLogger {
    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_CHARS = 200_000

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { appendCrash(appContext, thread, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasLog(context: Context): Boolean = logFile(context).let { it.exists() && it.length() > 0L }

    fun clear(context: Context) {
        logFile(context).delete()
    }

    private fun appendCrash(context: Context, thread: Thread, throwable: Throwable) {
        val file = logFile(context)
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = "\n===== $timestamp · thread=${thread.name} =====\n$stackTrace"
        val existing = if (file.exists()) file.readText() else ""
        file.writeText((existing + entry).takeLast(MAX_CHARS))
    }
}
