package com.tsoft.audiotranscribe.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

sealed class TranscriptionState {
    data object Idle : TranscriptionState()
    data class Uploading(val progress: String) : TranscriptionState()
    data class Processing(val transcriptId: String) : TranscriptionState()
    data class Done(val result: TranscriptResponse) : TranscriptionState()
    data class Failed(val message: String) : TranscriptionState()
}

class TranscriptionRepository(private val context: Context) {

    private fun apiFor(apiKey: String): AssemblyAiApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("authorization", apiKey)
                    .build()
                chain.proceed(request)
            })
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(AssemblyAiApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(AssemblyAiApi::class.java)
    }

    /** Copia el audio elegido por el usuario a un archivo local temporal para poder subirlo. */
    private fun copyToCache(uri: Uri): File {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir el archivo de audio seleccionado")
        val outFile = File(context.cacheDir, "audio_upload_${System.currentTimeMillis()}")
        input.use { inStream ->
            outFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
        }
        return outFile
    }

    suspend fun transcribe(
        apiKey: String,
        audioUri: Uri,
        onState: suspend (TranscriptionState) -> Unit
    ) {
        val api = apiFor(apiKey)
        try {
            onState(TranscriptionState.Uploading("Subiendo audio..."))
            val file = copyToCache(audioUri)
            val body = file.asRequestBody("application/octet-stream".toMediaType())
            val uploadResponse = api.uploadAudio(body)
            file.delete()

            val created = api.createTranscript(
                CreateTranscriptRequest(audio_url = uploadResponse.upload_url)
            )
            onState(TranscriptionState.Processing(created.id))

            while (true) {
                delay(3000)
                val result = api.getTranscript(created.id)
                when (result.status) {
                    "completed" -> {
                        onState(TranscriptionState.Done(result))
                        return
                    }
                    "error" -> {
                        onState(TranscriptionState.Failed(result.error ?: "Error desconocido"))
                        return
                    }
                    else -> onState(TranscriptionState.Processing(created.id))
                }
            }
        } catch (e: Exception) {
            onState(TranscriptionState.Failed(e.message ?: "Error de conexión"))
        }
    }
}
