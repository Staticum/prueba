package com.staticum.mientreno.ui.quicklog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.data.SessionExerciseLog
import com.staticum.mientreno.data.WorkoutSession
import com.staticum.mientreno.ui.routines.DraftExerciseItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickLogState(
    val items: List<DraftExerciseItem> = emptyList(),
    val notes: String = "",
    val isSaved: Boolean = false
) {
    val isValid: Boolean get() = items.isNotEmpty()
}

class QuickLogViewModel(private val repository: FitnessRepository) : ViewModel() {

    var state by mutableStateOf(QuickLogState())
        private set

    val availableExercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExercise(exercise: Exercise) {
        state = state.copy(items = state.items + DraftExerciseItem.fromExercise(exercise))
    }

    fun createExerciseAndAdd(
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
            addExercise(newExercise.copy(id = newId))
        }
    }

    fun removeItem(index: Int) {
        state = state.copy(items = state.items.filterIndexed { i, _ -> i != index })
    }

    fun updateItem(index: Int, updated: DraftExerciseItem) {
        val mutable = state.items.toMutableList()
        mutable[index] = updated
        state = state.copy(items = mutable)
    }

    fun updateNotes(value: String) {
        state = state.copy(notes = value)
    }

    fun save() {
        if (!state.isValid) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val logs = mutableListOf<SessionExerciseLog>()
            state.items.forEachIndexed { index, item ->
                if (item.measureType == MeasureType.REPS) {
                    val setCount = item.sets.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    repeat(setCount) { setIndex ->
                        logs.add(
                            SessionExerciseLog(
                                sessionId = 0,
                                exerciseId = item.exerciseId,
                                exerciseName = item.exerciseName,
                                category = item.category,
                                measureType = item.measureType,
                                orderIndex = index,
                                roundNumber = setIndex + 1,
                                reps = item.reps.toIntOrNull(),
                                weightKg = item.weightKg.toDoubleOrNull(),
                                completed = true
                            )
                        )
                    }
                } else {
                    logs.add(
                        SessionExerciseLog(
                            sessionId = 0,
                            exerciseId = item.exerciseId,
                            exerciseName = item.exerciseName,
                            category = item.category,
                            measureType = item.measureType,
                            orderIndex = index,
                            roundNumber = 1,
                            durationSeconds = item.durationSeconds.toIntOrNull(),
                            distanceMeters = item.distanceMeters.toIntOrNull(),
                            completed = true
                        )
                    )
                }
            }
            val session = WorkoutSession(
                routineTemplateId = null,
                routineName = null,
                startMillis = now,
                endMillis = now,
                notes = state.notes.takeIf { it.isNotBlank() }
            )
            repository.saveSession(session, logs)
            state = state.copy(isSaved = true)
        }
    }
}
