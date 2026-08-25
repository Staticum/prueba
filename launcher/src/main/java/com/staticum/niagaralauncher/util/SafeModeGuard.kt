package com.staticum.niagaralauncher.util

import android.content.Context

/** Tracks whether the previous launch reached a "settled" state before this one
 * started. A crashing widget could otherwise lock the user out of their own home
 * screen forever (crash on open -> relaunch -> crash again): if the last launch
 * never settled, this run skips rendering widgets on Home so the user can still
 * reach Settings and remove the offending widget. */
object SafeModeGuard {
    private const val PREFS = "safe_mode_guard"
    private const val KEY_DIRTY = "start_in_progress"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call as early as possible in onCreate. Returns true if widgets should be
     * suppressed this run because the previous launch likely crashed. */
    fun onLaunchStart(context: Context): Boolean {
        val p = prefs(context)
        val previouslyDirty = p.getBoolean(KEY_DIRTY, false)
        p.edit().putBoolean(KEY_DIRTY, true).apply()
        return previouslyDirty
    }

    /** Call once the UI has stayed up for a few seconds without crashing. */
    fun onLaunchSettled(context: Context) {
        prefs(context).edit().putBoolean(KEY_DIRTY, false).apply()
    }
}
