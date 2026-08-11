package com.staticum.mientreno.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.data.ExerciseCategory
import com.staticum.mientreno.data.MeasureType

@Composable
fun ExerciseFormDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: ExerciseCategory,
        measureType: MeasureType,
        equipment: String,
        instructions: String,
        restSeconds: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.CASA) }
    var measureType by remember { mutableStateOf(MeasureType.REPS) }
    var equipment by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var restSeconds by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExerciseCategory.entries) { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("¿Cómo se mide?")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MeasureType.entries) { option ->
                        FilterChip(
                            selected = measureType == option,
                            onClick = { measureType = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Indicación para la guía por voz (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text("Descanso por defecto (segundos)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            category,
                            measureType,
                            equipment,
                            instructions,
                            restSeconds.toIntOrNull() ?: 60
                        )
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
