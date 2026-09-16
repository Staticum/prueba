package com.staticum.diariocalorico.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DashboardUiState(
    val goals: DailyGoals = DailyGoals(),
    val todayMeals: List<MealWithPhotos> = emptyList(),
    val consumedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFat: Double = 0.0
)

class DashboardViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        repository.observeMealsBetween(startOfDay, endOfDay)
            .combine(userPreferences.dailyGoals) { meals, goals -> meals to goals }
            .onEach { (meals, goals) ->
                _uiState.value = DashboardUiState(
                    goals = goals,
                    todayMeals = meals,
                    consumedCalories = meals.sumOf { it.meal.calories },
                    consumedProtein = meals.sumOf { it.meal.proteinGrams },
                    consumedCarbs = meals.sumOf { it.meal.carbsGrams },
                    consumedFat = meals.sumOf { it.meal.fatGrams }
                )
            }
            .launchIn(viewModelScope)
    }

    fun deleteMeal(meal: com.staticum.diariocalorico.data.MealEntry) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }
}
