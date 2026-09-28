package com.staticum.diariocalorico.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DashboardUiState(
    val goals: DailyGoals = DailyGoals(),
    val todayMeals: List<MealWithPhotos> = emptyList(),
    val consumedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFat: Double = 0.0,
    val latestWeightKg: Double? = null,
    val insights: List<DashboardInsight> = emptyList()
)

class DashboardViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val zone = ZoneId.systemDefault()

    init {
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        viewModelScope.launch { trackingRepository.ensureSeedWeight(82.8) }
        viewModelScope.launch { loadInsights() }

        repository.observeMealsBetween(startOfDay, endOfDay)
            .combine(userPreferences.dailyGoals) { meals, goals -> meals to goals }
            .combine(trackingRepository.observeLatestWeight()) { (meals, goals), weight -> Triple(meals, goals, weight) }
            .onEach { (meals, goals, weight) ->
                _uiState.value = _uiState.value.copy(
                    goals = goals,
                    todayMeals = meals,
                    consumedCalories = meals.sumOf { it.meal.calories },
                    consumedProtein = meals.sumOf { it.meal.proteinGrams },
                    consumedCarbs = meals.sumOf { it.meal.carbsGrams },
                    consumedFat = meals.sumOf { it.meal.fatGrams },
                    latestWeightKg = weight?.weightKg
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Calcula patrones (déficit/superávit sostenido, proteína baja sostenida) sobre los últimos
     * días cerrados, comparando números que ya están guardados localmente. No usa Gemini: es una
     * señal temprana en el Dashboard sin depender de que la persona pida un análisis.
     */
    private suspend fun loadInsights() {
        val today = LocalDate.now(zone)
        val days = (1..3).map { offset ->
            val date = today.minusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val meals = repository.getMealsBetween(start, end)
            val expenditure = trackingRepository.getExpenditureForDate(date)
            DayInsightData(
                date = date,
                consumedCalories = meals.sumOf { it.calories },
                consumedProtein = meals.sumOf { it.proteinGrams },
                expenditure = expenditure?.caloriesBurned
            )
        }
        val goals = userPreferences.dailyGoals.first()
        _uiState.value = _uiState.value.copy(insights = DashboardInsights.compute(days, goals))
    }

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch { trackingRepository.saveWeight(weightKg) }
    }

    fun deleteMeal(meal: com.staticum.diariocalorico.data.MealEntry) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }
}
