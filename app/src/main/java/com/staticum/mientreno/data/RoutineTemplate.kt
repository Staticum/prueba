package com.staticum.mientreno.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routine_templates")
data class RoutineTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_exercises")
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val orderIndex: Int,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Int? = null,
    val restSeconds: Int = 60,
    val notes: String? = null
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineTemplate,
    @Relation(
        entity = RoutineExercise::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<RoutineExercise>
)
