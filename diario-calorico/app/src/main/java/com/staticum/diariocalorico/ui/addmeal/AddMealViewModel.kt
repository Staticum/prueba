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
    val editingMealId: Long? = null,
    val foodPhotos: List<File> = emptyList(),
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

    private val _frequentMeals = MutableStateFlow<List<MealEntry>>(emptyList())
    val frequentMeals: StateFlow<List<MealEntry>> = _frequentMeals

    init {
        viewModelScope.launch {
            _frequentMeals.value = repository.getFrequentMeals()
        }
    }

    fun loadForEdit(mealId: Long) {
        viewModelScope.launch {
            val existing = repository.getMealWithPhotos(mealId) ?: return@launch
            _form.value = AddMealFormState(
                editingMealId = existing.meal.id,
                foodPhotos = existing.allFoodPhotoPaths.map { File(it) },
                labelPhotos = existing.labelPhotos.map { File(it.photoPath) },
                userNote = existing.meal.description,
                consumedAt = existing.meal.consumedAt,
                mealType = existing.meal.mealType,
                calories = existing.meal.calories.toString(),
                proteinGrams = existing.meal.proteinGrams.toString(),
                carbsGrams = existing.meal.carbsGrams.toString(),
                fatGrams = existing.meal.fatGrams.toString(),
                detectedFoods = existing.meal.detectedFoods
            )
        }
    }

    fun addFoodPhoto(file: File) {
        _form.value = _form.value.copy(foodPhotos = _form.value.foodPhotos + file)
    }

    fun removeFoodPhoto(file: File) {
        _form.value = _form.value.copy(foodPhotos = _form.value.foodPhotos - file)
    }

    fun addLabelPhoto(file: File) {
        _form.value = _form.value.copy(labelPhotos = _form.value.labelPhotos + file)
    }

    fun removeLabelPhoto(file: File) {
        _form.value = _form.value.copy(labelPhotos = _form.value.labelPhotos - file)
    }

    fun setUserNote(note: String) { _form.value = _form.value.copy(userNote = note) }
    fun setConsumedAt(instant: Instant) { _form.value = _form.value.copy(consumedAt = instant) }
    fun setMealType(type: MealType) { _form.value = _form.value.copy(mealType = type) }

    /**
     * Al agregar una comida desde la vista de un día cerrado, se precarga esa fecha (a mediodía)
     * en vez de la hora actual, ya que la comida no fue registrada hoy.
     */
    fun presetDate(date: java.time.LocalDate) {
        val instant = date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant()
        _form.value = _form.value.copy(consumedAt = instant)
    }

    fun applyFrequentMeal(meal: MealEntry) {
        _form.value = _form.value.copy(
            mealType = meal.mealType,
            userNote = meal.description,
            calories = meal.calories.toString(),
            proteinGrams = meal.proteinGrams.toString(),
            carbsGrams = meal.carbsGrams.toString(),
            fatGrams = meal.fatGrams.toString(),
            detectedFoods = meal.detectedFoods
        )
    }

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
        val photos = _form.value.foodPhotos
        if (photos.isEmpty()) {
            _analysisState.value = AnalysisState.Failed("Primero toma o selecciona al menos una foto del alimento")
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
            when (val result = client.estimateNutrition(photos, _form.value.labelPhotos, _form.value.userNote)) {
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
        if (f.foodPhotos.isEmpty()) { onError("Falta al menos una foto del alimento"); return }
        // Si Gemini no estuvo disponible y el usuario no completó las calorías a mano,
        // se guarda igual con 0 en vez de bloquear el registro: se puede corregir después.
        val calories = f.calories.toIntOrNull() ?: 0

        viewModelScope.launch {
            val entry = MealEntry(
                id = f.editingMealId ?: 0,
                consumedAt = f.consumedAt,
                mealType = f.mealType,
                description = f.userNote,
                foodPhotoPath = f.foodPhotos.first().absolutePath,
                calories = calories,
                proteinGrams = f.proteinGrams.toDoubleOrNull() ?: 0.0,
                carbsGrams = f.carbsGrams.toDoubleOrNull() ?: 0.0,
                fatGrams = f.fatGrams.toDoubleOrNull() ?: 0.0,
                detectedFoods = f.detectedFoods
            )
            val extraFoodPhotos = f.foodPhotos.drop(1).map { it.absolutePath }
            val labelPhotos = f.labelPhotos.map { it.absolutePath }
            if (f.editingMealId != null) {
                repository.updateMeal(entry, extraFoodPhotos, labelPhotos)
            } else {
                repository.saveMeal(entry, extraFoodPhotos, labelPhotos)
            }
            onSaved()
        }
    }
}
