package com.staticum.urodiario.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "record_photos",
    foreignKeys = [
        ForeignKey(
            entity = MicturitionRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("recordId")]
)
data class RecordPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val uri: String
)
