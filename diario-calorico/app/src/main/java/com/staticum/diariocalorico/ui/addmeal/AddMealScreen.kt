package com.staticum.diariocalorico.ui.addmeal

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealType
import com.staticum.diariocalorico.util.DateTimeFormatters
import com.staticum.diariocalorico.util.PhotoFiles
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    viewModel: AddMealViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val form by viewModel.form.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val frequentMeals by viewModel.frequentMeals.collectAsState()

    var pendingCameraTarget by remember { mutableStateOf<File?>(null) }
    var pendingCameraIsLabel by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val target = pendingCameraTarget
        if (success && target != null) {
            if (pendingCameraIsLabel) viewModel.addLabelPhoto(target) else viewModel.addFoodPhoto(target)
        }
        pendingCameraTarget = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraAction?.invoke()
        } else {
            errorMessage = "Se necesita permiso de cámara para tomar la foto"
        }
        pendingCameraAction = null
    }

    fun launchCamera(isLabel: Boolean) {
        val action = {
            val (file, uri) = PhotoFiles.createPhotoUri(context)
            pendingCameraTarget = file
            pendingCameraIsLabel = isLabel
            cameraLauncher.launch(uri)
        }
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            pendingCameraAction = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val foodGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.addFoodPhoto(PhotoFiles.copyFromUri(context, it)) }
    }
    val labelGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.addLabelPhoto(PhotoFiles.copyFromUri(context, it)) }
    }

    var previousPhotosTargetIsLabel by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (form.editingMealId != null) "Editar comida" else "Nueva comida") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (frequentMeals.isNotEmpty() && form.editingMealId == null) {
                SectionLabel("Comidas frecuentes")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(frequentMeals) { meal ->
                        AssistChip(
                            onClick = { viewModel.applyFrequentMeal(meal) },
                            label = { Text("${meal.detectedFoods.take(24)} · ${meal.calories} kcal") }
                        )
                    }
                }
            }

            SectionLabel("Foto(s) del alimento *")
            PhotoGrid(
                photos = form.foodPhotos,
                onRemove = viewModel::removeFoodPhoto
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launchCamera(isLabel = false) }) { Text("Cámara") }
                OutlinedButton(onClick = { foodGalleryLauncher.launch("image/*") }) { Text("Galería") }
                OutlinedButton(onClick = { previousPhotosTargetIsLabel = false }) { Text("Anteriores") }
            }
            Text(
                "Si tu comida tiene varios platos (ej. entrada, fondo, postre), agrega una foto de cada uno; se estiman como una sola comida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            SectionLabel("Etiqueta nutricional (opcional)")
            PhotoGrid(
                photos = form.labelPhotos,
                onRemove = viewModel::removeLabelPhoto
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launchCamera(isLabel = true) }) { Text("+ Etiqueta (cámara)") }
                OutlinedButton(onClick = { labelGalleryLauncher.launch("image/*") }) { Text("+ Etiqueta (galería)") }
                OutlinedButton(onClick = { previousPhotosTargetIsLabel = true }) { Text("+ Anteriores") }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = form.userNote,
                onValueChange = viewModel::setUserNote,
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            MealTypeDropdown(form.mealType, viewModel::setMealType)

            DateTimePickerRow(form.consumedAt, viewModel::setConsumedAt)

            Button(
                onClick = { viewModel.analyzeWithGemini() },
                enabled = analysisState !is AnalysisState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Analizar con Gemini") }

            when (val state = analysisState) {
                is AnalysisState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Text(state.progress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is AnalysisState.Failed -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                is AnalysisState.Done -> NutritionalInsightCard(state.estimate)
                is AnalysisState.AutoSavedPending -> Text(
                    "Gemini no respondió a tiempo. La comida se guardó igual; el análisis se reintentará solo en segundo plano y te avisaremos cuando esté listo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> {}
            }

            LaunchedEffect(analysisState) {
                if (analysisState is AnalysisState.AutoSavedPending) {
                    kotlinx.coroutines.delay(1800)
                    onSaved()
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resultado (editable antes de guardar)", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = form.calories, onValueChange = { viewModel.updateEditableFields(calories = it) }, label = { Text("Calorías (kcal)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = form.proteinGrams, onValueChange = { viewModel.updateEditableFields(protein = it) }, label = { Text("Proteína (g)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = form.carbsGrams, onValueChange = { viewModel.updateEditableFields(carbs = it) }, label = { Text("Carbohidratos (g)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = form.fatGrams, onValueChange = { viewModel.updateEditableFields(fat = it) }, label = { Text("Grasa (g)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = form.detectedFoods, onValueChange = { viewModel.updateEditableFields(foods = it) }, label = { Text("Alimentos detectados") }, modifier = Modifier.fillMaxWidth())
                }
            }

            errorMessage?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = { viewModel.saveMeal(context, onSaved = onSaved, onError = { errorMessage = it }) },
                    modifier = Modifier.weight(1f)
                ) { Text("Guardar") }
            }
        }
    }

    previousPhotosTargetIsLabel?.let { isLabel ->
        PreviousPhotosDialog(
            onDismiss = { previousPhotosTargetIsLabel = null },
            onSelected = { file ->
                val copy = PhotoFiles.copyFromFile(context, file)
                if (isLabel) viewModel.addLabelPhoto(copy) else viewModel.addFoodPhoto(copy)
                previousPhotosTargetIsLabel = null
            }
        )
    }
}

@Composable
private fun PreviousPhotosDialog(onDismiss: () -> Unit, onSelected: (File) -> Unit) {
    val context = LocalContext.current
    val photos = remember { PhotoFiles.listPreviousPhotos(context) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fotos anteriores") },
        text = {
            if (photos.isEmpty()) {
                Text("Todavía no tienes fotos usadas en comidas anteriores.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    gridItems(photos) { file ->
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                                .clickable { onSelected(file) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun NutritionalInsightCard(estimate: com.staticum.diariocalorico.network.NutritionEstimate) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aprende sobre lo que comiste", style = MaterialTheme.typography.titleSmall)
            if (estimate.confidenceNote.isNotBlank()) {
                Text("Nota de confianza: ${estimate.confidenceNote}", style = MaterialTheme.typography.bodySmall)
            }
            if (estimate.benefits.isNotBlank()) {
                InsightRow("Beneficios", estimate.benefits)
            }
            if (estimate.drawbacks.isNotBlank()) {
                InsightRow("A tener en cuenta", estimate.drawbacks)
            }
            if (estimate.frequencyAdvice.isNotBlank()) {
                InsightRow("¿Consumo frecuente?", estimate.frequencyAdvice)
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun PhotoGrid(photos: List<File>, onRemove: (File) -> Unit) {
    if (photos.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos) { file ->
            Box(modifier = Modifier.size(96.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(0.dp),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemove(file) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTypeDropdown(selected: MealType, onSelected: (MealType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de comida") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MealType.values().forEach { type ->
                DropdownMenuItem(text = { Text(type.label) }, onClick = { onSelected(type); expanded = false })
            }
        }
    }
}

@Composable
private fun DateTimePickerRow(consumedAt: Instant, onChanged: (Instant) -> Unit) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = consumedAt.toEpochMilli() }
            DatePickerDialog(context, { _, year, month, day ->
                cal.set(year, month, day)
                onChanged(Instant.ofEpochMilli(cal.timeInMillis))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }) { Text(DateTimeFormatters.formatDate(consumedAt)) }

        OutlinedButton(onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = consumedAt.toEpochMilli() }
            TimePickerDialog(context, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                onChanged(Instant.ofEpochMilli(cal.timeInMillis))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }) { Text(consumedAt.atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)) }
    }
}
