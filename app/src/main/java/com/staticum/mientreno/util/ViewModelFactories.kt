package com.staticum.mientreno.util

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.ui.history.HistoryViewModel
import com.staticum.mientreno.ui.history.SessionDetailViewModel
import com.staticum.mientreno.ui.library.ExerciseLibraryViewModel
import com.staticum.mientreno.ui.progress.ProgressViewModel
import com.staticum.mientreno.ui.quicklog.QuickLogViewModel
import com.staticum.mientreno.ui.routines.RoutineEditorViewModel
import com.staticum.mientreno.ui.routines.RoutineListViewModel
import com.staticum.mientreno.ui.settings.SettingsViewModel
import com.staticum.mientreno.ui.workout.RoutineExecutionViewModel

fun libraryViewModelFactory(repository: FitnessRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { ExerciseLibraryViewModel(repository) }
    }

fun routineListViewModelFactory(repository: FitnessRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { RoutineListViewModel(repository) }
    }

fun routineEditorViewModelFactory(repository: FitnessRepository, routineId: Long?): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { RoutineEditorViewModel(repository, routineId) }
    }

fun routineExecutionViewModelFactory(repository: FitnessRepository, routineId: Long): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { RoutineExecutionViewModel(repository, routineId) }
    }

fun quickLogViewModelFactory(repository: FitnessRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { QuickLogViewModel(repository) }
    }

fun historyViewModelFactory(repository: FitnessRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { HistoryViewModel(repository) }
    }

fun sessionDetailViewModelFactory(repository: FitnessRepository, sessionId: Long): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { SessionDetailViewModel(repository, sessionId) }
    }

fun progressViewModelFactory(repository: FitnessRepository): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { ProgressViewModel(repository) }
    }

fun settingsViewModelFactory(appContext: Context): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { SettingsViewModel(appContext) }
    }
