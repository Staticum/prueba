package com.staticum.urodiario.ui.form

import com.staticum.urodiario.data.RecordPhoto
import com.staticum.urodiario.data.UrineColor
import com.staticum.urodiario.data.UrineOdor

data class RecordFormState(
    val id: Long = 0,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val volumeMl: String = "",
    val durationSeconds: String = "",
    val color: UrineColor = UrineColor.AMARILLO,
    val odor: UrineOdor = UrineOdor.NORMAL,
    val urgency: Int = 0,
    val painLevel: Int = 0,
    val hasBlood: Boolean = false,
    val hasLeakage: Boolean = false,
    val isNocturnal: Boolean = false,
    val fluidIntakeMl: String = "",
    val notes: String = "",
    val existingPhotos: List<RecordPhoto> = emptyList(),
    val newPhotoUris: List<String> = emptyList(),
    val removedPhotoIds: Set<Long> = emptySet(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    val displayPhotoUris: List<String>
        get() = existingPhotos.filter { it.id !in removedPhotoIds }.map { it.uri } + newPhotoUris

    val isValid: Boolean
        get() = dateTimeMillis > 0
}
