package com.staticum.urodiario.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.urodiario.data.MicturitionRepository
import com.staticum.urodiario.data.RecordWithPhotos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: MicturitionRepository) : ViewModel() {

    val records: StateFlow<List<RecordWithPhotos>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRecord(item: RecordWithPhotos) {
        viewModelScope.launch {
            repository.deleteRecord(item.record)
        }
    }
}
