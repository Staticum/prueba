package com.staticum.diariocalorico.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.staticum.diariocalorico.export.CsvExporter
import com.staticum.diariocalorico.export.PdfReportExporter

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
        Column(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.values().forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.loadRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            CaloriesBarChart(
                values = state.dailyCalories.map { it.calories },
                goal = state.goals.calories,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val uri = CsvExporter.export(context, state.mealsInRange)
                    shareFile(context, uri, "text/csv")
                }) { Text("Exportar CSV") }
                Button(onClick = {
                    val uri = PdfReportExporter.export(context, state.mealsInRange)
                    shareFile(context, uri, "application/pdf")
                }) { Text("Exportar PDF") }
            }
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
