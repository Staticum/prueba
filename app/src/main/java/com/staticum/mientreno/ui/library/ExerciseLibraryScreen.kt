package com.staticum.mientreno.ui.library

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
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.ui.components.CategoryFilterRow
import com.staticum.mientreno.ui.components.EmptyState

@Composable
fun ExerciseLibraryScreen(viewModel: ExerciseLibraryViewModel) {
    val exercises by viewModel.exercises.collectAsState()
    val (query, category) = viewModel.filterState.collectAsState().value
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar ejercicio")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                label = { Text("Buscar ejercicio") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            CategoryFilterRow(selected = category, onSelect = viewModel::setCategory)

            if (exercises.isEmpty()) {
                EmptyState("No hay ejercicios para este filtro todavía.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 80.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        Card {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${exercise.category.label} · ${exercise.measureType.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        exercise.equipment?.let {
                                            Text("Equipo: $it", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                    if (exercise.isCustom) {
                                        IconButton(onClick = { viewModel.deleteExercise(exercise) }) {
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
    }

    if (showAddDialog) {
        ExerciseFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, type, equipment, instructions, rest ->
                viewModel.addCustomExercise(name, cat, type, equipment, instructions, rest)
                showAddDialog = false
            }
        )
    }
}
