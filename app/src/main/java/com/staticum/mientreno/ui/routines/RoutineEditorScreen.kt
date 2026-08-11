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
import androidx.compose.material3.HorizontalDivider
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    viewModel: RoutineEditorViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val state = viewModel.state
    val availableExercises by viewModel.availableExercises.collectAsState()
    var pickerForBlock by remember { mutableStateOf<Int?>(null) }

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
                Text("Bloques", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Un bloque con 1 ejercicio = series normales. Con varios ejercicios = un circuito que se repite por rondas.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(state.blocks) { blockIndex, block ->
                DraftBlockCard(
                    block = block,
                    onChangeBlock = { viewModel.updateBlock(blockIndex, it) },
                    onRemoveBlock = { viewModel.removeBlock(blockIndex) },
                    onAddExercise = { pickerForBlock = blockIndex },
                    onRemoveExercise = { exIndex -> viewModel.removeExerciseFromBlock(blockIndex, exIndex) },
                    onMoveExercise = { exIndex, delta -> viewModel.moveExerciseInBlock(blockIndex, exIndex, delta) },
                    onChangeExercise = { exIndex, item -> viewModel.updateExerciseInBlock(blockIndex, exIndex, item) }
                )
            }
            item {
                OutlinedButton(onClick = { viewModel.addBlock() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar bloque")
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

    val activeBlockIndex = pickerForBlock
    if (activeBlockIndex != null) {
        ExercisePickerDialog(
            exercises = availableExercises,
            onDismiss = { pickerForBlock = null },
            onPick = {
                viewModel.addExerciseToBlock(activeBlockIndex, it)
                pickerForBlock = null
            },
            onCreateNew = { name, category, measureType, equipment, instructions, restSeconds ->
                viewModel.createExerciseAndAddToBlock(
                    activeBlockIndex, name, category, measureType, equipment, instructions, restSeconds
                )
                pickerForBlock = null
            }
        )
    }
}

@Composable
private fun DraftBlockCard(
    block: DraftBlock,
    onChangeBlock: (DraftBlock) -> Unit,
    onRemoveBlock: () -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    onChangeExercise: (Int, DraftExerciseItem) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (block.isCircuit) "Circuito" else "Bloque",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onRemoveBlock) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar bloque")
                }
            }

            OutlinedTextField(
                value = block.name,
                onValueChange = { onChangeBlock(block.copy(name = it)) },
                label = { Text("Nombre del bloque (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                NumberField(
                    label = "Rondas",
                    value = block.rounds,
                    onChange = { onChangeBlock(block.copy(rounds = it)) },
                    modifier = Modifier.width(110.dp)
                )
                NumberField(
                    label = "Descanso entre rondas (s)",
                    value = block.restBetweenRoundsSeconds,
                    onChange = { onChangeBlock(block.copy(restBetweenRoundsSeconds = it)) },
                    modifier = Modifier.width(180.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            block.items.forEachIndexed { exIndex, item ->
                DraftExerciseRow(
                    item = item,
                    showRestField = exIndex < block.items.lastIndex,
                    onChange = { onChangeExercise(exIndex, it) },
                    onMoveUp = { onMoveExercise(exIndex, -1) },
                    onMoveDown = { onMoveExercise(exIndex, 1) },
                    onRemove = { onRemoveExercise(exIndex) }
                )
                if (exIndex < block.items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Agregar ejercicio al bloque")
            }
        }
    }
}

@Composable
private fun DraftExerciseRow(
    item: DraftExerciseItem,
    showRestField: Boolean,
    onChange: (DraftExerciseItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.exerciseName, style = MaterialTheme.typography.bodyLarge)
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

        if (item.measureType == MeasureType.REPS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = "Distancia (m, opcional)",
                    value = item.distanceMeters,
                    onChange = { onChange(item.copy(distanceMeters = it)) },
                    modifier = Modifier.width(160.dp)
                )
            }
        }

        if (showRestField) {
            NumberField(
                label = "Descanso antes del siguiente (s)",
                value = item.restAfterSeconds,
                onChange = { onChange(item.copy(restAfterSeconds = it)) },
                modifier = Modifier.width(200.dp).padding(top = 8.dp)
            )
        }

        OutlinedTextField(
            value = item.notes,
            onValueChange = { onChange(item.copy(notes = it)) },
            label = { Text("Nota (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}
