package com.staticum.mientreno.ui.routines

import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.ExerciseType
import com.staticum.mientreno.data.RoutineExercise

data class DraftExerciseItem(
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val sets: String = "3",
    val reps: String = "10",
    val weightKg: String = "",
    val durationSeconds: String = "",
    val distanceMeters: String = "",
    val restSeconds: String = "60",
    val notes: String = ""
) {
    companion object {
        fun fromExercise(exercise: Exercise): DraftExerciseItem = DraftExerciseItem(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            category = exercise.category,
            type = exercise.type,
            restSeconds = exercise.defaultRestSeconds.toString(),
            durationSeconds = if (exercise.type == ExerciseType.CARDIO) "600" else ""
        )

        fun fromRoutineExercise(item: RoutineExercise): DraftExerciseItem = DraftExerciseItem(
            exerciseId = item.exerciseId,
            exerciseName = item.exerciseName,
            category = item.category,
            type = item.type,
            sets = item.targetSets?.toString() ?: "",
            reps = item.targetReps?.toString() ?: "",
            weightKg = item.targetWeightKg?.toString() ?: "",
            durationSeconds = item.targetDurationSeconds?.toString() ?: "",
            distanceMeters = item.targetDistanceMeters?.toString() ?: "",
            restSeconds = item.restSeconds.toString(),
            notes = item.notes ?: ""
        )
    }

    fun toRoutineExercise(routineId: Long, orderIndex: Int): RoutineExercise = RoutineExercise(
        routineId = routineId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        category = category,
        type = type,
        orderIndex = orderIndex,
        targetSets = sets.toIntOrNull(),
        targetReps = reps.toIntOrNull(),
        targetWeightKg = weightKg.toDoubleOrNull(),
        targetDurationSeconds = durationSeconds.toIntOrNull(),
        targetDistanceMeters = distanceMeters.toIntOrNull(),
        restSeconds = restSeconds.toIntOrNull() ?: 60,
        notes = notes.takeIf { it.isNotBlank() }
    )
}
