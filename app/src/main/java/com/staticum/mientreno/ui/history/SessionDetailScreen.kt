package com.staticum.mientreno.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.data.ExerciseType
import com.staticum.mientreno.data.SessionExerciseLog
import com.staticum.mientreno.ui.components.EmptyState
import com.staticum.mientreno.util.toFormattedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(viewModel: SessionDetailViewModel, onBack: () -> Unit) {
    val sessionWithLogs by viewModel.session.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sessionWithLogs?.session?.routineName ?: "Sesión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        val data = sessionWithLogs
        if (data == null) {
            EmptyState("Cargando…", modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        val groupedLogs = data.logs.sortedBy { it.orderIndex }.groupBy { it.exerciseName }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(data.session.startMillis.toFormattedDateTime(), style = MaterialTheme.typography.bodyLarge)
                data.session.notes?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
            items(groupedLogs.entries.toList()) { (exerciseName, logs) ->
                ExerciseLogCard(exerciseName = exerciseName, logs = logs)
            }
        }
    }
}

@Composable
private fun ExerciseLogCard(exerciseName: String, logs: List<SessionExerciseLog>) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(exerciseName, style = MaterialTheme.typography.titleMedium)
            logs.sortedBy { it.setNumber }.forEach { log ->
                val line = if (log.type == ExerciseType.FUERZA) {
                    val reps = log.reps?.let { "$it reps" } ?: ""
                    val weight = log.weightKg?.let { " · ${it} kg" } ?: ""
                    "Serie ${log.setNumber}: $reps$weight"
                } else {
                    val duration = log.durationSeconds?.let { "${it / 60} min" } ?: ""
                    val distance = log.distanceMeters?.let { " · ${it} m" } ?: ""
                    "$duration$distance"
                }
                Text(line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
