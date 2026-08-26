package com.staticum.niagaralauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.usageDataStore by preferencesDataStore(name = "launcher_usage")

/** Half-life of a launch's weight. At ~3.5 days, an app used heavily a week ago sits
 * at roughly a quarter of the weight of one used today - which is the "last mobile
 * week" behaviour asked for, but as a smooth ramp rather than a hard 7-day cliff
 * where an app vanishes from the list overnight. */
private const val HALF_LIFE_MS = 3.5 * 24 * 60 * 60 * 1000

/** One app's decayed launch score plus the moment it was last updated. Storing a
 * single number per app instead of a list of timestamps keeps storage bounded and
 * makes both recording and ranking O(1). */
private data class UsageEntry(val score: Float, val lastUpdate: Long)

private fun decay(entry: UsageEntry, now: Long): Float {
    val elapsed = (now - entry.lastUpdate).coerceAtLeast(0L)
    return entry.score * Math.pow(0.5, elapsed / HALF_LIFE_MS).toFloat()
}

/**
 * Tracks how much each app is used, to drive the "Frecuentes" section and the
 * ordering inside a letter of the A-Z index.
 *
 * Two sources, deliberately:
 *  - **Own counter.** Every launch goes through `MainActivity.launchApp`, so this
 *    costs no permission and works from the first run. Its blind spot is real
 *    though: it only sees launches made *from MinZen*, so an app usually opened
 *    from a notification (messaging apps above all) looks far less used than it is.
 *  - **System usage stats.** Sees every launch regardless of origin and comes with
 *    history, so it is accurate immediately - but PACKAGE_USAGE_STATS is a special
 *    permission that cannot be requested with a normal dialog; the user has to
 *    enable it by hand under Settings > Usage access.
 *
 * When the permission is granted the system data wins; otherwise the local counter
 * is used. Everything stays on the device.
 */
class UsageRepository(private val context: Context) {

    private val scoresKey = stringPreferencesKey("usage_scores")

    /** Local, decayed scores keyed by `AppInfo.key` (package/activity). */
    val localScoresFlow: Flow<Map<String, Float>> = context.usageDataStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        parse(prefs[scoresKey]).mapValues { (_, entry) -> decay(entry, now) }
    }

    suspend fun recordLaunch(appKey: String) {
        val now = System.currentTimeMillis()
        context.usageDataStore.edit { prefs ->
            val entries = parse(prefs[scoresKey]).toMutableMap()
            val current = entries[appKey]
            // Decay whatever was there to *now* before adding this launch, so a burst
            // of use long ago can't keep outranking steady recent use.
            val decayed = if (current == null) 0f else decay(current, now)
            entries[appKey] = UsageEntry(decayed + 1f, now)
            prefs[scoresKey] = serialize(entries)
        }
    }

    suspend fun clear() {
        context.usageDataStore.edit { it.remove(scoresKey) }
    }

    /**
     * Per-package foreground time over the last 7 days, normalised into scores.
     *
     * Returns an empty map when the permission isn't granted, so callers can simply
     * fall back to the local counter.
     */
    suspend fun systemScoresByPackage(): Map<String, Float> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess(context)) return@withContext emptyMap()
        runCatching {
            val manager = context.getSystemService(UsageStatsManager::class.java)
                ?: return@runCatching emptyMap<String, Float>()
            val now = System.currentTimeMillis()
            val weekAgo = now - 7L * 24 * 60 * 60 * 1000
            manager.queryAndAggregateUsageStats(weekAgo, now)
                .mapNotNull { (pkg, stats) ->
                    val minutes = stats.totalTimeInForeground / 60_000f
                    if (minutes <= 0f) null else pkg to minutes
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    companion object {
        /** Whether the user has granted Usage access. This is an AppOps check, not a
         * runtime permission check - PACKAGE_USAGE_STATS is never "granted" in the
         * normal sense. */
        fun hasUsageAccess(context: Context): Boolean = runCatching {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    // Same serialised-map shape already used by WidgetRepository: "key:a:b,key:a:b".
    // Safe because neither a package name nor an activity name can contain ':' or ','.
    private fun parse(raw: String?): Map<String, UsageEntry> =
        raw?.split(",")
            ?.mapNotNull { chunk ->
                val parts = chunk.split(":")
                if (parts.size != 3) return@mapNotNull null
                val score = parts[1].toFloatOrNull() ?: return@mapNotNull null
                val ts = parts[2].toLongOrNull() ?: return@mapNotNull null
                parts[0] to UsageEntry(score, ts)
            }
            ?.toMap()
            ?: emptyMap()

    private fun serialize(entries: Map<String, UsageEntry>): String =
        entries.entries
            // Drop anything that has decayed into irrelevance so the blob can't grow
            // without bound across months of use.
            .filter { it.value.score > 0.01f }
            .joinToString(",") { "${it.key}:${it.value.score}:${it.value.lastUpdate}" }
}
