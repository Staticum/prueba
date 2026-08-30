package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "launcher_widgets")

/** A widget's placement in the home grid: top-left cell at (col, row), spanning
 * colSpan columns and rowSpan rows. All four are cell counts, not dp - actual pixel
 * size comes from multiplying by the grid's cell size at render time. */
data class WidgetEntry(val id: Int, val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int)

/** Fixed grid geometry shared between the repository (default placement for a newly
 * added widget) and the UI (rendering, drag, resize). A fixed column count and cell
 * height - rather than a variable one derived from content - is what makes "put this
 * exactly where I want it" possible: every widget measures itself in the same units,
 * so two widgets can align edge to edge on purpose instead of by coincidence. */
object WidgetGrid {
    const val COLUMNS = 6
    const val CELL_HEIGHT_DP = 80
    const val MIN_COL_SPAN = 1
    const val MIN_ROW_SPAN = 1
    const val MAX_ROW_SPAN = 8
}

/**
 * Persists where each app-widget id sits on the home grid.
 *
 * Superseded the previous model (an ordered list plus an independent width-percent
 * and height-dp per widget, auto-packed into rows) because it could not represent
 * "put this exact widget in this exact spot" - packing order decided placement, not
 * the user. Widgets now carry explicit grid coordinates instead.
 */
class WidgetRepository(private val context: Context) {

    private val gridKey = stringPreferencesKey("widget_grid")

    // Superseded keys, from before this became a real grid - read-only now, used
    // solely to migrate anyone updating from that version without losing their
    // widgets outright. Never written again once gridKey exists.
    private val legacyOrderKey = stringPreferencesKey("widget_order")
    private val legacyHeightsKey = stringPreferencesKey("widget_heights")
    private val legacyWidthsKey = stringPreferencesKey("widget_widths")

    val widgetsFlow: Flow<List<WidgetEntry>> = context.widgetDataStore.data.map { prefs ->
        val raw = prefs[gridKey]
        if (raw != null) {
            parseGrid(raw)
        } else {
            migrateLegacy(prefs[legacyOrderKey], prefs[legacyHeightsKey], prefs[legacyWidthsKey])
        }
    }

    val widgetIdsFlow: Flow<List<Int>> = widgetsFlow.map { list -> list.map { it.id } }

    suspend fun addWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val entries = currentEntries(prefs).toMutableMap()
            if (entries.containsKey(id)) return@edit
            val (colSpan, rowSpan) = defaultSpanFor(id)
            // New widgets land in their own row at the bottom, full-width by
            // default - the least surprising starting point, since the user hasn't
            // told the grid anything about where they want it yet. They can then
            // drag and resize it anywhere.
            val startRow = entries.values.maxOfOrNull { it.row + it.rowSpan } ?: 0
            entries[id] = WidgetEntry(id, col = 0, row = startRow, colSpan = colSpan, rowSpan = rowSpan)
            prefs[gridKey] = serializeGrid(entries.values)
        }
    }

    suspend fun removeWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val entries = currentEntries(prefs) - id
            prefs[gridKey] = serializeGrid(entries.values)
        }
    }

    /**
     * Moves or resizes [id] to the given cell rect, then resolves any collisions
     * this creates by pushing whatever it now overlaps straight down, cascading as
     * far as needed - simple push-down rather than packing everything back into a
     * dense layout, because a launcher that quietly reflows widgets you didn't touch
     * is more disorienting than a gap you can decide to close yourself.
     */
    suspend fun placeWidget(id: Int, col: Int, row: Int, colSpan: Int, rowSpan: Int) {
        context.widgetDataStore.edit { prefs ->
            val entries = currentEntries(prefs).toMutableMap()
            val clampedCol = col.coerceIn(0, (WidgetGrid.COLUMNS - colSpan).coerceAtLeast(0))
            entries[id] = WidgetEntry(id, clampedCol, row.coerceAtLeast(0), colSpan, rowSpan)
            resolveCollisions(entries, movedId = id)
            prefs[gridKey] = serializeGrid(entries.values)
        }
    }

    /** Best-effort default size for a widget that hasn't been placed yet: as wide as
     * its declared minimum allows (never wider than the grid), tall enough to clear
     * its declared minimum height in whole cells. */
    private fun defaultSpanFor(id: Int): Pair<Int, Int> {
        val providerInfo = runCatching { AppWidgetManager.getInstance(context).getAppWidgetInfo(id) }.getOrNull()
        val metrics = context.resources.displayMetrics
        val screenWidthDp = metrics.widthPixels / metrics.density
        val cellWidthDp = screenWidthDp / WidgetGrid.COLUMNS
        val minWidthDp = providerInfo?.minWidth?.let { it / metrics.density } ?: 0f
        val minHeightDp = providerInfo?.minHeight?.let { it / metrics.density } ?: 0f
        val colSpan = if (minWidthDp > 0f) {
            kotlin.math.ceil(minWidthDp / cellWidthDp).toInt().coerceIn(WidgetGrid.MIN_COL_SPAN, WidgetGrid.COLUMNS)
        } else {
            WidgetGrid.COLUMNS
        }
        val rowSpan = if (minHeightDp > 0f) {
            kotlin.math.ceil(minHeightDp / WidgetGrid.CELL_HEIGHT_DP)
                .toInt()
                .coerceIn(WidgetGrid.MIN_ROW_SPAN, WidgetGrid.MAX_ROW_SPAN)
        } else {
            2
        }
        return colSpan to rowSpan
    }

    private fun currentEntries(prefs: androidx.datastore.preferences.core.Preferences): Map<Int, WidgetEntry> {
        val raw = prefs[gridKey]
        val list = if (raw != null) {
            parseGrid(raw)
        } else {
            migrateLegacy(prefs[legacyOrderKey], prefs[legacyHeightsKey], prefs[legacyWidthsKey])
        }
        return list.associateBy { it.id }
    }

    private fun overlaps(a: WidgetEntry, b: WidgetEntry): Boolean {
        val colsOverlap = a.col < b.col + b.colSpan && b.col < a.col + a.colSpan
        val rowsOverlap = a.row < b.row + b.rowSpan && b.row < a.row + a.rowSpan
        return colsOverlap && rowsOverlap
    }

    private fun resolveCollisions(entries: MutableMap<Int, WidgetEntry>, movedId: Int) {
        // Bounded rather than a while(true): a real layout converges in a handful of
        // passes, and a guard here just means a pathological case stops pushing
        // instead of hanging, not that anything renders wrong.
        repeat(50) {
            var changed = false
            val current = entries.values.toList()
            for (a in current) {
                for (b in current) {
                    if (a.id == b.id || !overlaps(entries.getValue(a.id), entries.getValue(b.id))) continue
                    // Whichever of the pair isn't the widget the user just placed
                    // gets pushed - if neither is, push the one that was already
                    // lower, so a cascade always moves further down, never in place.
                    val (anchor, victim) = when {
                        a.id == movedId -> a to b
                        b.id == movedId -> b to a
                        a.row <= b.row -> a to b
                        else -> b to a
                    }
                    val anchorNow = entries.getValue(anchor.id)
                    val victimNow = entries.getValue(victim.id)
                    val requiredRow = anchorNow.row + anchorNow.rowSpan
                    if (victimNow.row < requiredRow) {
                        entries[victim.id] = victimNow.copy(row = requiredRow)
                        changed = true
                    }
                }
            }
            if (!changed) return
        }
    }

    /** One-time, read-only conversion from the pre-grid model: each widget becomes
     * its own full-width-or-less row, stacked top to bottom in the order the user
     * had them. This doesn't reproduce the old side-by-side packing exactly, but it
     * keeps every widget present and reasonably sized the moment this update lands,
     * which matters more than an exact pixel match to a layout model that no longer
     * exists. Nothing is written back here - the real grid is only persisted once
     * the user moves or resizes something, at which point this fallback is never
     * consulted again. */
    private fun migrateLegacy(orderRaw: String?, heightsRaw: String?, widthsRaw: String?): List<WidgetEntry> {
        val order = orderRaw?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return emptyList()
        val heights = parseLegacyMap(heightsRaw)
        val widths = parseLegacyMap(widthsRaw)
        var nextRow = 0
        return order.map { id ->
            val widthPercent = (widths[id] ?: 100).coerceIn(10, 100)
            val heightDp = heights[id] ?: (WidgetGrid.CELL_HEIGHT_DP * 2)
            val colSpan = kotlin.math.ceil(widthPercent / 100f * WidgetGrid.COLUMNS)
                .toInt()
                .coerceIn(WidgetGrid.MIN_COL_SPAN, WidgetGrid.COLUMNS)
            val rowSpan = kotlin.math.ceil(heightDp.toFloat() / WidgetGrid.CELL_HEIGHT_DP)
                .toInt()
                .coerceIn(WidgetGrid.MIN_ROW_SPAN, WidgetGrid.MAX_ROW_SPAN)
            val entry = WidgetEntry(id, col = 0, row = nextRow, colSpan = colSpan, rowSpan = rowSpan)
            nextRow += rowSpan
            entry
        }
    }

    private fun parseLegacyMap(raw: String?): Map<Int, Int> =
        raw?.split(",")
            ?.mapNotNull { entry ->
                val (idPart, valuePart) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
                val id = idPart.toIntOrNull() ?: return@mapNotNull null
                val value = valuePart.toIntOrNull() ?: return@mapNotNull null
                id to value
            }
            ?.toMap()
            ?: emptyMap()

    private fun parseGrid(raw: String): List<WidgetEntry> =
        raw.split(",")
            .mapNotNull { chunk ->
                val parts = chunk.split(":")
                if (parts.size != 5) return@mapNotNull null
                val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                val col = parts[1].toIntOrNull() ?: return@mapNotNull null
                val row = parts[2].toIntOrNull() ?: return@mapNotNull null
                val colSpan = parts[3].toIntOrNull() ?: return@mapNotNull null
                val rowSpan = parts[4].toIntOrNull() ?: return@mapNotNull null
                WidgetEntry(id, col, row, colSpan, rowSpan)
            }

    private fun serializeGrid(entries: Collection<WidgetEntry>): String =
        entries.joinToString(",") { "${it.id}:${it.col}:${it.row}:${it.colSpan}:${it.rowSpan}" }
}

object WidgetHostProvider {
    const val HOST_ID = 1024

    @Volatile
    private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        host ?: synchronized(this) {
            host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
        }

    fun manager(context: Context): AppWidgetManager = AppWidgetManager.getInstance(context)
}
