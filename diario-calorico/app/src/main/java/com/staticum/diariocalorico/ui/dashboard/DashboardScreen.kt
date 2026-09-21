package com.staticum.diariocalorico.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMeal: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditMeal: (Long) -> Unit,
    onOpenCoach: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diario Calórico") },
                actions = {
                    IconButton(onClick = onOpenCoach) { Icon(Icons.Filled.TipsAndUpdates, contentDescription = "Coach nutricional") }
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, contentDescription = "Historial") }
                    IconButton(onClick = onOpenReports) { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Reportes") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal) { Icon(Icons.Filled.Add, contentDescription = "Agregar comida") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ProgressCard(state) }
            item {
                TrackingCard(
                    yesterdayExpenditure = state.yesterdayExpenditure,
                    latestWeightKg = state.latestWeightKg,
                    onSaveExpenditure = viewModel::saveYesterdayExpenditure,
                    onSaveWeight = viewModel::saveWeight
                )
            }
            item { Text("Comidas de hoy", style = MaterialTheme.typography.titleMedium) }
            if (state.todayMeals.isEmpty()) {
                item { Text("Aún no registras comidas hoy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.todayMeals) { meal -> MealRow(meal, onClick = { onEditMeal(meal.meal.id) }) }
        }
    }
}

@Composable
private fun ProgressCard(state: DashboardUiState) {
    val ratio = (state.consumedCalories.toFloat() / state.goals.calories).coerceAtLeast(0f)
    val progressColor = when {
        ratio > 1.05f -> MaterialTheme.colorScheme.error
        ratio > 0.9f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.consumedCalories} / ${state.goals.calories} kcal", style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                color = progressColor,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroText("Prot", state.consumedProtein, state.goals.proteinGrams)
                MacroText("Carb", state.consumedCarbs, state.goals.carbsGrams)
                MacroText("Grasa", state.consumedFat, state.goals.fatGrams)
            }
        }
    }
}

@Composable
private fun TrackingCard(
    yesterdayExpenditure: Int?,
    latestWeightKg: Double?,
    onSaveExpenditure: (Int) -> Unit,
    onSaveWeight: (Double) -> Unit
) {
    var showExpenditureDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Seguimiento (reloj Polar)", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showExpenditureDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gasto calórico de ayer", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (yesterdayExpenditure != null) "$yesterdayExpenditure kcal" else "Sin registrar · toca para ingresar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showWeightDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Peso actual", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (latestWeightKg != null) "$latestWeightKg kg" else "Sin registrar · toca para ingresar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.MonitorWeight, contentDescription = null)
            }
        }
    }

    if (showExpenditureDialog) {
        NumberInputDialog(
            title = "Gasto calórico de ayer",
            label = "Calorías gastadas (kcal)",
            initialValue = yesterdayExpenditure?.toString().orEmpty(),
            onDismiss = { showExpenditureDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.let(onSaveExpenditure)
                showExpenditureDialog = false
            }
        )
    }

    if (showWeightDialog) {
        NumberInputDialog(
            title = "Registrar peso",
            label = "Peso (kg)",
            initialValue = latestWeightKg?.toString().orEmpty(),
            onDismiss = { showWeightDialog = false },
            onConfirm = { value ->
                value.toDoubleOrNull()?.let(onSaveWeight)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun NumberInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun MacroText(label: String, consumed: Double, goal: Int) {
    Text("$label: ${consumed.toInt()}/${goal}g", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MealRow(item: MealWithPhotos, onClick: () -> Unit) {
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
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("${item.meal.mealType.label} · ${DateTimeFormatters.formatDateTime(item.meal.consumedAt)}", style = MaterialTheme.typography.bodyMedium)
                Text("${item.meal.calories} kcal · ${item.meal.detectedFoods}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
