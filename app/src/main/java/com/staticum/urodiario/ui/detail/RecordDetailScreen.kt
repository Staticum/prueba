package com.staticum.urodiario.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.staticum.urodiario.data.RecordWithPhotos
import com.staticum.urodiario.ui.components.ColorSwatch
import com.staticum.urodiario.util.DateTimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordWithPhotosProvider: () -> RecordWithPhotos?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val item = recordWithPhotosProvider()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del registro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Text("Registro no encontrado", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                DateTimeFormatters.formatDateTime(item.record.dateTimeMillis),
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()

            DetailRow("Cantidad", item.record.volumeMl?.let { "$it ml" } ?: "No registrado")
            DetailRow("Duración", DateTimeFormatters.formatDuration(item.record.durationSeconds))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Color: ", style = MaterialTheme.typography.bodyLarge)
                ColorSwatch(color = item.record.color, modifier = Modifier.padding(horizontal = 6.dp))
                Text(item.record.color.label, style = MaterialTheme.typography.bodyLarge)
            }
            DetailRow("Olor", item.record.odor.label)
            DetailRow("Urgencia (0-4)", item.record.urgency.toString())
            DetailRow("Dolor / ardor (0-10)", item.record.painLevel.toString())
            DetailRow("Presencia de sangre", if (item.record.hasBlood) "Sí" else "No")
            DetailRow("Escape / incontinencia", if (item.record.hasLeakage) "Sí" else "No")
            DetailRow("Nocturno", if (item.record.isNocturnal) "Sí" else "No")
            item.record.fluidIntakeMl?.let { DetailRow("Líquido ingerido antes", "$it ml") }

            if (!item.record.notes.isNullOrBlank()) {
                Text("Notas", style = MaterialTheme.typography.labelMedium)
                Text(item.record.notes)
            }

            if (item.photos.isNotEmpty()) {
                Text("Fotos", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.photos) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
