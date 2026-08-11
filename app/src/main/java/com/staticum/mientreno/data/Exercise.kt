package com.staticum.mientreno.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val measureType: MeasureType,
    val equipment: String? = null,
    val instructions: String? = null,
    val muscleGroups: String? = null,
    val technique: String? = null,
    val defaultRestSeconds: Int = 60,
    val suggestedReps: Int? = null,
    val suggestedWeightKg: Double? = null,
    val suggestedDurationSeconds: Int? = null,
    val suggestedDistanceMeters: Int? = null,
    val isCustom: Boolean = false
)
