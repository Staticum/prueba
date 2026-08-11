package com.staticum.mientreno.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.data.SessionExerciseLog
import com.staticum.mientreno.data.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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

    private val _speechEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val speechEvents: SharedFlow<String> = _speechEvents

    private val startMillis = System.currentTimeMillis()
    private val completedLogs = mutableListOf<SessionExerciseLog>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val routineWithBlocks = repository.observeRoutine(routineId).filterNotNull().first()
            val steps = buildExecutionSteps(routineWithBlocks.blocks)
            state = state.copy(
                routineName = routineWithBlocks.routine.name,
                steps = steps,
                currentIndex = 0,
                phase = if (steps.isEmpty()) ExecutionPhase.DONE else ExecutionPhase.EXERCISE
            )
            if (steps.isNotEmpty()) beginCurrentStep()
        }
    }

    private fun beginCurrentStep() {
        val step = state.currentStep ?: return
        announceStepStart(step)
        if (step.measureType == MeasureType.TIME) {
            val seconds = step.targetDurationSeconds?.coerceAtLeast(1) ?: 30
            startCountdown(
                seconds = seconds,
                onTick = { remaining -> state = state.copy(remainingSeconds = remaining) },
                onDone = {
                    completeCurrentStep(
                        actualReps = null,
                        actualWeightKg = null,
                        actualDurationSeconds = seconds,
                        actualDistanceMeters = step.targetDistanceMeters
                    )
                }
            )
        }
    }

    private fun announceStepStart(step: ExecutionStep) {
        val roundLabel = if (step.totalRounds > 1) {
            if (step.isCircuit) "Ronda ${step.roundNumber} de ${step.totalRounds}." else "Serie ${step.roundNumber} de ${step.totalRounds}."
        } else ""
        val detail = when (step.measureType) {
            MeasureType.REPS -> {
                val reps = step.targetReps?.let { "$it repeticiones" } ?: "hasta el fallo"
                val weight = step.targetWeightKg?.let { " con $it kilos" } ?: ""
                "$reps$weight."
            }
            MeasureType.TIME -> {
                val seconds = step.targetDurationSeconds ?: 30
                val distance = step.targetDistanceMeters?.let { " o $it metros" } ?: ""
                "$seconds segundos$distance."
            }
        }
        val sentence = listOf("Inicia: ${step.exerciseName}.", roundLabel, detail)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        _speechEvents.tryEmit(sentence)

        if (step.measureType == MeasureType.TIME) {
            state = state.copy(remainingSeconds = step.targetDurationSeconds ?: 30)
        }
    }

    fun completeCurrentStep(
        actualReps: Int?,
        actualWeightKg: Double?,
        actualDurationSeconds: Int?,
        actualDistanceMeters: Int?
    ) {
        val step = state.currentStep ?: return
        timerJob?.cancel()
        completedLogs.add(
            SessionExerciseLog(
                sessionId = 0,
                exerciseId = step.exerciseId,
                exerciseName = step.exerciseName,
                category = step.category,
                measureType = step.measureType,
                orderIndex = state.currentIndex,
                roundNumber = step.roundNumber,
                blockName = step.blockName,
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

    fun skipTimedExercise() {
        val step = state.currentStep ?: return
        if (step.measureType != MeasureType.TIME) return
        val elapsed = (step.targetDurationSeconds ?: 0) - state.remainingSeconds
        completeCurrentStep(null, null, elapsed.coerceAtLeast(0), step.targetDistanceMeters)
    }

    private fun startRest(seconds: Int) {
        state = state.copy(phase = ExecutionPhase.RESTING, remainingSeconds = seconds)
        _speechEvents.tryEmit("Descansa $seconds segundos.")
        startCountdown(
            seconds = seconds,
            onTick = { remaining -> state = state.copy(remainingSeconds = remaining) },
            onDone = { advanceToNextStep() }
        )
    }

    private fun startCountdown(seconds: Int, onTick: (Int) -> Unit, onDone: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                onTick(remaining)
                when {
                    remaining == 5 && seconds > 6 -> _speechEvents.tryEmit("Quedan 5 segundos.")
                    remaining in 1..3 -> _speechEvents.tryEmit(remaining.toString())
                }
            }
            onDone()
        }
    }

    fun skipRest() {
        timerJob?.cancel()
        advanceToNextStep()
    }

    private fun advanceToNextStep() {
        val nextIndex = state.currentIndex + 1
        if (nextIndex < state.steps.size) {
            state = state.copy(currentIndex = nextIndex, phase = ExecutionPhase.EXERCISE, remainingSeconds = 0)
            beginCurrentStep()
        } else {
            state = state.copy(phase = ExecutionPhase.DONE, remainingSeconds = 0)
            _speechEvents.tryEmit("Rutina completada. Buen trabajo.")
            saveSession()
        }
    }

    fun finishEarly() {
        timerJob?.cancel()
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
        timerJob?.cancel()
    }
}
