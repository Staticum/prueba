package com.staticum.diariocalorico.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoachScreen(viewModel: CoachViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach nutricional") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumen de hoy", style = MaterialTheme.typography.titleSmall)
                    Text("${state.consumedCalories} / ${state.goals.calories} kcal · ${state.todayMeals.size} comidas registradas", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Prot: ${state.consumedProtein.toInt()}/${state.goals.proteinGrams}g · Carb: ${state.consumedCarbs.toInt()}/${state.goals.carbsGrams}g · Grasa: ${state.consumedFat.toInt()}/${state.goals.fatGrams}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.yesterdayExpenditure != null || state.latestWeightKg != null) {
                        Text(
                            listOfNotNull(
                                state.yesterdayExpenditure?.let { "Gasto de ayer: $it kcal" },
                                state.latestWeightKg?.let { "Peso: $it kg" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.extraContext,
                onValueChange = viewModel::setExtraContext,
                label = { Text("Contexto adicional para Gemini (opcional)") },
                placeholder = { Text("Ej: voy a entrenar en la tarde, tengo antojo de algo dulce, ando con poco tiempo para cocinar...") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.analyze() },
                enabled = state.coachState !is CoachState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Analizar mi día con Gemini") }

            when (val coachState = state.coachState) {
                is CoachState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                is CoachState.Failed -> Text("Error: ${coachState.message}", color = MaterialTheme.colorScheme.error)
                is CoachState.Done -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(coachState.advice, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                else -> {}
            }
        }
    }
}
