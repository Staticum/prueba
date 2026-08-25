package com.staticum.niagaralauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.data.PreferencesRepository
import com.staticum.niagaralauncher.data.ScreenTintMode
import com.staticum.niagaralauncher.widget.WidgetEntry
import com.staticum.niagaralauncher.widget.WidgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val prefs: LauncherPrefs = LauncherPrefs(),
    val widgets: List<WidgetEntry> = emptyList(),
) {
    val widgetIds: List<Int> get() = widgets.map { it.id }
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val widgetRepository: WidgetRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.prefsFlow, widgetRepository.widgetsFlow,
    ) { prefs, widgets -> SettingsUiState(prefs, widgets) }
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

    fun setScreenTintMode(mode: ScreenTintMode) =
        viewModelScope.launch { preferencesRepository.setScreenTintMode(mode) }

    fun setSoundId(id: String) = viewModelScope.launch { preferencesRepository.setSoundId(id) }

    fun setSoundVolume(volume: Float) =
        viewModelScope.launch { preferencesRepository.setSoundVolume(volume) }

    fun setIndexWaveOffset(offsetDp: Float) =
        viewModelScope.launch { preferencesRepository.setIndexWaveOffset(offsetDp) }

    fun setCustomAccentColor(argb: Int) =
        viewModelScope.launch { preferencesRepository.setCustomAccentColor(argb) }

    fun setAmbientLockEnabled(enabled: Boolean) =
        viewModelScope.launch { preferencesRepository.setAmbientLockEnabled(enabled) }

    fun addWidget(id: Int) = viewModelScope.launch { widgetRepository.addWidgetId(id) }

    fun removeWidget(id: Int) = viewModelScope.launch { widgetRepository.removeWidgetId(id) }

    fun moveWidget(id: Int, delta: Int) = viewModelScope.launch { widgetRepository.moveWidget(id, delta) }

    fun setWidgetHeight(id: Int, heightDp: Int) =
        viewModelScope.launch { widgetRepository.setWidgetHeight(id, heightDp) }

    fun setWidgetWidth(id: Int, widthPercent: Int) =
        viewModelScope.launch { widgetRepository.setWidgetWidth(id, widthPercent) }

    fun toggleFavoriteApp(appKey: String, favorite: Boolean) =
        viewModelScope.launch { preferencesRepository.toggleFavoriteApp(appKey, favorite) }

    fun moveFavoriteApp(appKey: String, delta: Int) =
        viewModelScope.launch { preferencesRepository.moveFavoriteApp(appKey, delta) }
}
