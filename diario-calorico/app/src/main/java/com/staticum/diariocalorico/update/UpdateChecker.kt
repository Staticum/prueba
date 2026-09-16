package com.staticum.diariocalorico.update

import com.staticum.diariocalorico.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Los releases de actualización se publican en el repo GitHub del proyecto (staticum/prueba)
 * con tags con prefijo "diario-calorico-v", ya que la app vive en una carpeta de ese repo.
 */
class UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases")
                .addHeader("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("No se pudo consultar releases (${response.code})")
                }
                val bodyString = response.body?.string().orEmpty()
                val releases = json.parseToJsonElement(bodyString) as JsonArray

                val match = releases
                    .map { it as JsonObject }
                    .firstOrNull { release ->
                        val tag = (release["tag_name"] as? JsonPrimitive)?.content ?: ""
                        tag.startsWith(BuildConfig.RELEASE_TAG_PREFIX)
                    } ?: return@withContext UpdateCheckResult.UpToDate

                val tagName = (match["tag_name"] as JsonPrimitive).content
                val remoteVersion = tagName.removePrefix(BuildConfig.RELEASE_TAG_PREFIX)

                if (!isNewer(remoteVersion, currentVersionName)) {
                    return@withContext UpdateCheckResult.UpToDate
                }

                val assets = match["assets"] as? JsonArray
                val apkAsset = assets?.map { it as JsonObject }
                    ?.firstOrNull { asset ->
                        val name = (asset["name"] as? JsonPrimitive)?.content ?: ""
                        name.endsWith(".apk")
                    } ?: return@withContext UpdateCheckResult.Error("El release $tagName no tiene un APK adjunto")

                val downloadUrl = (apkAsset["browser_download_url"] as JsonPrimitive).content
                val htmlUrl = (match["html_url"] as? JsonPrimitive)?.content ?: ""
                val notes = (match["body"] as? JsonPrimitive)?.content ?: ""

                UpdateCheckResult.UpdateAvailable(
                    ReleaseInfo(
                        tagName = tagName,
                        versionName = remoteVersion,
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = downloadUrl,
                        releaseNotes = notes
                    )
                )
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Error al chequear actualizaciones: ${e.message}")
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
        val r = parts(remote)
        val c = parts(current)
        val size = maxOf(r.size, c.size)
        for (i in 0 until size) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
