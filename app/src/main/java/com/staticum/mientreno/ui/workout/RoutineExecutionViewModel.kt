package com.staticum.mientreno.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.ExerciseType
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.SessionExerciseLog
import com.staticum.mientreno.data.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class RoutineExecutionViewModel(
    private val repository: FitnessRepository,
    private val routineId: Long
) : ViewModel() {

    var state by mutableStateOf(RoutineExecutionState())
        private set

    var isFinished by mutableStateOf(false)
        private set
    var savedSessionId by mutableStateOf<Long?>(null)
        private set

    private val _speechEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val speechEvents: SharedFlow<String> = _speechEvents

    private val startMillis = System.currentTimeMillis()
    private val completedLogs = mutableListOf<SessionExerciseLog>()
    private var restJob: Job? = null

    init {
        viewModelScope.launch {
            val routineWithExercises = repository.observeRoutine(routineId).filterNotNull().first()
            val steps = buildExecutionSteps(routineWithExercises.exercises)
            state = state.copy(
                routineName = routineWithExercises.routine.name,
                steps = steps,
                currentIndex = 0,
                phase = if (steps.isEmpty()) ExecutionPhase.DONE else ExecutionPhase.EXERCISE
            )
            announceCurrentStep()
        }
    }

    private fun announceCurrentStep() {
        val step = state.currentStep ?: return
        val target = when (step.type) {
            ExerciseType.FUERZA -> {
                val reps = step.targetReps?.let { "$it repeticiones" } ?: "hasta el fallo"
                val weight = step.targetWeightKg?.let { " con ${it} kilos" } ?: ""
                "Serie ${step.setNumber} de ${step.totalSets}: $reps$weight."
            }
            ExerciseType.CARDIO -> {
                val duration = step.targetDurationSeconds?.let { "${it / 60} minutos" } ?: ""
                val distance = step.targetDistanceMeters?.let { " o ${it} metros" } ?: ""
                "$duration$distance".ifBlank { "Comienza cuando estés listo." }
            }
        }
        _speechEvents.tryEmit("${step.exerciseName}. $target")
    }

    fun completeCurrentStep(
        actualReps: Int?,
        actualWeightKg: Double?,
        actualDurationSeconds: Int?,
        actualDistanceMeters: Int?
    ) {
        val step = state.currentStep ?: return
        completedLogs.add(
            SessionExerciseLog(
                sessionId = 0,
                exerciseId = step.exerciseId,
                exerciseName = step.exerciseName,
                category = step.category,
                type = step.type,
                orderIndex = state.currentIndex,
                setNumber = step.setNumber,
                reps = actualReps ?: step.targetReps,
                weightKg = actualWeightKg ?: step.targetWeightKg,
                durationSeconds = actualDurationSeconds ?: step.targetDurationSeconds,
                distanceMeters = actualDistanceMeters ?: step.targetDistanceMeters,
                completed = true
            )
        )

        val isLastStep = state.currentIndex >= state.steps.size - 1
        if (step.restSecondsAfter > 0 && !isLastStep) {
            startRest(step.restSecondsAfter)
        } else {
            advanceToNextStep()
        }
    }

    private fun startRest(seconds: Int) {
        state = state.copy(phase = ExecutionPhase.RESTING, remainingRestSeconds = seconds)
        _speechEvents.tryEmit("Descansa $seconds segundos.")
        restJob?.cancel()
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                state = state.copy(remainingRestSeconds = remaining)
                if (remaining in 1..3) {
                    _speechEvents.tryEmit(remaining.toString())
                }
            }
            advanceToNextStep()
        }
    }

    fun skipRest() {
        restJob?.cancel()
        advanceToNextStep()
    }

    private fun advanceToNextStep() {
        val nextIndex = state.currentIndex + 1
        if (nextIndex < state.steps.size) {
            state = state.copy(currentIndex = nextIndex, phase = ExecutionPhase.EXERCISE, remainingRestSeconds = 0)
            announceCurrentStep()
        } else {
            state = state.copy(phase = ExecutionPhase.DONE, remainingRestSeconds = 0)
            _speechEvents.tryEmit("Rutina completada. Buen trabajo.")
            saveSession()
        }
    }

    fun finishEarly() {
        restJob?.cancel()
        saveSession()
    }

    private fun saveSession() {
        viewModelScope.launch {
            val session = WorkoutSession(
                routineTemplateId = routineId,
                routineName = state.routineName,
                startMillis = startMillis,
                endMillis = System.currentTimeMillis()
            )
            savedSessionId = repository.saveSession(session, completedLogs)
            isFinished = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        restJob?.cancel()
    }
}
