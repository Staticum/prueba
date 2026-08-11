package com.staticum.mientreno.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: FitnessRepository) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch { repository.deleteSession(session) }
    }
}
