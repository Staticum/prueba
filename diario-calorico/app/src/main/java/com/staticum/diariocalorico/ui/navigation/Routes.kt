package com.staticum.diariocalorico.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_MEAL = "add_meal"
    const val EDIT_MEAL = "edit_meal/{mealId}"
    const val HISTORY = "history"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    fun editMeal(mealId: Long) = "edit_meal/$mealId"
}
