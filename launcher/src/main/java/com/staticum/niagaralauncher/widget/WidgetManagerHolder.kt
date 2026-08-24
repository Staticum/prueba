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

data class WidgetEntry(val id: Int, val heightDp: Int?)

/** Persists which app-widget ids the user has placed on the home screen, in the
 * order the user arranged them, plus an optional custom height per widget. */
class WidgetRepository(private val context: Context) {

    private val orderKey = stringPreferencesKey("widget_order")
    private val heightsKey = stringPreferencesKey("widget_heights")

    val widgetsFlow: Flow<List<WidgetEntry>> = context.widgetDataStore.data.map { prefs ->
        val order = parseOrder(prefs[orderKey])
        val heights = parseHeights(prefs[heightsKey])
        order.map { id -> WidgetEntry(id, heights[id]) }
    }

    val widgetIdsFlow: Flow<List<Int>> = widgetsFlow.map { list -> list.map { it.id } }

    suspend fun addWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val order = parseOrder(prefs[orderKey])
            prefs[orderKey] = (order + id).joinToString(",")
        }
    }

    suspend fun removeWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val order = parseOrder(prefs[orderKey])
            prefs[orderKey] = (order - id).joinToString(",")
            val heights = parseHeights(prefs[heightsKey]) - id
            prefs[heightsKey] = heights.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun moveWidget(id: Int, delta: Int) {
        context.widgetDataStore.edit { prefs ->
            val order = parseOrder(prefs[orderKey]).toMutableList()
            val index = order.indexOf(id)
            val target = index + delta
            if (index < 0 || target < 0 || target >= order.size) return@edit
            val tmp = order[index]
            order[index] = order[target]
            order[target] = tmp
            prefs[orderKey] = order.joinToString(",")
        }
    }

    suspend fun setWidgetHeight(id: Int, heightDp: Int) {
        context.widgetDataStore.edit { prefs ->
            val heights = parseHeights(prefs[heightsKey]) + (id to heightDp)
            prefs[heightsKey] = heights.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    private fun parseOrder(raw: String?): List<Int> =
        raw?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

    private fun parseHeights(raw: String?): Map<Int, Int> =
        raw?.split(",")
            ?.mapNotNull { entry ->
                val (idPart, heightPart) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
                val id = idPart.toIntOrNull() ?: return@mapNotNull null
                val height = heightPart.toIntOrNull() ?: return@mapNotNull null
                id to height
            }
            ?.toMap()
            ?: emptyMap()
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
