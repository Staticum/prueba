package com.staticum.mientreno.ui.settings

import android.net.Uri
import com.staticum.mientreno.update.UpdateInfo

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object Downloading : UpdateState
    data class ReadyToInstall(val uri: Uri) : UpdateState
    data class Error(val message: String) : UpdateState
}
