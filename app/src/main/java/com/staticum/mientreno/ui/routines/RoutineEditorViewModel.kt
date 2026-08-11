package com.staticum.mientreno.ui.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.FitnessRepository
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
                repository.observeRoutine(idToLoad).collect { routineWithExercises ->
                    if (routineWithExercises != null) {
                        state = state.copy(
                            id = routineWithExercises.routine.id,
                            name = routineWithExercises.routine.name,
                            description = routineWithExercises.routine.description ?: "",
                            items = routineWithExercises.exercises
                                .sortedBy { it.orderIndex }
                                .map { DraftExerciseItem.fromRoutineExercise(it) }
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

    fun addExercise(exercise: Exercise) {
        state = state.copy(items = state.items + DraftExerciseItem.fromExercise(exercise))
    }

    fun removeExercise(index: Int) {
        state = state.copy(items = state.items.filterIndexed { i, _ -> i != index })
    }

    fun moveExercise(index: Int, delta: Int) {
        val newIndex = index + delta
        if (newIndex < 0 || newIndex >= state.items.size) return
        val mutable = state.items.toMutableList()
        val moved = mutable.removeAt(index)
        mutable.add(newIndex, moved)
        state = state.copy(items = mutable)
    }

    fun updateItem(index: Int, updated: DraftExerciseItem) {
        val mutable = state.items.toMutableList()
        mutable[index] = updated
        state = state.copy(items = mutable)
    }

    fun save() {
        if (!state.isValid) return
        viewModelScope.launch {
            val routine = RoutineTemplate(
                id = state.id,
                name = state.name.trim(),
                description = state.description.trim().takeIf { it.isNotBlank() }
            )
            val routineExercises = state.items.mapIndexed { index, item ->
                item.toRoutineExercise(routineId = state.id, orderIndex = index)
            }
            val savedId = repository.saveRoutine(routine, routineExercises)
            state = state.copy(id = savedId, isSaved = true)
        }
    }
}
