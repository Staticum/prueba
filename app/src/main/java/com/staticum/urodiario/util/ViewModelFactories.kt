package com.staticum.urodiario.util

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.staticum.urodiario.data.MicturitionRepository
import com.staticum.urodiario.ui.form.RecordFormViewModel
import com.staticum.urodiario.ui.history.HistoryViewModel
import com.staticum.urodiario.ui.reports.ReportsViewModel

fun historyViewModelFactory(repository: MicturitionRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { HistoryViewModel(repository) }
    }

fun recordFormViewModelFactory(repository: MicturitionRepository, recordId: Long?): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { RecordFormViewModel(repository, recordId) }
    }

fun reportsViewModelFactory(repository: MicturitionRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { ReportsViewModel(repository) }
    }
