package com.tsoft.audiotranscribe.data

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(val upload_url: String)

@Serializable
data class CreateTranscriptRequest(
    val audio_url: String,
    val speaker_labels: Boolean = true,
    val language_code: String = "es"
)

@Serializable
data class CreateTranscriptResponse(val id: String)

@Serializable
data class Utterance(
    val speaker: String,
    val text: String,
    val start: Long,
    val end: Long
)

@Serializable
data class TranscriptResponse(
    val id: String,
    val status: String,
    val error: String? = null,
    val utterances: List<Utterance>? = null
)
