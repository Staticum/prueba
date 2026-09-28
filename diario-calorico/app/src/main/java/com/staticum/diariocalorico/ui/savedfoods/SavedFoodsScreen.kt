package com.staticum.diariocalorico.ui.savedfoods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.diariocalorico.data.SavedFood

@Composable
fun SavedFoodsScreen(viewModel: SavedFoodsViewModel, onBack: () -> Unit) {
    val foods by viewModel.foods.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alimentos guardados") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Agregar alimento") }
        }
    ) { padding ->
        if (foods.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Todavía no tienes alimentos guardados. Agrégalos manualmente aquí, o desde el resultado de un análisis con el botón \"Guardar como favorito\".",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(foods) { food ->
                    SavedFoodRow(food, onDelete = { viewModel.delete(food) })
                }
            }
        }
    }

    if (showAddDialog) {
        SavedFoodEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cal, prot, carbs, fat ->
                viewModel.add(name, cal, prot, carbs, fat)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SavedFoodRow(food: SavedFood, onDelete: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${food.calories} kcal · P:${food.proteinGrams}g C:${food.carbsGrams}g G:${food.fatGrams}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}

@Composable
fun SavedFoodEditDialog(
    initialName: String = "",
    initialCalories: String = "",
    initialProtein: String = "",
    initialCarbs: String = "",
    initialFat: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Int, protein: Double, carbs: Double, fat: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf(initialCalories) }
    var protein by remember { mutableStateOf(initialProtein) }
    var carbs by remember { mutableStateOf(initialCarbs) }
    var fat by remember { mutableStateOf(initialFat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alimento guardado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calorías (kcal)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Proteína (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbohidratos (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Grasa (g)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        name,
                        calories.toIntOrNull() ?: 0,
                        protein.toDoubleOrNull() ?: 0.0,
                        carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0
                    )
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
