package com.staticum.mientreno.ui.workout

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RoutineExecutionViewModel(
    private val appContext: Context,
    private val routineId: Long
) : ViewModel() {

    var state by mutableStateOf(RoutineExecutionState())
        private set

    var isFinished by mutableStateOf(false)
        private set
    var savedSessionId by mutableStateOf<Long?>(null)
        private set

    private var service: WorkoutTimerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bound = (binder as? WorkoutTimerService.LocalBinder)?.getService() ?: return
            service = bound
            viewModelScope.launch { bound.state.collect { state = it } }
            viewModelScope.launch { bound.isFinished.collect { isFinished = it } }
            viewModelScope.launch { bound.savedSessionId.collect { savedSessionId = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    init {
        val intent = Intent(appContext, WorkoutTimerService::class.java)
            .putExtra(WorkoutTimerService.EXTRA_ROUTINE_ID, routineId)
        ContextCompat.startForegroundService(appContext, intent)
        isBound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun completeCurrentStep(
        actualReps: Int?,
        actualWeightKg: Double?,
        actualDurationSeconds: Int?,
        actualDistanceMeters: Int?
    ) {
        service?.completeCurrentStep(actualReps, actualWeightKg, actualDurationSeconds, actualDistanceMeters)
    }

    fun skipTimedExercise() {
        service?.skipTimedExercise()
    }

    fun skipRest() {
        service?.skipRest()
    }

    fun finishEarly() {
        service?.finishEarly()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            appContext.unbindService(connection)
            isBound = false
        }
        service = null
    }
}
