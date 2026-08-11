package com.staticum.mientreno.ui.routines

import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.data.RoutineExercise

data class DraftExerciseItem(
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val measureType: MeasureType,
    val sets: String = "1",
    val reps: String = "",
    val weightKg: String = "",
    val durationSeconds: String = "",
    val distanceMeters: String = "",
    val restAfterSeconds: String = "30",
    val notes: String = ""
) {
    companion object {
        fun fromExercise(exercise: Exercise): DraftExerciseItem = DraftExerciseItem(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            category = exercise.category,
            measureType = exercise.measureType,
            reps = exercise.suggestedReps?.toString() ?: if (exercise.measureType == MeasureType.REPS) "10" else "",
            weightKg = exercise.suggestedWeightKg?.toString() ?: "",
            durationSeconds = exercise.suggestedDurationSeconds?.toString()
                ?: if (exercise.measureType == MeasureType.TIME) "30" else "",
            distanceMeters = exercise.suggestedDistanceMeters?.toString() ?: "",
            restAfterSeconds = exercise.defaultRestSeconds.toString()
        )

        fun fromRoutineExercise(item: RoutineExercise): DraftExerciseItem = DraftExerciseItem(
            exerciseId = item.exerciseId,
            exerciseName = item.exerciseName,
            category = item.category,
            measureType = item.measureType,
            reps = item.targetReps?.toString() ?: "",
            weightKg = item.targetWeightKg?.toString() ?: "",
            durationSeconds = item.targetDurationSeconds?.toString() ?: "",
            distanceMeters = item.targetDistanceMeters?.toString() ?: "",
            restAfterSeconds = item.restAfterSeconds.toString(),
            notes = item.notes ?: ""
        )
    }

    fun toRoutineExercise(orderIndex: Int): RoutineExercise = RoutineExercise(
        blockId = 0,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        category = category,
        measureType = measureType,
        orderIndex = orderIndex,
        targetReps = reps.toIntOrNull(),
        targetWeightKg = weightKg.toDoubleOrNull(),
        targetDurationSeconds = durationSeconds.toIntOrNull(),
        targetDistanceMeters = distanceMeters.toIntOrNull(),
        restAfterSeconds = restAfterSeconds.toIntOrNull() ?: 30,
        notes = notes.takeIf { it.isNotBlank() }
    )
}
