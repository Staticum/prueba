package com.staticum.urodiario.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.staticum.urodiario.export.CsvExporter
import com.staticum.urodiario.export.PdfReportExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel, onBack: () -> Unit) {
    val summaries by viewModel.dailySummaries.collectAsState()
    val avgVolume by viewModel.averageVolumeMl.collectAsState()
    val avgFrequency by viewModel.averageFrequencyPerDay.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun shareUri(uri: android.net.Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Volumen promedio", "$avgVolume ml", Modifier.weight(1f))
                SummaryCard("Frecuencia diaria", String.format("%.1f", avgFrequency), Modifier.weight(1f))
            }

            Text("Frecuencia últimos 14 días", style = MaterialTheme.typography.labelMedium)
            DailyCountBarChart(summaries = summaries)

            Text(
                "Las barras en rojo indican días con episodios de sangre o escape registrados.",
                style = MaterialTheme.typography.labelMedium
            )

            Text("Exportar para tu médico", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    scope.launch {
                        val records = viewModel.getAllRecordsForExport()
                        val uri = CsvExporter.export(context, records)
                        shareUri(uri, "text/csv")
                    }
                }) { Text("Exportar CSV") }

                Button(onClick = {
                    scope.launch {
                        val records = viewModel.getAllRecordsForExport()
                        val uri = PdfReportExporter.export(context, records)
                        shareUri(uri, "application/pdf")
                    }
                }) { Text("Exportar PDF") }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
