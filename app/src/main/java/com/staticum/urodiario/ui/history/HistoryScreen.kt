package com.staticum.urodiario.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.urodiario.data.RecordWithPhotos
import com.staticum.urodiario.ui.components.ColorSwatch
import com.staticum.urodiario.util.DateTimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onAddRecord: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onOpenReports: () -> Unit
) {
    val records by viewModel.records.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diario miccional") },
                actions = {
                    IconButton(onClick = onOpenReports) {
                        Icon(Icons.Filled.Assessment, contentDescription = "Reportes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecord) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar registro")
            }
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aún no tienes registros.\nToca + para agregar tu primera micción.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(records, key = { it.record.id }) { item ->
                    RecordRow(
                        item = item,
                        onClick = { onOpenRecord(item.record.id) },
                        onDelete = { viewModel.deleteRecord(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordRow(item: RecordWithPhotos, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(color = item.record.color, size = 28.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    DateTimeFormatters.formatDateTime(item.record.dateTimeMillis),
                    style = MaterialTheme.typography.bodyLarge
                )
                val details = buildList {
                    item.record.volumeMl?.let { add("$it ml") }
                    item.record.durationSeconds?.let { add(DateTimeFormatters.formatDuration(it)) }
                    add(item.record.odor.label)
                }.joinToString(" · ")
                Text(details, style = MaterialTheme.typography.labelMedium)
            }
            if (item.record.hasBlood) {
                Icon(
                    Icons.Filled.Bloodtype,
                    contentDescription = "Sangre presente",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (item.record.hasLeakage) {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = "Escape",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
