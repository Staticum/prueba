package com.staticum.diariocalorico.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.staticum.diariocalorico.data.DailyGoals

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var apiKeyField by remember { mutableStateOf(state.apiKey) }
    var caloriesField by remember { mutableStateOf(state.goals.calories.toString()) }
    var proteinField by remember { mutableStateOf(state.goals.proteinGrams.toString()) }
    var carbsField by remember { mutableStateOf(state.goals.carbsGrams.toString()) }
    var fatField by remember { mutableStateOf(state.goals.fatGrams.toString()) }

    LaunchedEffect(state.goals) {
        caloriesField = state.goals.calories.toString()
        proteinField = state.goals.proteinGrams.toString()
        carbsField = state.goals.carbsGrams.toString()
        fatField = state.goals.fatGrams.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("API key de Gemini")
            OutlinedTextField(
                value = apiKeyField,
                onValueChange = { apiKeyField = it },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { viewModel.saveApiKey(apiKeyField) }) { Text("Guardar API key") }

            Text("Metas diarias")
            OutlinedTextField(value = caloriesField, onValueChange = { caloriesField = it }, label = { Text("Calorías (kcal)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = proteinField, onValueChange = { proteinField = it }, label = { Text("Proteína (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = carbsField, onValueChange = { carbsField = it }, label = { Text("Carbohidratos (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fatField, onValueChange = { fatField = it }, label = { Text("Grasa (g)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                viewModel.saveGoals(
                    DailyGoals(
                        calories = caloriesField.toIntOrNull() ?: state.goals.calories,
                        proteinGrams = proteinField.toIntOrNull() ?: state.goals.proteinGrams,
                        carbsGrams = carbsField.toIntOrNull() ?: state.goals.carbsGrams,
                        fatGrams = fatField.toIntOrNull() ?: state.goals.fatGrams
                    )
                )
            }) { Text("Guardar metas") }

            Text("Versión instalada: ${state.versionName}")
            OutlinedButton(onClick = { viewModel.checkForUpdate() }) { Text("Buscar actualizaciones") }
            if (state.updateStatus.isNotBlank()) Text(state.updateStatus)
            state.downloadProgress?.let { progress ->
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            if (state.updateAvailable != null) {
                Button(onClick = { viewModel.downloadAndInstallUpdate() }) { Text("Descargar e instalar") }
            }
        }
    }
}
