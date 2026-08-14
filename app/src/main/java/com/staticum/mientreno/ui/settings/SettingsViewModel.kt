package com.staticum.mientreno.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.BuildConfig
import com.staticum.mientreno.ui.theme.AppTheme
import com.staticum.mientreno.update.ApkDownloader
import com.staticum.mientreno.update.UpdateChecker
import com.staticum.mientreno.update.UpdateInfo
import com.staticum.mientreno.util.ThemePreferences
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val appContext: Context) : ViewModel() {

    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    val selectedTheme: StateFlow<AppTheme> = ThemePreferences.selectedTheme

    fun selectTheme(theme: AppTheme) {
        ThemePreferences.setTheme(theme)
    }

    fun checkForUpdate() {
        updateState = UpdateState.Checking
        viewModelScope.launch {
            val info = UpdateChecker.fetchLatest()
            updateState = when {
                info == null -> UpdateState.Error("No se pudo comprobar si hay actualizaciones. Revisa tu conexión.")
                info.versionCode > currentVersionCode -> UpdateState.Available(info)
                else -> UpdateState.UpToDate
            }
        }
    }

    fun downloadAndInstall(info: UpdateInfo) {
        updateState = UpdateState.Downloading
        viewModelScope.launch {
            runCatching { ApkDownloader.download(appContext, info.apkUrl) }
                .onSuccess { uri -> updateState = UpdateState.ReadyToInstall(uri) }
                .onFailure { updateState = UpdateState.Error("No se pudo descargar la actualización.") }
        }
    }

    fun resetToIdle() {
        updateState = UpdateState.Idle
    }
}
