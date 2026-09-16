package com.staticum.diariocalorico.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.update.ApkDownloader
import com.staticum.diariocalorico.update.ReleaseInfo
import com.staticum.diariocalorico.update.UpdateCheckResult
import com.staticum.diariocalorico.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val goals: DailyGoals = DailyGoals(),
    val versionName: String = "",
    val updateStatus: String = "",
    val updateAvailable: ReleaseInfo? = null,
    val downloadProgress: Float? = null
)

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val updateChecker = UpdateChecker()
    private val apkDownloader = ApkDownloader(appContext)

    init {
        val versionName = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "?"
        } catch (e: PackageManager.NameNotFoundException) { "?" }

        _uiState.value = _uiState.value.copy(
            apiKey = userPreferences.getGeminiApiKey() ?: "",
            versionName = versionName
        )

        userPreferences.dailyGoals.onEach { goals ->
            _uiState.value = _uiState.value.copy(goals = goals)
        }.launchIn(viewModelScope)
    }

    fun saveApiKey(key: String) {
        userPreferences.setGeminiApiKey(key)
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun saveGoals(goals: DailyGoals) {
        viewModelScope.launch { userPreferences.setDailyGoals(goals) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateStatus = "Buscando actualizaciones...")
            when (val result = updateChecker.checkForUpdate(_uiState.value.versionName)) {
                is UpdateCheckResult.UpdateAvailable -> _uiState.value = _uiState.value.copy(
                    updateStatus = "Nueva versión disponible: ${result.release.versionName}",
                    updateAvailable = result.release
                )
                UpdateCheckResult.UpToDate -> _uiState.value = _uiState.value.copy(
                    updateStatus = "Ya tienes la última versión", updateAvailable = null
                )
                is UpdateCheckResult.Error -> _uiState.value = _uiState.value.copy(
                    updateStatus = result.message, updateAvailable = null
                )
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val release = _uiState.value.updateAvailable ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(downloadProgress = 0f)
            val result = apkDownloader.downloadAndInstall(release) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }
            _uiState.value = _uiState.value.copy(
                downloadProgress = null,
                updateStatus = if (result.isSuccess) "Instalando..." else "Error al descargar: ${result.exceptionOrNull()?.message}"
            )
        }
    }
}
