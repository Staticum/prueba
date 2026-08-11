package com.staticum.mientreno.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staticum.mientreno.data.MeasureType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineExecutionScreen(
    viewModel: RoutineExecutionViewModel,
    onFinished: () -> Unit
) {
    val state = viewModel.state
    val coach by rememberWorkoutCoach()

    LaunchedEffect(viewModel) {
        viewModel.speechEvents.collect { text -> coach?.speak(text) }
    }

    LaunchedEffect(viewModel.isFinished) {
        if (viewModel.isFinished) onFinished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.routineName) },
                actions = {
                    IconButton(onClick = { viewModel.finishEarly() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Terminar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(state.progressLabel, style = MaterialTheme.typography.labelMedium)

            when (state.phase) {
                ExecutionPhase.LOADING -> Text("Cargando rutina…")
                ExecutionPhase.EXERCISE -> {
                    val step = state.currentStep
                    if (step != null) {
                        StepHeader(step)
                        if (step.measureType == MeasureType.TIME) {
                            TimedExerciseContent(
                                remainingSeconds = state.remainingSeconds,
                                onSkip = { viewModel.skipTimedExercise() }
                            )
                        } else {
                            RepsExerciseContent(step = step, onComplete = viewModel::completeCurrentStep)
                        }
                    }
                }
                ExecutionPhase.RESTING -> {
                    Text("Descanso", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${state.remainingSeconds}",
                        fontSize = 64.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedButton(onClick = { viewModel.skipRest() }) {
                        Text("Saltar descanso")
                    }
                }
                ExecutionPhase.DONE -> {
                    Text("¡Rutina completada!", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: ExecutionStep) {
    if (step.totalRounds > 1) {
        val label = if (step.isCircuit) "Ronda ${step.roundNumber} de ${step.totalRounds}" else "Serie ${step.roundNumber} de ${step.totalRounds}"
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
    Text(step.exerciseName, style = MaterialTheme.typography.titleLarge)
    step.notes?.let {
        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TimedExerciseContent(remainingSeconds: Int, onSkip: () -> Unit) {
    Text(
        "$remainingSeconds",
        fontSize = 64.sp,
        style = MaterialTheme.typography.titleLarge
    )
    OutlinedButton(onClick = onSkip) {
        Text("Marcar hecho antes")
    }
}

@Composable
private fun RepsExerciseContent(
    step: ExecutionStep,
    onComplete: (Int?, Double?, Int?, Int?) -> Unit
) {
    var reps by remember(step) { mutableStateOf(step.targetReps?.toString() ?: "") }
    var weight by remember(step) { mutableStateOf(step.targetWeightKg?.toString() ?: "") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter { c -> c.isDigit() } },
            label = { Text("Reps") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.45f)
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Kg") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }

    Button(
        onClick = { onComplete(reps.toIntOrNull(), weight.toDoubleOrNull(), null, null) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Serie hecha") }
}
