package com.staticum.niagaralauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.AppRepository
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.data.PreferencesRepository
import com.staticum.niagaralauncher.data.SwipeDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val allApps: List<AppInfo> = emptyList(),
    val query: String = "",
    val prefs: LauncherPrefs = LauncherPrefs(),
    val isLoading: Boolean = true,
) {
    val visibleApps: List<AppInfo>
        get() = allApps
            .filter { it.key !in prefs.hiddenApps }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    fun favoriteFor(direction: SwipeDirection): AppInfo? =
        prefs.gestureFavorites[direction]?.let { key -> allApps.firstOrNull { it.key == key } }
}

class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        apps, query, preferencesRepository.prefsFlow, loading,
    ) { apps, query, prefs, loading ->
        HomeUiState(allApps = apps, query = query, prefs = prefs, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            loading.value = true
            apps.value = appRepository.loadInstalledApps()
            loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleHidden(app: AppInfo, hidden: Boolean) {
        viewModelScope.launch { preferencesRepository.toggleHiddenApp(app.key, hidden) }
    }

    fun setGestureFavorite(direction: SwipeDirection, app: AppInfo?) {
        viewModelScope.launch { preferencesRepository.setGestureFavorite(direction, app?.key) }
    }
}
