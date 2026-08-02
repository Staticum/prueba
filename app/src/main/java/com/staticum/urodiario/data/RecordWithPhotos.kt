package com.staticum.urodiario.data

import androidx.room.Embedded
import androidx.room.Relation

data class RecordWithPhotos(
    @Embedded val record: MicturitionRecord,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val photos: List<RecordPhoto>
)
