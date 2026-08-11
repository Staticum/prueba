package com.staticum.mientreno.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.ui.components.EmptyState

@Composable
fun RoutineListScreen(
    viewModel: RoutineListViewModel,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    onExecuteRoutine: (Long) -> Unit
) {
    val routines by viewModel.routines.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva rutina")
            }
        }
    ) { padding ->
        if (routines.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("Todavía no tienes rutinas. Crea una con el botón +.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium)
                            routine.description?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { onExecuteRoutine(routine.id) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Ejecutar")
                                }
                                IconButton(onClick = { onEditRoutine(routine.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { viewModel.deleteRoutine(routine) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
