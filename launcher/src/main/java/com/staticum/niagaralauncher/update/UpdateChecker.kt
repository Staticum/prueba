package com.staticum.niagaralauncher.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Talks to the GitHub Releases API for [BuildConfig.UPDATE_REPO] to find newer builds of this
 * app published by the CI workflow (tag pattern "launcher-vNNN", asset name "*.apk").
 */
class UpdateChecker(private val context: Context, private val repo: String) {

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val json = get("https://api.github.com/repos/$repo/releases/latest")
                ?: return@withContext UpdateCheckResult.Error("No se pudo contactar a GitHub")
            val obj = JSONObject(json)
            val tag = obj.optString("tag_name")
            val remoteVersionCode = tag.substringAfterLast("v").toIntOrNull()
                ?: return@withContext UpdateCheckResult.Error("Formato de versión desconocido: $tag")

            if (remoteVersionCode <= currentVersionCode) {
                return@withContext UpdateCheckResult.UpToDate
            }

            val assets = obj.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (apkUrl == null) {
                return@withContext UpdateCheckResult.Error("El release no tiene un APK adjunto")
            }

            UpdateCheckResult.Available(
                UpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = obj.optString("name", tag),
                    downloadUrl = apkUrl,
                    releaseNotes = obj.optString("body"),
                ),
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun downloadApk(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "launcher-v${info.versionCode}.apk")

        val connection = URL(info.downloadUrl).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connect()

        val totalSize = connection.contentLength.takeIf { it > 0 } ?: -1
        var downloaded = 0

        connection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalSize > 0) onProgress(downloaded.toFloat() / totalSize)
                }
            }
        }
        connection.disconnect()
        target
    }

    private fun get(urlString: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
