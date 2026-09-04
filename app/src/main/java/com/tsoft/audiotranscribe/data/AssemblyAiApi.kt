package com.tsoft.audiotranscribe.data

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Cliente de la API de AssemblyAI (https://www.assemblyai.com/docs).
 * Requiere una API key propia del usuario (ver AssemblyAiApi.BASE_URL y el
 * campo "API key" en la pantalla principal de la app).
 */
interface AssemblyAiApi {

    @POST("v2/upload")
    suspend fun uploadAudio(@Body body: RequestBody): UploadResponse

    @POST("v2/transcript")
    suspend fun createTranscript(@Body request: CreateTranscriptRequest): CreateTranscriptResponse

    @GET("v2/transcript/{id}")
    suspend fun getTranscript(@Path("id") id: String): TranscriptResponse

    companion object {
        const val BASE_URL = "https://api.assemblyai.com/"
    }
}
