package com.staticum.mientreno.ui.workout

import com.staticum.mientreno.data.BlockWithExercises
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.MeasureType

data class ExecutionStep(
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val measureType: MeasureType,
    val blockName: String?,
    val isCircuit: Boolean,
    val roundNumber: Int,
    val totalRounds: Int,
    val targetReps: Int?,
    val targetWeightKg: Double?,
    val targetDurationSeconds: Int?,
    val targetDistanceMeters: Int?,
    val restSecondsAfter: Int,
    val notes: String?
)

fun buildExecutionSteps(blocks: List<BlockWithExercises>): List<ExecutionStep> {
    val steps = mutableListOf<ExecutionStep>()
    blocks.sortedBy { it.block.orderIndex }.forEach { blockWithExercises ->
        val block = blockWithExercises.block
        val exercises = blockWithExercises.exercises.sortedBy { it.orderIndex }
        if (exercises.isEmpty()) return@forEach
        val rounds = block.rounds.coerceAtLeast(1)
        val isCircuit = exercises.size > 1

        for (round in 1..rounds) {
            exercises.forEachIndexed { index, exercise ->
                val isLastInRound = index == exercises.lastIndex
                val restSeconds = if (isLastInRound) block.restBetweenRoundsSeconds else exercise.restAfterSeconds
                steps.add(
                    ExecutionStep(
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        category = exercise.category,
                        measureType = exercise.measureType,
                        blockName = block.name,
                        isCircuit = isCircuit,
                        roundNumber = round,
                        totalRounds = rounds,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg,
                        targetDurationSeconds = exercise.targetDurationSeconds,
                        targetDistanceMeters = exercise.targetDistanceMeters,
                        restSecondsAfter = restSeconds,
                        notes = exercise.notes
                    )
                )
            }
        }
    }
    return steps
}
