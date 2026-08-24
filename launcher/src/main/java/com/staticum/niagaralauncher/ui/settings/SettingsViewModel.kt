package com.staticum.niagaralauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.data.PreferencesRepository
import com.staticum.niagaralauncher.widget.WidgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val prefs: LauncherPrefs = LauncherPrefs(),
    val widgetIds: List<Int> = emptyList(),
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val widgetRepository: WidgetRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.prefsFlow, widgetRepository.widgetIdsFlow,
    ) { prefs, widgetIds -> SettingsUiState(prefs, widgetIds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setPalette(id: String) = viewModelScope.launch { preferencesRepository.setPalette(id) }

    fun setUseWallpaper(enabled: Boolean) =
        viewModelScope.launch { preferencesRepository.setUseWallpaper(enabled) }

    fun setWallpaperUri(uri: String?) =
        viewModelScope.launch { preferencesRepository.setWallpaperUri(uri) }

    fun setIconSizeFactor(factor: Float) =
        viewModelScope.launch { preferencesRepository.setIconSizeFactor(factor) }

    fun setMonochromeIcons(enabled: Boolean) =
        viewModelScope.launch { preferencesRepository.setMonochromeIcons(enabled) }

    fun addWidget(id: Int) = viewModelScope.launch { widgetRepository.addWidgetId(id) }

    fun removeWidget(id: Int) = viewModelScope.launch { widgetRepository.removeWidgetId(id) }
}
