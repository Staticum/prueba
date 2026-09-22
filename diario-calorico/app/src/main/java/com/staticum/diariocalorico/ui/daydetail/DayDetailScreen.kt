package com.staticum.diariocalorico.ui.daydetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File

@Composable
fun DayDetailScreen(
    viewModel: DayDetailViewModel,
    onBack: () -> Unit,
    onAddMeal: () -> Unit,
    onEditMeal: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showExpenditureDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateTimeFormatters.formatLongDate(state.date)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal) { Icon(Icons.Filled.Add, contentDescription = "Agregar comida") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${state.consumedCalories} / ${state.goals.calories} kcal", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Prot: ${state.consumedProtein.toInt()}/${state.goals.proteinGrams}g · Carb: ${state.consumedCarbs.toInt()}/${state.goals.carbsGrams}g · Grasa: ${state.consumedFat.toInt()}/${state.goals.fatGrams}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showExpenditureDialog = true }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Gasto calórico (reloj Polar)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                state.expenditure?.let { "$it kcal" } ?: "Sin registrar · toca para ingresar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.analyze() },
                    enabled = state.analysisState !is DayAnalysisState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Analizar este día con Gemini") }
            }

            item {
                when (val analysis = state.analysisState) {
                    is DayAnalysisState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is DayAnalysisState.Failed -> Text("Error: ${analysis.message}", color = MaterialTheme.colorScheme.error)
                    is DayAnalysisState.Done -> Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(analysis.advice, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {}
                }
            }

            item { Text("Comidas de este día", style = MaterialTheme.typography.titleMedium) }
            if (state.meals.isEmpty()) {
                item { Text("No hay comidas registradas para este día.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.meals) { meal ->
                DayMealRow(
                    meal,
                    onClick = { onEditMeal(meal.meal.id) },
                    onDelete = { viewModel.deleteMeal(meal.meal) }
                )
            }
        }
    }

    if (showExpenditureDialog) {
        ExpenditureInputDialog(
            initialValue = state.expenditure?.toString().orEmpty(),
            onDismiss = { showExpenditureDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.let(viewModel::saveExpenditure)
                showExpenditureDialog = false
            }
        )
    }
}

@Composable
private fun ExpenditureInputDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gasto calórico del día") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Calorías gastadas (kcal)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DayMealRow(item: MealWithPhotos, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val photoPath = item.allFoodPhotoPaths.firstOrNull()
            if (photoPath != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(photoPath)),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.RestaurantMenu, contentDescription = null, modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("${item.meal.mealType.label} · ${DateTimeFormatters.formatDateTime(item.meal.consumedAt)}", style = MaterialTheme.typography.bodyMedium)
                Text("${item.meal.calories} kcal · ${item.meal.detectedFoods}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}
