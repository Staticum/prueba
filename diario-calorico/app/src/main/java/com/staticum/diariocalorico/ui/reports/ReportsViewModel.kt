package com.staticum.diariocalorico.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.DailyGoals
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealType
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class ChartMode { CALORIES, MACROS, DEFICIT }

data class DayPoint(val date: LocalDate, val value: Int)
data class DayMacros(val date: LocalDate, val proteinGrams: Double, val carbsGrams: Double, val fatGrams: Double)
data class DayWeight(val date: LocalDate, val weightKg: Double?)

data class ReportKpis(
    val avgCalories: Int,
    val daysOnTrack: Int,
    val daysOffTrack: Int,
    val dominantMacroLabel: String,
    val streakDays: Int,
    val avgVsPreviousPeriod: Int? // diferencia de promedio diario vs. período anterior equivalente
)

data class ReportsUiState(
    val startDate: LocalDate = LocalDate.now().minusDays(6),
    val endDate: LocalDate = LocalDate.now(),
    val goals: DailyGoals = DailyGoals(),
    val dailyCalories: List<DayPoint> = emptyList(),
    val dailyExpenditure: List<DayPoint> = emptyList(),
    val dailyWeight: List<DayWeight> = emptyList(),
    val dailyMacros: List<DayMacros> = emptyList(),
    val mealsInRange: List<MealEntry> = emptyList(),
    val mealTypeBreakdown: List<Pair<MealType, Int>> = emptyList(),
    val kpis: ReportKpis = ReportKpis(0, 0, 0, "-", 0, null),
    val chartMode: ChartMode = ChartMode.CALORIES,
    val mealTypeFilter: MealType? = null,
    val foodSearchQuery: String = "",
    val filteredMeals: List<MealEntry> = emptyList(),
    val filteredTotalCalories: Int = 0
)

class ReportsViewModel(
    private val repository: MealRepository,
    private val userPreferences: UserPreferences,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState

    private val zone = ZoneId.systemDefault()

    init {
        loadRange(LocalDate.now().minusDays(6), LocalDate.now())
    }

    fun setChartMode(mode: ChartMode) {
        _uiState.value = _uiState.value.copy(chartMode = mode)
    }

    fun setMealTypeFilter(type: MealType?) {
        _uiState.value = _uiState.value.copy(mealTypeFilter = type)
        applyFilters()
    }

    fun setFoodSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(foodSearchQuery = query)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.mealsInRange.filter { meal ->
            (state.mealTypeFilter == null || meal.mealType == state.mealTypeFilter) &&
                (state.foodSearchQuery.isBlank() || meal.detectedFoods.contains(state.foodSearchQuery, ignoreCase = true))
        }
        _uiState.value = state.copy(filteredMeals = filtered, filteredTotalCalories = filtered.sumOf { it.calories })
    }

    fun loadRange(start: LocalDate, end: LocalDate) {
        if (start.isAfter(end)) return
        viewModelScope.launch {
            val startInstant = start.atStartOfDay(zone).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()

            val meals = repository.getMealsBetween(startInstant, endInstant)
            val goals = userPreferences.dailyGoals.first()
            val expenditureEntries = trackingRepository.getExpenditureBetween(start, end)
            val weightEntries = trackingRepository.getWeightsBetween(startInstant, endInstant)

            val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
            val mealsByDate = meals.groupBy { it.consumedAt.atZone(zone).toLocalDate() }
            val expenditureByDate = expenditureEntries.associateBy { it.date }
            val weightByDate = weightEntries.groupBy { it.recordedAt.atZone(zone).toLocalDate() }
                .mapValues { (_, entries) -> entries.maxByOrNull { it.recordedAt }?.weightKg }

            val dailyCalories = days.map { date -> DayPoint(date, mealsByDate[date]?.sumOf { it.calories } ?: 0) }
            val dailyExpenditure = days.map { date -> DayPoint(date, expenditureByDate[date]?.caloriesBurned ?: 0) }
            val dailyWeight = days.map { date -> DayWeight(date, weightByDate[date]) }
            val dailyMacros = days.map { date ->
                val dayMeals = mealsByDate[date].orEmpty()
                DayMacros(
                    date = date,
                    proteinGrams = dayMeals.sumOf { it.proteinGrams },
                    carbsGrams = dayMeals.sumOf { it.carbsGrams },
                    fatGrams = dayMeals.sumOf { it.fatGrams }
                )
            }

            val mealTypeBreakdown = meals.groupBy { it.mealType }
                .map { (type, list) -> type to list.sumOf { it.calories } }
                .sortedByDescending { it.second }

            val kpis = computeKpis(days, dailyCalories, dailyMacros, goals, start, end)

            _uiState.value = _uiState.value.copy(
                startDate = start,
                endDate = end,
                goals = goals,
                dailyCalories = dailyCalories,
                dailyExpenditure = dailyExpenditure,
                dailyWeight = dailyWeight,
                dailyMacros = dailyMacros,
                mealsInRange = meals,
                mealTypeBreakdown = mealTypeBreakdown,
                kpis = kpis,
                mealTypeFilter = null,
                foodSearchQuery = ""
            )
            applyFilters()
        }
    }

    private suspend fun computeKpis(
        days: List<LocalDate>,
        dailyCalories: List<DayPoint>,
        dailyMacros: List<DayMacros>,
        goals: DailyGoals,
        start: LocalDate,
        end: LocalDate
    ): ReportKpis {
        val avgCalories = if (dailyCalories.isNotEmpty()) dailyCalories.sumOf { it.value } / dailyCalories.size else 0

        val daysWithMeals = dailyCalories.filter { it.value > 0 }
        val onTrackThreshold = goals.calories * 1.05
        val daysOnTrack = daysWithMeals.count { it.value <= onTrackThreshold }
        val daysOffTrack = daysWithMeals.size - daysOnTrack

        val totalProteinCals = dailyMacros.sumOf { it.proteinGrams } * 4
        val totalCarbsCals = dailyMacros.sumOf { it.carbsGrams } * 4
        val totalFatCals = dailyMacros.sumOf { it.fatGrams } * 9
        val totalMacroCals = totalProteinCals + totalCarbsCals + totalFatCals
        val dominantMacroLabel = if (totalMacroCals > 0) {
            val entries = listOf("Proteína" to totalProteinCals, "Carbohidratos" to totalCarbsCals, "Grasa" to totalFatCals)
            val top = entries.maxByOrNull { it.second }!!
            "${top.first} (${(top.second / totalMacroCals * 100).toInt()}%)"
        } else "-"

        var streak = 0
        for (day in dailyCalories.sortedByDescending { it.date }) {
            if (day.value in 1..(onTrackThreshold.toInt())) streak++ else break
        }

        val periodLengthDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
        val prevStart = start.minusDays(periodLengthDays)
        val prevEnd = start.minusDays(1)
        val prevMeals = repository.getMealsBetween(
            prevStart.atStartOfDay(zone).toInstant(),
            prevEnd.plusDays(1).atStartOfDay(zone).toInstant()
        )
        val avgVsPreviousPeriod = if (prevMeals.isNotEmpty()) {
            val prevAvg = prevMeals.groupBy { it.consumedAt.atZone(zone).toLocalDate() }
                .values.sumOf { it.sumOf { m -> m.calories } } / periodLengthDays.toInt()
            avgCalories - prevAvg
        } else null

        return ReportKpis(
            avgCalories = avgCalories,
            daysOnTrack = daysOnTrack,
            daysOffTrack = daysOffTrack,
            dominantMacroLabel = dominantMacroLabel,
            streakDays = streak,
            avgVsPreviousPeriod = avgVsPreviousPeriod
        )
    }
}
