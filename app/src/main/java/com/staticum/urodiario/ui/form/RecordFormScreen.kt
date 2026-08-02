package com.staticum.urodiario.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.staticum.urodiario.data.UrineColor
import com.staticum.urodiario.data.UrineOdor
import com.staticum.urodiario.ui.components.ColorSwatch
import com.staticum.urodiario.ui.components.ScaleSelector
import com.staticum.urodiario.util.PhotoFiles
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(
    viewModel: RecordFormViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { viewModel.addPhoto(it.toString()) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val stored = PhotoFiles.copyContentUriToAppStorage(context, uri)
            viewModel.addPhoto(stored.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar registro" else "Nuevo registro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) { Text("Guardar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fecha y hora
            Text("Momento del registro", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val zoned = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault())
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(zoned.toLocalDate().toString())
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(String.format("%02d:%02d", zoned.hour, zoned.minute))
                }
            }

            // Volumen y duración
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.volumeMl,
                    onValueChange = viewModel::updateVolume,
                    label = { Text("Cantidad (ml)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.durationSeconds,
                    onValueChange = viewModel::updateDuration,
                    label = { Text("Duración (s)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Color
            Text("Color", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UrineColor.values().toList()) { color ->
                    FilterChip(
                        selected = state.color == color,
                        onClick = { viewModel.updateColor(color) },
                        leadingIcon = { ColorSwatch(color = color, size = 16.dp) },
                        label = { Text(color.label) }
                    )
                }
            }

            // Olor
            Text("Olor", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UrineOdor.values().toList()) { odor ->
                    FilterChip(
                        selected = state.odor == odor,
                        onClick = { viewModel.updateOdor(odor) },
                        label = { Text(odor.label) }
                    )
                }
            }

            // Urgencia y dolor
            ScaleSelector("Urgencia (0=ninguna, 4=extrema)", state.urgency, 0..4, viewModel::updateUrgency)
            ScaleSelector("Dolor / ardor (0=ninguno, 10=máximo)", state.painLevel, 0..10, viewModel::updatePainLevel)

            // Ingesta de líquidos
            OutlinedTextField(
                value = state.fluidIntakeMl,
                onValueChange = viewModel::updateFluidIntake,
                label = { Text("Líquido ingerido antes (ml, opcional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Switches
            SwitchRow("Presencia de sangre", state.hasBlood, viewModel::updateHasBlood)
            SwitchRow("Escape / incontinencia", state.hasLeakage, viewModel::updateHasLeakage)
            SwitchRow("Ocurrió de noche (nocturia)", state.isNocturnal, viewModel::updateIsNocturnal)

            // Notas
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Comentarios / notas") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            // Fotos
            Text("Fotos", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.displayPhotoUris) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removePhoto(uri) },
                            modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White)
                        }
                    }
                }
                item {
                    IconButton(onClick = {
                        val uri = PhotoFiles.createNewPhotoUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Tomar foto")
                    }
                }
                item {
                    IconButton(onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Elegir de galería")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val zoned = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneOffset.UTC)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = zoned.toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val currentTime = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                        val combined = newDate.atTime(currentTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        viewModel.updateDateTime(combined)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val zoned = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = zoned.hour,
            initialMinute = zoned.minute,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val currentDate = Instant.ofEpochMilli(state.dateTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val combined = currentDate.atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.updateDateTime(combined)
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
