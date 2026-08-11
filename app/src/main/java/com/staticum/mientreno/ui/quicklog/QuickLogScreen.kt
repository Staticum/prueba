package com.staticum.mientreno.ui.quicklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.ui.components.NumberField
import com.staticum.mientreno.ui.routines.DraftExerciseItem
import com.staticum.mientreno.ui.routines.ExercisePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogScreen(
    viewModel: QuickLogViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val state = viewModel.state
    val availableExercises by viewModel.availableExercises.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar sesión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(state.items) { index, item ->
                QuickLogItemCard(
                    item = item,
                    onChange = { viewModel.updateItem(index, it) },
                    onRemove = { viewModel.removeItem(index) }
                )
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar ejercicio realizado")
                }
            }
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notas de la sesión (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = viewModel::save,
                    enabled = state.isValid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar sesión") }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = availableExercises,
            onDismiss = { showPicker = false },
            onPick = {
                viewModel.addExercise(it)
                showPicker = false
            },
            onCreateNew = { name, category, measureType, equipment, instructions, restSeconds ->
                viewModel.createExerciseAndAdd(name, category, measureType, equipment, instructions, restSeconds)
                showPicker = false
            }
        )
    }
}

@Composable
private fun QuickLogItemCard(
    item: DraftExerciseItem,
    onChange: (DraftExerciseItem) -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar")
                }
            }

            if (item.measureType == MeasureType.REPS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Series",
                        value = item.sets,
                        onChange = { onChange(item.copy(sets = it)) },
                        modifier = Modifier.width(90.dp)
                    )
                    NumberField(
                        label = "Reps",
                        value = item.reps,
                        onChange = { onChange(item.copy(reps = it)) },
                        modifier = Modifier.width(90.dp)
                    )
                    NumberField(
                        label = "Peso (kg)",
                        value = item.weightKg,
                        onChange = { onChange(item.copy(weightKg = it)) },
                        modifier = Modifier.width(110.dp),
                        allowDecimal = true
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Duración (s)",
                        value = item.durationSeconds,
                        onChange = { onChange(item.copy(durationSeconds = it)) },
                        modifier = Modifier.width(130.dp)
                    )
                    NumberField(
                        label = "Distancia (m)",
                        value = item.distanceMeters,
                        onChange = { onChange(item.copy(distanceMeters = it)) },
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }
    }
}
