package com.staticum.niagaralauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.AppRepository
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.data.PreferencesRepository
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.data.UsageRepository
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
    /** Own launch counter, keyed by `AppInfo.key`. */
    val localScores: Map<String, Float> = emptyMap(),
    /** System usage stats, keyed by *package* - the system aggregates per package,
     * not per activity, so every activity of a package shares its score. */
    val systemScores: Map<String, Float> = emptyMap(),
) {
    val visibleApps: List<AppInfo>
        get() = allApps
            .filter { it.key !in prefs.hiddenApps }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    /** System data wins when the user opted in and the permission actually produced
     * something; otherwise the local counter, which always works. */
    private val useSystem: Boolean
        get() = prefs.useSystemUsageStats && systemScores.isNotEmpty()

    fun usageScoreFor(app: AppInfo): Float =
        if (useSystem) systemScores[app.packageName] ?: 0f else localScores[app.key] ?: 0f

    /**
     * The apps for the "Frecuentes" section. Empty when there isn't enough signal
     * yet - a half-filled block on a cold start looks broken rather than helpful.
     */
    val frequentApps: List<AppInfo>
        get() {
            if (!prefs.frequentsEnabled) return emptyList()
            val ranked = allApps
                .filter { it.key !in prefs.hiddenApps }
                .map { it to usageScoreFor(it) }
                .filter { it.second > 0f }
                .sortedByDescending { it.second }
            if (ranked.size < MIN_FREQUENTS) return emptyList()
            return ranked.take(prefs.frequentsCount).map { it.first }
        }

    fun favoriteFor(direction: SwipeDirection): AppInfo? =
        prefs.gestureFavorites[direction]?.let { key -> allApps.firstOrNull { it.key == key } }

    private companion object {
        /** Below this, the section has nothing useful to say yet. */
        const val MIN_FREQUENTS = 3
    }
}

class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    private val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val systemScores = MutableStateFlow<Map<String, Float>>(emptyMap())

    private val scores = combine(
        usageRepository.localScoresFlow, systemScores,
    ) { local, system -> local to system }

    val uiState: StateFlow<HomeUiState> = combine(
        apps, query, preferencesRepository.prefsFlow, loading, scores,
    ) { apps, query, prefs, loading, scores ->
        HomeUiState(
            allApps = apps,
            query = query,
            prefs = prefs,
            isLoading = loading,
            localScores = scores.first,
            systemScores = scores.second,
        )
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
        refreshSystemUsage()
    }

    /** Cheap no-op when the permission isn't granted: the repository returns an
     * empty map and the local counter keeps driving the ranking. */
    fun refreshSystemUsage() {
        viewModelScope.launch { systemScores.value = usageRepository.systemScoresByPackage() }
    }

    fun recordLaunch(app: AppInfo) {
        viewModelScope.launch { usageRepository.recordLaunch(app.key) }
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
