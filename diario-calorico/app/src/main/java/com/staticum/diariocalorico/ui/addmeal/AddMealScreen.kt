package com.staticum.diariocalorico.ui.addmeal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.staticum.diariocalorico.data.MealType
import com.staticum.diariocalorico.util.DateTimeFormatters
import com.staticum.diariocalorico.util.PhotoFiles
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

@Composable
fun AddMealScreen(
    viewModel: AddMealViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val form by viewModel.form.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<java.io.File?>(null) }
    var pendingLabelUri by remember { mutableStateOf<Uri?>(null) }
    var pendingLabelFile by remember { mutableStateOf<java.io.File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val foodCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraFile?.let { viewModel.setFoodPhoto(it) }
    }
    val foodGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setFoodPhoto(PhotoFiles.copyFromUri(context, it)) }
    }
    val labelCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingLabelFile?.let { viewModel.addLabelPhoto(it) }
    }
    val labelGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addLabelPhoto(PhotoFiles.copyFromUri(context, it)) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Nueva comida") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Foto del alimento *")
            form.foodPhoto?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val (file, uri) = PhotoFiles.createPhotoUri(context)
                    pendingCameraFile = file; pendingCameraUri = uri
                    foodCameraLauncher.launch(uri)
                }) { Text("Cámara") }
                OutlinedButton(onClick = { foodGalleryLauncher.launch("image/*") }) { Text("Galería") }
            }

            Text("Fotos de etiqueta nutricional (opcional)")
            form.labelPhotos.forEach { file ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(file),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )
                    OutlinedButton(onClick = { viewModel.removeLabelPhoto(file) }) { Text("Quitar") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val (file, uri) = PhotoFiles.createPhotoUri(context)
                    pendingLabelFile = file; pendingLabelUri = uri
                    labelCameraLauncher.launch(uri)
                }) { Text("+ Etiqueta (cámara)") }
                OutlinedButton(onClick = { labelGalleryLauncher.launch("image/*") }) { Text("+ Etiqueta (galería)") }
            }

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
                is AnalysisState.Loading -> CircularProgressIndicator()
                is AnalysisState.Failed -> Text("Error: ${state.message}")
                is AnalysisState.Done -> Text("Nota de confianza: ${state.estimate.confidenceNote}")
                else -> {}
            }

            Text("Resultado (editable antes de guardar)")
            OutlinedTextField(value = form.calories, onValueChange = { viewModel.updateEditableFields(calories = it) }, label = { Text("Calorías (kcal)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = form.proteinGrams, onValueChange = { viewModel.updateEditableFields(protein = it) }, label = { Text("Proteína (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = form.carbsGrams, onValueChange = { viewModel.updateEditableFields(carbs = it) }, label = { Text("Carbohidratos (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = form.fatGrams, onValueChange = { viewModel.updateEditableFields(fat = it) }, label = { Text("Grasa (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = form.detectedFoods, onValueChange = { viewModel.updateEditableFields(foods = it) }, label = { Text("Alimentos detectados") }, modifier = Modifier.fillMaxWidth())

            errorMessage?.let { Text("Error: $it") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Cancelar") }
                Button(onClick = {
                    viewModel.saveMeal(context, onSaved = onSaved, onError = { errorMessage = it })
                }) { Text("Guardar") }
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
