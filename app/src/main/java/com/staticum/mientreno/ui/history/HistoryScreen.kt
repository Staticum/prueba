package com.staticum.mientreno.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.staticum.mientreno.data.WorkoutSession
import com.staticum.mientreno.ui.components.EmptyState
import com.staticum.mientreno.util.toFormattedDateTime
import com.staticum.mientreno.util.toFormattedDuration

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onAddQuickLog: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddQuickLog) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar sesión")
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("Todavía no tienes sesiones registradas.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenSession(session.id) },
                        onDelete = { viewModel.deleteSession(session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.routineName ?: "Sesión libre", style = MaterialTheme.typography.titleMedium)
                Text(session.startMillis.toFormattedDateTime(), style = MaterialTheme.typography.labelMedium)
                val durationSeconds = session.endMillis?.let { ((it - session.startMillis) / 1000).toInt() }
                durationSeconds?.takeIf { it > 0 }?.let {
                    Text("Duración: ${it.toFormattedDuration()}", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
