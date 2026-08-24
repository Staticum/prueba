package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "launcher_widgets")

/** Persists which app-widget ids the user has placed on the home screen. */
class WidgetRepository(private val context: Context) {

    private val key = stringSetPreferencesKey("widget_ids")

    val widgetIdsFlow: Flow<List<Int>> = context.widgetDataStore.data.map { prefs ->
        (prefs[key] ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted()
    }

    suspend fun addWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + id.toString()
        }
    }

    suspend fun removeWidgetId(id: Int) {
        context.widgetDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current - id.toString()
        }
    }
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
