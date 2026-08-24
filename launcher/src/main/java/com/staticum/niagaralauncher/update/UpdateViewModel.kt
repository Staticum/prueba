package com.staticum.niagaralauncher.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class Available(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUiState()
    data class ReadyToInstall(val file: File) : UpdateUiState()
    data class Failed(val message: String) : UpdateUiState()
}

class UpdateViewModel(
    private val checker: UpdateChecker,
    private val currentVersionCode: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = UpdateUiState.Checking
            _state.value = when (val result = checker.checkForUpdate(currentVersionCode)) {
                is UpdateCheckResult.Available -> UpdateUiState.Available(result.info)
                is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.Error -> UpdateUiState.Failed(result.message)
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(info, 0f)
            try {
                val file = checker.downloadApk(info) { progress ->
                    _state.value = UpdateUiState.Downloading(info, progress)
                }
                _state.value = UpdateUiState.ReadyToInstall(file)
            } catch (e: Exception) {
                _state.value = UpdateUiState.Failed(e.message ?: "Error al descargar")
            }
        }
    }
}
