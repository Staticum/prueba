package com.staticum.mientreno.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.RoutineTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineListViewModel(private val repository: FitnessRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineTemplate>> = repository.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRoutine(routine: RoutineTemplate) {
        viewModelScope.launch { repository.deleteRoutine(routine) }
    }
}
