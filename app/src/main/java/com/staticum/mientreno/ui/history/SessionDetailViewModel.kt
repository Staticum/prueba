package com.staticum.mientreno.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.SessionWithLogs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionDetailViewModel(
    repository: FitnessRepository,
    sessionId: Long
) : ViewModel() {

    val session: StateFlow<SessionWithLogs?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
