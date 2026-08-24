package com.staticum.niagaralauncher.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.staticum.niagaralauncher.data.AppRepository
import com.staticum.niagaralauncher.data.PreferencesRepository
import com.staticum.niagaralauncher.ui.home.HomeViewModel
import com.staticum.niagaralauncher.ui.settings.SettingsViewModel
import com.staticum.niagaralauncher.widget.WidgetRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val appRepository by lazy { AppRepository(context.applicationContext) }
    private val preferencesRepository by lazy { PreferencesRepository(context.applicationContext) }
    private val widgetRepository by lazy { WidgetRepository(context.applicationContext) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(appRepository, preferencesRepository) as T
        SettingsViewModel::class.java -> SettingsViewModel(preferencesRepository, widgetRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}
