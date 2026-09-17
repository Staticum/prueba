package com.staticum.diariocalorico.ui.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.staticum.diariocalorico.data.MealType
import com.staticum.diariocalorico.export.CsvExporter
import com.staticum.diariocalorico.export.PdfReportExporter
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                DateRangeControls(
                    startDate = state.startDate,
                    endDate = state.endDate,
                    onQuickRange = { days -> viewModel.loadRange(LocalDate.now().minusDays(days - 1L), LocalDate.now()) },
                    onCustomRange = { start, end -> viewModel.loadRange(start, end) }
                )
            }

            item { KpiRow(state.kpis) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.chartMode == ChartMode.CALORIES, onClick = { viewModel.setChartMode(ChartMode.CALORIES) }, label = { Text("Calorías") })
                    FilterChip(selected = state.chartMode == ChartMode.MACROS, onClick = { viewModel.setChartMode(ChartMode.MACROS) }, label = { Text("Macros") })
                    FilterChip(selected = state.chartMode == ChartMode.DEFICIT, onClick = { viewModel.setChartMode(ChartMode.DEFICIT) }, label = { Text("Déficit") })
                }
            }

            item {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        when (state.chartMode) {
                            ChartMode.CALORIES -> CaloriesBarChart(state.dailyCalories, state.goals.calories, modifier = Modifier.fillMaxWidth())
                            ChartMode.MACROS -> StackedMacrosChart(state.dailyMacros, modifier = Modifier.fillMaxWidth())
                            ChartMode.DEFICIT -> DeficitChart(state.dailyCalories, state.dailyExpenditure, state.dailyWeight, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Por tipo de comida", style = MaterialTheme.typography.titleSmall)
                    MealTypeBreakdownChart(state.mealTypeBreakdown, modifier = Modifier.padding(top = 8.dp))
                }
            }

            item {
                Column {
                    Text("Filtrar comidas del período", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.mealTypeFilter == null, onClick = { viewModel.setMealTypeFilter(null) }, label = { Text("Todos") })
                        MealType.values().forEach { type ->
                            FilterChip(selected = state.mealTypeFilter == type, onClick = { viewModel.setMealTypeFilter(type) }, label = { Text(type.label) })
                        }
                    }
                    OutlinedTextField(
                        value = state.foodSearchQuery,
                        onValueChange = viewModel::setFoodSearchQuery,
                        label = { Text("Buscar alimento (ej. pan)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Text(
                        "${state.filteredMeals.size} comidas · ${state.filteredTotalCalories} kcal en total",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val uri = CsvExporter.export(context, state.filteredMeals.ifEmpty { state.mealsInRange })
                        shareFile(context, uri, "text/csv")
                    }) { Text("Exportar CSV") }
                    Button(onClick = {
                        val uri = PdfReportExporter.export(context, state)
                        shareFile(context, uri, "application/pdf")
                    }) { Text("Exportar PDF") }
                }
            }
        }
    }
}

@Composable
private fun DateRangeControls(
    startDate: LocalDate,
    endDate: LocalDate,
    onQuickRange: (Long) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit
) {
    val context = LocalContext.current
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = false, onClick = { onQuickRange(7) }, label = { Text("7 días") })
            FilterChip(selected = false, onClick = { onQuickRange(30) }, label = { Text("30 días") })
            OutlinedButton(onClick = {
                pickDate(context, startDate) { newStart ->
                    pickDate(context, endDate) { newEnd -> onCustomRange(newStart, newEnd) }
                }
            }) { Text("Rango personalizado") }
        }
        Text(
            "${DateTimeFormatters.formatDate(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())} - ${DateTimeFormatters.formatDate(endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun pickDate(context: android.content.Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    val cal = Calendar.getInstance().apply { set(initial.year, initial.monthValue - 1, initial.dayOfMonth) }
    DatePickerDialog(context, { _, year, month, day ->
        onPicked(LocalDate.of(year, month + 1, day))
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable
private fun KpiRow(kpis: ReportKpis) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { KpiCard("Promedio diario", "${kpis.avgCalories} kcal") }
        item { KpiCard("Días dentro de meta", "${kpis.daysOnTrack} / ${kpis.daysOnTrack + kpis.daysOffTrack}") }
        item { KpiCard("Macro dominante", kpis.dominantMacroLabel) }
        item { KpiCard("Racha actual", "${kpis.streakDays} días") }
        kpis.avgVsPreviousPeriod?.let { delta ->
            item {
                val sign = if (delta >= 0) "+" else ""
                val color = if (delta > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                KpiCard("vs. período anterior", "$sign$delta kcal/día", valueColor = color)
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, valueColor: Color? = null) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun shareFile(context: android.content.Context, uri: android.net.Uri, mimeType: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Compartir reporte"))
}
