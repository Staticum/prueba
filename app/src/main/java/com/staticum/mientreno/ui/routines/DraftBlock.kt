package com.staticum.mientreno.ui.routines

import com.staticum.mientreno.data.BlockWithExercises
import com.staticum.mientreno.data.RoutineBlock
import com.staticum.mientreno.data.RoutineExercise

data class DraftBlock(
    val name: String = "",
    val rounds: String = "3",
    val restBetweenRoundsSeconds: String = "60",
    val items: List<DraftExerciseItem> = emptyList()
) {
    val isCircuit: Boolean get() = items.size > 1

    companion object {
        fun fromBlockWithExercises(blockWithExercises: BlockWithExercises): DraftBlock = DraftBlock(
            name = blockWithExercises.block.name ?: "",
            rounds = blockWithExercises.block.rounds.toString(),
            restBetweenRoundsSeconds = blockWithExercises.block.restBetweenRoundsSeconds.toString(),
            items = blockWithExercises.exercises
                .sortedBy { it.orderIndex }
                .map { DraftExerciseItem.fromRoutineExercise(it) }
        )
    }

    fun toBlockWithExercises(orderIndex: Int): Pair<RoutineBlock, List<RoutineExercise>> {
        val block = RoutineBlock(
            routineId = 0,
            orderIndex = orderIndex,
            name = name.trim().takeIf { it.isNotBlank() },
            rounds = rounds.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            restBetweenRoundsSeconds = restBetweenRoundsSeconds.toIntOrNull() ?: 60
        )
        val exercises = items.mapIndexed { index, item -> item.toRoutineExercise(index) }
        return block to exercises
    }
}
