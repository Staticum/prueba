package com.staticum.diariocalorico.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReportRange(val label: String, val days: Long) {
    WEEK("7 días", 7), MONTH("30 días", 30)
}

data class DailyCalories(val date: LocalDate, val calories: Int)

data class ReportsUiState(
    val range: ReportRange = ReportRange.WEEK,
    val goals: DailyGoals = DailyGoals(),
    val dailyCalories: List<DailyCalories> = emptyList(),
    val mealsInRange: List<MealEntry> = emptyList()
)

class ReportsViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState

    init {
        loadRange(ReportRange.WEEK)
    }

    fun loadRange(range: ReportRange) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.minusDays(range.days - 1).atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            val meals = repository.getMealsBetween(start, end)
            val goals = userPreferences.dailyGoals.first()

            val grouped = meals.groupBy { it.consumedAt.atZone(zone).toLocalDate() }
            val series = (0 until range.days).map { offset ->
                val date = today.minusDays(range.days - 1 - offset)
                DailyCalories(date, grouped[date]?.sumOf { it.calories } ?: 0)
            }

            _uiState.value = ReportsUiState(range, goals, series, meals)
        }
    }
}
