package com.staticum.diariocalorico.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ApkDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAndInstall(release: ReleaseInfo, onProgress: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(release.apkDownloadUrl).build()
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "diario-calorico-${release.versionName}.apk")

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Descarga falló (${response.code})"))
                    }
                    val body = response.body ?: return@withContext Result.failure(Exception("Sin contenido"))
                    val total = body.contentLength().takeIf { it > 0 }
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(8 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (total != null) onProgress(downloaded.toFloat() / total)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) { launchInstall(target) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun launchInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
