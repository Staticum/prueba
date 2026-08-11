package com.staticum.mientreno.ui.workout

import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.ExerciseType
import com.staticum.mientreno.data.RoutineExercise

data class ExecutionStep(
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val setNumber: Int,
    val totalSets: Int,
    val targetReps: Int?,
    val targetWeightKg: Double?,
    val targetDurationSeconds: Int?,
    val targetDistanceMeters: Int?,
    val restSecondsAfter: Int,
    val notes: String?
)

fun buildExecutionSteps(routineExercises: List<RoutineExercise>): List<ExecutionStep> {
    val steps = mutableListOf<ExecutionStep>()
    routineExercises.sortedBy { it.orderIndex }.forEach { exercise ->
        val setCount = if (exercise.type == ExerciseType.FUERZA) {
            (exercise.targetSets ?: 1).coerceAtLeast(1)
        } else {
            1
        }
        for (setNumber in 1..setCount) {
            steps.add(
                ExecutionStep(
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.exerciseName,
                    category = exercise.category,
                    type = exercise.type,
                    setNumber = setNumber,
                    totalSets = setCount,
                    targetReps = exercise.targetReps,
                    targetWeightKg = exercise.targetWeightKg,
                    targetDurationSeconds = exercise.targetDurationSeconds,
                    targetDistanceMeters = exercise.targetDistanceMeters,
                    restSecondsAfter = exercise.restSeconds,
                    notes = exercise.notes
                )
            )
        }
    }
    return steps
}
