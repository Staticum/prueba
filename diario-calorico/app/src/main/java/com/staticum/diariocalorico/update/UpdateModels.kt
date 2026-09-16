package com.staticum.diariocalorico.update

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val htmlUrl: String,
    val apkDownloadUrl: String,
    val releaseNotes: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
