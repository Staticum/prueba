package com.staticum.mientreno.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.data.Exercise
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.MeasureType
import com.staticum.mientreno.ui.library.ExerciseFormDialog

@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    onCreateNew: (
        name: String,
        category: ExerciseCategory,
        measureType: MeasureType,
        equipment: String,
        instructions: String,
        restSeconds: Int
    ) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val filtered = remember(exercises, query) {
        exercises.filter { it.name.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir ejercicio") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("＋ Crear ejercicio nuevo") }

                LazyColumn(modifier = Modifier.height(320.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.id }) { exercise ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            TextButton(onClick = { onPick(exercise) }) {
                                Column {
                                    Text(exercise.name)
                                    Text(
                                        "${exercise.category.label} · ${exercise.measureType.label}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    if (showCreateDialog) {
        ExerciseFormDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, category, measureType, equipment, instructions, restSeconds ->
                onCreateNew(name, category, measureType, equipment, instructions, restSeconds)
                showCreateDialog = false
            }
        )
    }
}
