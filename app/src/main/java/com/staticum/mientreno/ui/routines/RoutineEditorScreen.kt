package com.staticum.mientreno.ui.routines

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.staticum.mientreno.data.ExerciseType
import com.staticum.mientreno.ui.components.NumberField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    viewModel: RoutineEditorViewModel,
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
                title = { Text(if (state.isEditing) "Editar rutina" else "Nueva rutina") },
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
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Ejercicios", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(state.items) { index, draftItem ->
                DraftExerciseCard(
                    item = draftItem,
                    onChange = { viewModel.updateItem(index, it) },
                    onMoveUp = { viewModel.moveExercise(index, -1) },
                    onMoveDown = { viewModel.moveExercise(index, 1) },
                    onRemove = { viewModel.removeExercise(index) }
                )
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar ejercicio")
                }
            }
            item {
                Button(
                    onClick = viewModel::save,
                    enabled = state.isValid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar rutina") }
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
            }
        )
    }
}

@Composable
private fun DraftExerciseCard(
    item: DraftExerciseItem,
    onChange: (DraftExerciseItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Quitar")
                    }
                }
            }

            if (item.type == ExerciseType.FUERZA) {
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

            NumberField(
                label = "Descanso después (s)",
                value = item.restSeconds,
                onChange = { onChange(item.copy(restSeconds = it)) },
                modifier = Modifier.width(160.dp).padding(top = 8.dp)
            )

            OutlinedTextField(
                value = item.notes,
                onValueChange = { onChange(item.copy(notes = it)) },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
