package com.staticum.diariocalorico.ui.navigation

import java.time.LocalDate

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_MEAL = "add_meal"
    const val ADD_MEAL_FOR_DATE = "add_meal_for_date/{epochDay}"
    const val EDIT_MEAL = "edit_meal/{mealId}"
    const val HISTORY = "history"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val COACH = "coach"
    const val DAY_DETAIL = "day_detail/{epochDay}"

    fun editMeal(mealId: Long) = "edit_meal/$mealId"
    fun addMealForDate(date: LocalDate) = "add_meal_for_date/${date.toEpochDay()}"
    fun dayDetail(date: LocalDate) = "day_detail/${date.toEpochDay()}"
}
