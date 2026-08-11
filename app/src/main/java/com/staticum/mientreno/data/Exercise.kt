package com.staticum.mientreno.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val equipment: String? = null,
    val instructions: String? = null,
    val defaultRestSeconds: Int = 60,
    val isCustom: Boolean = false
)
