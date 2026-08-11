package com.staticum.mientreno.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.MeasureType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseLibraryViewModel(private val repository: FitnessRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<ExerciseCategory?>(null)

    val filterState: StateFlow<Pair<String, ExerciseCategory?>> =
        combine(query, categoryFilter) { q, c -> q to c }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "" to null)

    val exercises: StateFlow<List<Exercise>> = combine(
        repository.observeExercises(),
        query,
        categoryFilter
    ) { all, q, category ->
        all.filter { exercise ->
            (category == null || exercise.category == category) &&
                (q.isBlank() || exercise.name.contains(q, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setCategory(value: ExerciseCategory?) {
        categoryFilter.value = value
    }

    fun addCustomExercise(
        name: String,
        category: ExerciseCategory,
        measureType: MeasureType,
        equipment: String?,
        instructions: String?,
        defaultRestSeconds: Int
    ) {
        viewModelScope.launch {
            repository.saveExercise(
                Exercise(
                    name = name,
                    category = category,
                    measureType = measureType,
                    equipment = equipment?.takeIf { it.isNotBlank() },
                    instructions = instructions?.takeIf { it.isNotBlank() },
                    defaultRestSeconds = defaultRestSeconds,
                    isCustom = true
                )
            )
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }
}
