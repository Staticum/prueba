package com.staticum.diariocalorico.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.network.CoachResult
import com.staticum.diariocalorico.network.GeminiClient
import com.staticum.diariocalorico.util.DateTimeFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed class CoachState {
    object Idle : CoachState()
    object Loading : CoachState()
    data class Done(val advice: String) : CoachState()
    data class Failed(val message: String) : CoachState()
}

data class CoachUiState(
    val todayMeals: List<MealEntry> = emptyList(),
    val goals: DailyGoals = DailyGoals(),
    val consumedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFat: Double = 0.0,
    val yesterdayExpenditure: Int? = null,
    val latestWeightKg: Double? = null,
    val extraContext: String = "",
    val coachState: CoachState = CoachState.Idle
)

class CoachViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState

    private val zone = ZoneId.systemDefault()

    init {
        viewModelScope.launch { loadTodayData() }
    }

    private suspend fun loadTodayData() {
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        val meals = repository.getMealsBetween(startOfDay, endOfDay)
        val goals = userPreferences.dailyGoals.first()
        val expenditure = trackingRepository.getExpenditureForDate(today.minusDays(1))
        val weight = trackingRepository.observeLatestWeight().first()

        _uiState.value = _uiState.value.copy(
            todayMeals = meals,
            goals = goals,
            consumedCalories = meals.sumOf { it.calories },
            consumedProtein = meals.sumOf { it.proteinGrams },
            consumedCarbs = meals.sumOf { it.carbsGrams },
            consumedFat = meals.sumOf { it.fatGrams },
            yesterdayExpenditure = expenditure?.caloriesBurned,
            latestWeightKg = weight?.weightKg
        )
    }

    fun setExtraContext(text: String) {
        _uiState.value = _uiState.value.copy(extraContext = text)
    }

    fun analyze() {
        val apiKey = userPreferences.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(coachState = CoachState.Failed("Configura tu API key de Gemini en Ajustes"))
            return
        }

        _uiState.value = _uiState.value.copy(coachState = CoachState.Loading)
        viewModelScope.launch {
            val prompt = buildPrompt(_uiState.value)
            val client = GeminiClient(apiKey)
            when (val result = client.getDailyAdvice(prompt)) {
                is CoachResult.Success -> _uiState.value = _uiState.value.copy(coachState = CoachState.Done(result.advice))
                is CoachResult.Error -> _uiState.value = _uiState.value.copy(coachState = CoachState.Failed(result.message))
            }
        }
    }

    private fun buildPrompt(state: CoachUiState): String {
        val now = LocalTime.now(zone)
        val remainingCalories = state.goals.calories - state.consumedCalories
        val mealsText = if (state.todayMeals.isEmpty()) {
            "Todavía no ha registrado ninguna comida hoy."
        } else {
            state.todayMeals.joinToString("\n") { meal ->
                "- ${DateTimeFormatters.formatDateTime(meal.consumedAt)} (${meal.mealType.label}): " +
                    "${meal.calories} kcal, P:${meal.proteinGrams}g C:${meal.carbsGrams}g G:${meal.fatGrams}g — ${meal.detectedFoods}"
            }
        }
        val expenditurePart = state.yesterdayExpenditure?.let { "Gasto calórico de ayer (reloj Polar): $it kcal." }
            ?: "No registró su gasto calórico de ayer."
        val weightPart = state.latestWeightKg?.let { "Peso actual: $it kg." } ?: "No ha registrado su peso."
        val contextPart = if (state.extraContext.isNotBlank()) {
            "Contexto adicional entregado por el usuario: \"${state.extraContext}\"."
        } else ""

        return """
            Eres un nutricionista experto y coach de alimentación. Analiza el consumo del usuario
            durante el día de hoy y dale sugerencias prácticas y breves.

            Hora actual: ${now.hour}:${now.minute.toString().padStart(2, '0')}.

            Meta diaria: ${state.goals.calories} kcal (Proteína ${state.goals.proteinGrams}g, Carbohidratos ${state.goals.carbsGrams}g, Grasa ${state.goals.fatGrams}g).
            Consumido hasta ahora: ${state.consumedCalories} kcal (Proteína ${state.consumedProtein}g, Carbohidratos ${state.consumedCarbs}g, Grasa ${state.consumedFat}g).
            Calorías restantes respecto a la meta: $remainingCalories kcal.

            Comidas registradas hoy:
            $mealsText

            $expenditurePart
            $weightPart
            $contextPart

            Instrucciones:
            - Si es temprano o quedan comidas del día por registrar, sugiere qué comer en lo que
              resta del día para acercarse a la meta sin excederla, considerando los macros que
              faltan por cubrir.
            - Si el día ya está prácticamente terminado, haz una evaluación general de cómo estuvo
              el consumo respecto a la meta y el gasto calórico, con aprendizajes para mañana.
            - Explica brevemente el efecto o beneficio de cada sugerencia (ej. por qué esa proteína,
              ese carbohidrato, etc.).
            - Ten en cuenta el contexto adicional que haya entregado el usuario si existe.
            - Responde en español, en un tono cercano y práctico, en 3 a 6 párrafos cortos o una
              lista breve. No repitas los números que ya se te dieron, úsalos como base del análisis.
        """.trimIndent()
    }
}
