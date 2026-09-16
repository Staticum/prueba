package com.staticum.diariocalorico.ui.addmeal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealType
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.network.GeminiClient
import com.staticum.diariocalorico.network.GeminiResult
import com.staticum.diariocalorico.network.NutritionEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Done(val estimate: NutritionEstimate) : AnalysisState()
    data class Failed(val message: String) : AnalysisState()
}

data class AddMealFormState(
    val foodPhoto: File? = null,
    val labelPhotos: List<File> = emptyList(),
    val userNote: String = "",
    val consumedAt: Instant = Instant.now(),
    val mealType: MealType = MealType.ALMUERZO,
    val calories: String = "",
    val proteinGrams: String = "",
    val carbsGrams: String = "",
    val fatGrams: String = "",
    val detectedFoods: String = ""
)

class AddMealViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _form = MutableStateFlow(AddMealFormState())
    val form: StateFlow<AddMealFormState> = _form

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    fun setFoodPhoto(file: File) { _form.value = _form.value.copy(foodPhoto = file) }

    fun addLabelPhoto(file: File) {
        _form.value = _form.value.copy(labelPhotos = _form.value.labelPhotos + file)
    }

    fun removeLabelPhoto(file: File) {
        _form.value = _form.value.copy(labelPhotos = _form.value.labelPhotos - file)
    }

    fun setUserNote(note: String) { _form.value = _form.value.copy(userNote = note) }
    fun setConsumedAt(instant: Instant) { _form.value = _form.value.copy(consumedAt = instant) }
    fun setMealType(type: MealType) { _form.value = _form.value.copy(mealType = type) }

    fun updateEditableFields(
        calories: String = _form.value.calories,
        protein: String = _form.value.proteinGrams,
        carbs: String = _form.value.carbsGrams,
        fat: String = _form.value.fatGrams,
        foods: String = _form.value.detectedFoods
    ) {
        _form.value = _form.value.copy(
            calories = calories,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            detectedFoods = foods
        )
    }

    fun analyzeWithGemini() {
        val photo = _form.value.foodPhoto ?: run {
            _analysisState.value = AnalysisState.Failed("Primero toma o selecciona una foto del alimento")
            return
        }
        val apiKey = userPreferences.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _analysisState.value = AnalysisState.Failed("Configura tu API key de Gemini en Ajustes")
            return
        }

        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            val client = GeminiClient(apiKey)
            when (val result = client.estimateNutrition(photo, _form.value.labelPhotos, _form.value.userNote)) {
                is GeminiResult.Success -> {
                    val estimate = result.estimate
                    updateEditableFields(
                        calories = estimate.calories.toString(),
                        protein = estimate.proteinGrams.toString(),
                        carbs = estimate.carbsGrams.toString(),
                        fat = estimate.fatGrams.toString(),
                        foods = estimate.detectedFoods.joinToString(", ")
                    )
                    _analysisState.value = AnalysisState.Done(estimate)
                }
                is GeminiResult.Error -> _analysisState.value = AnalysisState.Failed(result.message)
            }
        }
    }

    fun saveMeal(context: Context, onSaved: () -> Unit, onError: (String) -> Unit) {
        val f = _form.value
        val photo = f.foodPhoto
        if (photo == null) { onError("Falta la foto del alimento"); return }
        val calories = f.calories.toIntOrNull()
        if (calories == null) { onError("Ingresa un valor válido de calorías"); return }

        viewModelScope.launch {
            val entry = MealEntry(
                consumedAt = f.consumedAt,
                mealType = f.mealType,
                description = f.userNote,
                foodPhotoPath = photo.absolutePath,
                calories = calories,
                proteinGrams = f.proteinGrams.toDoubleOrNull() ?: 0.0,
                carbsGrams = f.carbsGrams.toDoubleOrNull() ?: 0.0,
                fatGrams = f.fatGrams.toDoubleOrNull() ?: 0.0,
                detectedFoods = f.detectedFoods
            )
            repository.saveMeal(entry, f.labelPhotos.map { it.absolutePath })
            onSaved()
        }
    }
}
