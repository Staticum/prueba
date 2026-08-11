package com.staticum.mientreno.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.FitnessRepository
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    repository: FitnessRepository,
    exerciseId: Long
) : ViewModel() {

    var exercise by mutableStateOf<Exercise?>(null)
        private set

    init {
        viewModelScope.launch {
            exercise = repository.getExercise(exerciseId)
        }
    }
}
