package com.staticum.urodiario.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "micturition_records")
data class MicturitionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimeMillis: Long,
    val volumeMl: Int? = null,
    val durationSeconds: Int? = null,
    val color: UrineColor = UrineColor.AMARILLO,
    val odor: UrineOdor = UrineOdor.NORMAL,
    val urgency: Int = 0,
    val painLevel: Int = 0,
    val hasBlood: Boolean = false,
    val hasLeakage: Boolean = false,
    val isNocturnal: Boolean = false,
    val fluidIntakeMl: Int? = null,
    val notes: String? = null
)
