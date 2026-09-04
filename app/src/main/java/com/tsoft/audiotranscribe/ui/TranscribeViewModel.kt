package com.tsoft.audiotranscribe.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.tsoft.audiotranscribe.data.AppDatabase
import com.tsoft.audiotranscribe.data.SpeakerNameEntity
import com.tsoft.audiotranscribe.data.TranscriptResponse
import com.tsoft.audiotranscribe.data.TranscriptionRepository
import com.tsoft.audiotranscribe.data.TranscriptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val apiKey: String = "",
    val audioUri: Uri? = null,
    val transcriptionState: TranscriptionState = TranscriptionState.Idle,
    val speakerNames: Map<String, String> = emptyMap()
)

class TranscribeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranscriptionRepository(application)
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "audio-transcribe.db")
        .fallbackToDestructiveMigration()
        .build()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setApiKey(key: String) {
        _state.value = _state.value.copy(apiKey = key)
    }

    fun setAudioUri(uri: Uri) {
        _state.value = _state.value.copy(audioUri = uri, transcriptionState = TranscriptionState.Idle)
    }

    fun startTranscription() {
        val current = _state.value
        val uri = current.audioUri ?: return
        if (current.apiKey.isBlank()) {
            _state.value = current.copy(
                transcriptionState = TranscriptionState.Failed("Ingresa tu API key de AssemblyAI antes de continuar")
            )
            return
        }
        viewModelScope.launch {
            repository.transcribe(current.apiKey, uri) { newState ->
                _state.value = _state.value.copy(transcriptionState = newState)
                if (newState is TranscriptionState.Done) {
                    loadSpeakerNames(newState.result)
                }
            }
        }
    }

    private fun loadSpeakerNames(result: TranscriptResponse) {
        viewModelScope.launch {
            val saved = db.speakerNameDao().getForTranscript(result.id)
            _state.value = _state.value.copy(
                speakerNames = saved.associate { it.speakerLabel to it.displayName }
            )
        }
    }

    fun renameSpeaker(transcriptId: String, speakerLabel: String, newName: String) {
        viewModelScope.launch {
            db.speakerNameDao().upsert(SpeakerNameEntity(transcriptId, speakerLabel, newName))
            _state.value = _state.value.copy(
                speakerNames = _state.value.speakerNames + (speakerLabel to newName)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        db.close()
    }
}
