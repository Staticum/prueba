package com.staticum.diariocalorico.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.util.DateTimeFormatters

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMeal: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diario Calórico") },
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, contentDescription = "Historial") }
                    IconButton(onClick = onOpenReports) { Icon(Icons.Filled.ShowChart, contentDescription = "Reportes") }
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
            item { Text("Comidas de hoy", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            items(state.todayMeals) { meal -> MealRow(meal) }
        }
    }
}

@Composable
private fun ProgressCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.consumedCalories} / ${state.goals.calories} kcal")
            LinearProgressIndicator(
                progress = { (state.consumedCalories.toFloat() / state.goals.calories).coerceIn(0f, 1f) },
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
private fun MacroText(label: String, consumed: Double, goal: Int) {
    Text("$label: ${consumed.toInt()}/${goal}g")
}

@Composable
private fun MealRow(item: MealWithPhotos) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${item.meal.mealType.label} · ${DateTimeFormatters.formatDateTime(item.meal.consumedAt)}")
            Text("${item.meal.calories} kcal · ${item.meal.detectedFoods}")
        }
    }
}
