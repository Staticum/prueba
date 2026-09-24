package com.staticum.diariocalorico.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.GeminiLogRepository
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.network.CoachResult
import com.staticum.diariocalorico.network.GeminiClient
import com.staticum.diariocalorico.util.DateTimeFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

sealed class DayAnalysisState {
    object Idle : DayAnalysisState()
    data class Loading(val progress: String = "Analizando...") : DayAnalysisState()
    data class Done(val advice: String) : DayAnalysisState()
    data class Failed(val message: String) : DayAnalysisState()
}

data class DayDetailUiState(
    val date: LocalDate = LocalDate.now(),
    val meals: List<MealWithPhotos> = emptyList(),
    val goals: DailyGoals = DailyGoals(),
    val consumedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFat: Double = 0.0,
    val expenditure: Int? = null,
    val analysisState: DayAnalysisState = DayAnalysisState.Idle
)

class DayDetailViewModel(
    private val date: LocalDate,
    private val repository: MealRepository,
    private val userPreferences: UserPreferences,
    private val trackingRepository: TrackingRepository,
    private val geminiLogRepository: GeminiLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState(date = date))
    val uiState: StateFlow<DayDetailUiState> = _uiState

    private val zone = ZoneId.systemDefault()

    init {
        val startOfDay = date.atStartOfDay(zone).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()

        repository.observeMealsBetween(startOfDay, endOfDay)
            .combine(userPreferences.dailyGoals) { meals, goals -> meals to goals }
            .combine(trackingRepository.observeExpenditureForDate(date)) { (meals, goals), expenditure ->
                Triple(meals, goals, expenditure?.caloriesBurned)
            }
            .onEach { (meals, goals, expenditure) ->
                _uiState.value = _uiState.value.copy(
                    meals = meals,
                    goals = goals,
                    consumedCalories = meals.sumOf { it.meal.calories },
                    consumedProtein = meals.sumOf { it.meal.proteinGrams },
                    consumedCarbs = meals.sumOf { it.meal.carbsGrams },
                    consumedFat = meals.sumOf { it.meal.fatGrams },
                    expenditure = expenditure
                )
            }
            .launchIn(viewModelScope)
    }

    fun saveExpenditure(caloriesBurned: Int) {
        viewModelScope.launch { trackingRepository.saveExpenditure(date, caloriesBurned) }
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }

    fun analyze() {
        val apiKey = userPreferences.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(analysisState = DayAnalysisState.Failed("Configura tu API key de Gemini en Ajustes"))
            return
        }

        _uiState.value = _uiState.value.copy(analysisState = DayAnalysisState.Loading())
        viewModelScope.launch {
            val weight = trackingRepository.getWeightsBetween(
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant()
            ).lastOrNull() ?: trackingRepository.observeLatestWeight().first()

            val prompt = buildPrompt(_uiState.value, weight?.weightKg)
            val client = GeminiClient(apiKey, onLog = { entry -> geminiLogRepository.log(entry) })
            val result = client.getDailyAdvice(
                prompt,
                context = "day-analysis:$date",
                onProgress = { progress -> _uiState.value = _uiState.value.copy(analysisState = DayAnalysisState.Loading(progress)) }
            )
            when (result) {
                is CoachResult.Success -> _uiState.value = _uiState.value.copy(analysisState = DayAnalysisState.Done(result.advice))
                is CoachResult.Error -> _uiState.value = _uiState.value.copy(analysisState = DayAnalysisState.Failed(result.message))
            }
        }
    }

    private fun buildPrompt(state: DayDetailUiState, weightKg: Double?): String {
        val mealsText = if (state.meals.isEmpty()) {
            "No se registró ninguna comida ese día."
        } else {
            state.meals.joinToString("\n") { item ->
                val meal = item.meal
                "- ${DateTimeFormatters.formatDateTime(meal.consumedAt)} (${meal.mealType.label}): " +
                    "${meal.calories} kcal, P:${meal.proteinGrams}g C:${meal.carbsGrams}g G:${meal.fatGrams}g — ${meal.detectedFoods}"
            }
        }
        val expenditureText = state.expenditure?.let { "$it kcal" } ?: "no registrado"
        val balanceText = state.expenditure?.let { "${state.consumedCalories - it} kcal" } ?: "no se puede calcular (falta el gasto)"

        return """
            Eres un nutricionista experto. Analiza el día ya cerrado del usuario (no se puede
            modificar el consumo de ese día, así que no sugieras qué comer en lo que resta — el
            objetivo es evaluar lo ocurrido y dejar aprendizajes).

            Fecha analizada: ${DateTimeFormatters.formatLongDate(state.date)}.

            Meta diaria: ${state.goals.calories} kcal (Proteína ${state.goals.proteinGrams}g, Carbohidratos ${state.goals.carbsGrams}g, Grasa ${state.goals.fatGrams}g).
            Consumo total ese día: ${state.consumedCalories} kcal (Proteína ${state.consumedProtein}g, Carbohidratos ${state.consumedCarbs}g, Grasa ${state.consumedFat}g).
            Gasto calórico ese día (reloj Polar): $expenditureText.
            Balance neto (consumo - gasto): $balanceText.
            Peso registrado en torno a esa fecha: ${weightKg?.let { "$it kg" } ?: "no disponible"}.

            Comidas registradas ese día:
            $mealsText

            Instrucciones:
            - Evalúa el balance calórico del día (déficit o superávit) respecto al gasto real medido
              por el reloj, no solo respecto a la meta.
            - Comenta la calidad nutricional de lo consumido ese día: qué estuvo bien, qué se puede
              mejorar, y por qué (efectos/beneficios concretos, no generalidades).
            - Si el balance o la composición de macros fue problemático, explica el motivo probable
              y qué ajuste concreto aplicar en un día similar futuro.
            - Sé un análisis retrospectivo y de aprendizaje, no una sugerencia de qué comer ahora.
            - Responde en español, tono directo y cercano, en 3 a 6 párrafos cortos o una lista breve.
        """.trimIndent()
    }
}
