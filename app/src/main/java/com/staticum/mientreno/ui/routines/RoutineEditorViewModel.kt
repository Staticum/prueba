package com.staticum.mientreno.ui.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.data.RoutineTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineEditorViewModel(
    private val repository: FitnessRepository,
    private val routineId: Long?
) : ViewModel() {

    var state by mutableStateOf(RoutineEditorState(isEditing = routineId != null))
        private set

    val availableExercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val idToLoad = routineId
        if (idToLoad != null) {
            viewModelScope.launch {
                repository.observeRoutine(idToLoad).collect { routineWithBlocks ->
                    if (routineWithBlocks != null) {
                        state = state.copy(
                            id = routineWithBlocks.routine.id,
                            name = routineWithBlocks.routine.name,
                            description = routineWithBlocks.routine.description ?: "",
                            blocks = routineWithBlocks.blocks
                                .sortedBy { it.block.orderIndex }
                                .map { DraftBlock.fromBlockWithExercises(it) }
                        )
                    }
                }
            }
        }
    }

    fun updateName(value: String) {
        state = state.copy(name = value)
    }

    fun updateDescription(value: String) {
        state = state.copy(description = value)
    }

    fun addBlock() {
        state = state.copy(blocks = state.blocks + DraftBlock())
    }

    fun removeBlock(blockIndex: Int) {
        state = state.copy(blocks = state.blocks.filterIndexed { i, _ -> i != blockIndex })
    }

    fun updateBlock(blockIndex: Int, updated: DraftBlock) {
        val mutable = state.blocks.toMutableList()
        mutable[blockIndex] = updated
        state = state.copy(blocks = mutable)
    }

    fun addExerciseToBlock(blockIndex: Int, exercise: Exercise) {
        val block = state.blocks.getOrNull(blockIndex) ?: return
        updateBlock(blockIndex, block.copy(items = block.items + DraftExerciseItem.fromExercise(exercise)))
    }

    fun createExerciseAndAddToBlock(
        blockIndex: Int,
        name: String,
        category: ExerciseCategory,
        measureType: MeasureType,
        equipment: String,
        instructions: String,
        restSeconds: Int
    ) {
        viewModelScope.launch {
            val newExercise = Exercise(
                name = name,
                category = category,
                measureType = measureType,
                equipment = equipment.takeIf { it.isNotBlank() },
                instructions = instructions.takeIf { it.isNotBlank() },
                defaultRestSeconds = restSeconds,
                isCustom = true
            )
            val newId = repository.saveExercise(newExercise)
            addExerciseToBlock(blockIndex, newExercise.copy(id = newId))
        }
    }

    fun removeExerciseFromBlock(blockIndex: Int, exerciseIndex: Int) {
        val block = state.blocks.getOrNull(blockIndex) ?: return
        updateBlock(blockIndex, block.copy(items = block.items.filterIndexed { i, _ -> i != exerciseIndex }))
    }

    fun moveExerciseInBlock(blockIndex: Int, exerciseIndex: Int, delta: Int) {
        val block = state.blocks.getOrNull(blockIndex) ?: return
        val newIndex = exerciseIndex + delta
        if (newIndex < 0 || newIndex >= block.items.size) return
        val mutable = block.items.toMutableList()
        val moved = mutable.removeAt(exerciseIndex)
        mutable.add(newIndex, moved)
        updateBlock(blockIndex, block.copy(items = mutable))
    }

    fun updateExerciseInBlock(blockIndex: Int, exerciseIndex: Int, updated: DraftExerciseItem) {
        val block = state.blocks.getOrNull(blockIndex) ?: return
        val mutable = block.items.toMutableList()
        mutable[exerciseIndex] = updated
        updateBlock(blockIndex, block.copy(items = mutable))
    }

    fun save() {
        if (!state.isValid) return
        viewModelScope.launch {
            val routine = RoutineTemplate(
                id = state.id,
                name = state.name.trim(),
                description = state.description.trim().takeIf { it.isNotBlank() }
            )
            val blocks = state.blocks.mapIndexed { index, block -> block.toBlockWithExercises(index) }
            val savedId = repository.saveRoutine(routine, blocks)
            state = state.copy(id = savedId, isSaved = true)
        }
    }
}
