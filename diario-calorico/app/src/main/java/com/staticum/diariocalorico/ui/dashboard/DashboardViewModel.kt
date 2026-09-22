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
    val latestWeightKg: Double? = null
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

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch { trackingRepository.saveWeight(weightKg) }
    }

    fun deleteMeal(meal: com.staticum.diariocalorico.data.MealEntry) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }
}
