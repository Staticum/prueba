package com.staticum.mientreno.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import com.staticum.mientreno.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "mientreno_settings"
    private const val KEY_THEME = "selected_theme"

    private lateinit var prefs: SharedPreferences

    private val _selectedTheme = MutableStateFlow(AppTheme.OSCURO)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_THEME, null)
        _selectedTheme.value = AppTheme.entries.firstOrNull { it.name == stored }
            ?: defaultForSystem(context)
    }

    fun setTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    private fun defaultForSystem(context: Context): AppTheme {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) AppTheme.OSCURO else AppTheme.CLARO
    }
}
