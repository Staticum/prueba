package com.staticum.mientreno.ui.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.staticum.mientreno.MainActivity
import com.staticum.mientreno.MiEntrenoApplication
import com.staticum.mientreno.R
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.data.SessionExerciseLog
import com.staticum.mientreno.data.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class WorkoutTimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): WorkoutTimerService = this@WorkoutTimerService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: FitnessRepository
    private var coach: WorkoutCoach? = null

    private val _state = MutableStateFlow(RoutineExecutionState())
    val state: StateFlow<RoutineExecutionState> = _state

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _savedSessionId = MutableStateFlow<Long?>(null)
    val savedSessionId: StateFlow<Long?> = _savedSessionId

    private var activeRoutineId: Long? = null
    private var startMillis: Long = 0L
    private val completedLogs = mutableListOf<SessionExerciseLog>()
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = (application as MiEntrenoApplication).repository
        createNotificationChannel()
        var engine: TextToSpeech? = null
        engine = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("es", "ES")
                coach = engine?.let { WorkoutCoach(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Preparando rutina…", ""),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        val routineId = intent?.getLongExtra(EXTRA_ROUTINE_ID, -1L) ?: -1L
        if (routineId >= 0 && routineId != activeRoutineId) {
            activeRoutineId = routineId
            beginRoutine(routineId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        coach?.shutdown()
    }

    private fun beginRoutine(routineId: Long) {
        startMillis = System.currentTimeMillis()
        completedLogs.clear()
        serviceScope.launch {
            val routineWithBlocks = repository.observeRoutine(routineId).filterNotNull().first()
            val steps = buildExecutionSteps(routineWithBlocks.blocks)
            _state.value = _state.value.copy(
                routineName = routineWithBlocks.routine.name,
                steps = steps,
                currentIndex = 0,
                phase = if (steps.isEmpty()) ExecutionPhase.DONE else ExecutionPhase.EXERCISE
            )
            if (steps.isNotEmpty()) {
                beginCurrentStep()
            } else {
                saveSessionAndStop(routineId)
            }
        }
    }

    private fun beginCurrentStep() {
        val step = _state.value.currentStep ?: return
        announceStepStart(step)
        if (step.measureType == MeasureType.TIME) {
            val seconds = step.targetDurationSeconds?.coerceAtLeast(1) ?: 30
            startCountdown(
                seconds = seconds,
                onTick = { remaining -> _state.value = _state.value.copy(remainingSeconds = remaining) },
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
        val spokenNote = step.notes?.replace("·", ",")?.let { "$it." } ?: ""
        val sentence = listOf("Inicia: ${step.exerciseName}.", spokenNote, roundLabel, detail)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        speak(sentence)

        if (step.measureType == MeasureType.TIME) {
            _state.value = _state.value.copy(remainingSeconds = step.targetDurationSeconds ?: 30)
        }
        updateNotification(exerciseTitle = step.exerciseName, body = step.notes ?: "")
    }

    fun completeCurrentStep(
        actualReps: Int?,
        actualWeightKg: Double?,
        actualDurationSeconds: Int?,
        actualDistanceMeters: Int?
    ) {
        val step = _state.value.currentStep ?: return
        timerJob?.cancel()
        completedLogs.add(
            SessionExerciseLog(
                sessionId = 0,
                exerciseId = step.exerciseId,
                exerciseName = step.exerciseName,
                category = step.category,
                measureType = step.measureType,
                orderIndex = _state.value.currentIndex,
                roundNumber = step.roundNumber,
                blockName = step.blockName,
                reps = actualReps ?: step.targetReps,
                weightKg = actualWeightKg ?: step.targetWeightKg,
                durationSeconds = actualDurationSeconds ?: step.targetDurationSeconds,
                distanceMeters = actualDistanceMeters ?: step.targetDistanceMeters,
                completed = true
            )
        )

        val isLastStep = _state.value.currentIndex >= _state.value.steps.size - 1
        if (step.restSecondsAfter > 0 && !isLastStep) {
            startRest(step.restSecondsAfter)
        } else {
            advanceToNextStep()
        }
    }

    fun skipTimedExercise() {
        val step = _state.value.currentStep ?: return
        if (step.measureType != MeasureType.TIME) return
        val elapsed = (step.targetDurationSeconds ?: 0) - _state.value.remainingSeconds
        completeCurrentStep(null, null, elapsed.coerceAtLeast(0), step.targetDistanceMeters)
    }

    private fun startRest(seconds: Int) {
        _state.value = _state.value.copy(phase = ExecutionPhase.RESTING, remainingSeconds = seconds)
        speak("Descansa $seconds segundos.")
        updateNotification(exerciseTitle = "Descanso", body = "$seconds segundos")
        startCountdown(
            seconds = seconds,
            onTick = { remaining -> _state.value = _state.value.copy(remainingSeconds = remaining) },
            onDone = { advanceToNextStep() }
        )
    }

    private fun startCountdown(seconds: Int, onTick: (Int) -> Unit, onDone: () -> Unit) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                onTick(remaining)
                if (remaining % 5 == 0 || remaining <= 3) {
                    updateNotificationRemaining(remaining)
                }
                when {
                    remaining == 5 && seconds > 6 -> speak("Quedan 5 segundos.")
                    remaining in 1..3 -> speak(remaining.toString())
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
        val nextIndex = _state.value.currentIndex + 1
        if (nextIndex < _state.value.steps.size) {
            _state.value = _state.value.copy(currentIndex = nextIndex, phase = ExecutionPhase.EXERCISE, remainingSeconds = 0)
            beginCurrentStep()
        } else {
            _state.value = _state.value.copy(phase = ExecutionPhase.DONE, remainingSeconds = 0)
            speak("Rutina completada. Buen trabajo.")
            val routineId = activeRoutineId ?: return
            saveSessionAndStop(routineId)
        }
    }

    fun finishEarly() {
        timerJob?.cancel()
        val routineId = activeRoutineId ?: return
        saveSessionAndStop(routineId)
    }

    private fun saveSessionAndStop(routineId: Long) {
        serviceScope.launch {
            val session = WorkoutSession(
                routineTemplateId = routineId,
                routineName = _state.value.routineName,
                startMillis = startMillis,
                endMillis = System.currentTimeMillis()
            )
            _savedSessionId.value = repository.saveSession(session, completedLogs)
            _isFinished.value = true
            ServiceCompat.stopForeground(this@WorkoutTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun speak(text: String) {
        coach?.speak(text)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Entrenamiento en curso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progreso de la rutina guiada"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, body: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun updateNotification(exerciseTitle: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(exerciseTitle, body))
    }

    private fun updateNotificationRemaining(remainingSeconds: Int) {
        val step = _state.value.currentStep
        val title = if (_state.value.phase == ExecutionPhase.RESTING) "Descanso" else step?.exerciseName ?: ""
        updateNotification(title, "Quedan ${remainingSeconds}s")
    }

    companion object {
        const val EXTRA_ROUTINE_ID = "routine_id"
        private const val CHANNEL_ID = "workout_timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
