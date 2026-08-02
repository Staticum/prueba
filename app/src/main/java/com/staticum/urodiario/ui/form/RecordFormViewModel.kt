package com.staticum.urodiario.ui.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.urodiario.data.MicturitionRecord
import com.staticum.urodiario.data.MicturitionRepository
import com.staticum.urodiario.data.UrineColor
import com.staticum.urodiario.data.UrineOdor
import kotlinx.coroutines.launch

class RecordFormViewModel(
    private val repository: MicturitionRepository,
    private val recordId: Long?
) : ViewModel() {

    var state by mutableStateOf(RecordFormState(isEditing = recordId != null, isLoading = recordId != null))
        private set

    init {
        val idToLoad = recordId
        if (idToLoad != null) {
            viewModelScope.launch {
                repository.observeById(idToLoad).collect { data ->
                    if (data != null) {
                        state = state.copy(
                            id = data.record.id,
                            dateTimeMillis = data.record.dateTimeMillis,
                            volumeMl = data.record.volumeMl?.toString() ?: "",
                            durationSeconds = data.record.durationSeconds?.toString() ?: "",
                            color = data.record.color,
                            odor = data.record.odor,
                            urgency = data.record.urgency,
                            painLevel = data.record.painLevel,
                            hasBlood = data.record.hasBlood,
                            hasLeakage = data.record.hasLeakage,
                            isNocturnal = data.record.isNocturnal,
                            fluidIntakeMl = data.record.fluidIntakeMl?.toString() ?: "",
                            notes = data.record.notes ?: "",
                            existingPhotos = data.photos,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateDateTime(millis: Long) {
        state = state.copy(dateTimeMillis = millis)
    }

    fun updateVolume(value: String) {
        state = state.copy(volumeMl = value.filter { it.isDigit() }.take(5))
    }

    fun updateDuration(value: String) {
        state = state.copy(durationSeconds = value.filter { it.isDigit() }.take(4))
    }

    fun updateFluidIntake(value: String) {
        state = state.copy(fluidIntakeMl = value.filter { it.isDigit() }.take(5))
    }

    fun updateColor(color: UrineColor) {
        state = state.copy(color = color)
    }

    fun updateOdor(odor: UrineOdor) {
        state = state.copy(odor = odor)
    }

    fun updateUrgency(value: Int) {
        state = state.copy(urgency = value)
    }

    fun updatePainLevel(value: Int) {
        state = state.copy(painLevel = value)
    }

    fun updateHasBlood(value: Boolean) {
        state = state.copy(hasBlood = value)
    }

    fun updateHasLeakage(value: Boolean) {
        state = state.copy(hasLeakage = value)
    }

    fun updateIsNocturnal(value: Boolean) {
        state = state.copy(isNocturnal = value)
    }

    fun updateNotes(value: String) {
        state = state.copy(notes = value)
    }

    fun addPhoto(uri: String) {
        state = state.copy(newPhotoUris = state.newPhotoUris + uri)
    }

    fun removePhoto(uri: String) {
        val existing = state.existingPhotos.find { it.uri == uri }
        state = if (existing != null) {
            state.copy(removedPhotoIds = state.removedPhotoIds + existing.id)
        } else {
            state.copy(newPhotoUris = state.newPhotoUris - uri)
        }
    }

    fun save() {
        viewModelScope.launch {
            val record = MicturitionRecord(
                id = state.id,
                dateTimeMillis = state.dateTimeMillis,
                volumeMl = state.volumeMl.toIntOrNull(),
                durationSeconds = state.durationSeconds.toIntOrNull(),
                color = state.color,
                odor = state.odor,
                urgency = state.urgency,
                painLevel = state.painLevel,
                hasBlood = state.hasBlood,
                hasLeakage = state.hasLeakage,
                isNocturnal = state.isNocturnal,
                fluidIntakeMl = state.fluidIntakeMl.toIntOrNull(),
                notes = state.notes.ifBlank { null }
            )
            val savedId = repository.saveRecord(record, state.newPhotoUris)
            state.existingPhotos.filter { it.id in state.removedPhotoIds }.forEach { photo ->
                repository.deletePhoto(photo)
            }
            state = state.copy(id = savedId, isSaved = true)
        }
    }
}
