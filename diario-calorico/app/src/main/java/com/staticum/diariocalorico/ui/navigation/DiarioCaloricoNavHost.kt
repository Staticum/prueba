package com.staticum.diariocalorico.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.staticum.diariocalorico.DiarioCaloricoApplication
import com.staticum.diariocalorico.ui.addmeal.AddMealScreen
import com.staticum.diariocalorico.ui.addmeal.AddMealViewModel
import com.staticum.diariocalorico.ui.coach.CoachScreen
import com.staticum.diariocalorico.ui.coach.CoachViewModel
import com.staticum.diariocalorico.ui.dashboard.DashboardScreen
import com.staticum.diariocalorico.ui.dashboard.DashboardViewModel
import com.staticum.diariocalorico.ui.history.HistoryScreen
import com.staticum.diariocalorico.ui.history.HistoryViewModel
import com.staticum.diariocalorico.ui.reports.ReportsScreen
import com.staticum.diariocalorico.ui.reports.ReportsViewModel
import com.staticum.diariocalorico.ui.settings.SettingsScreen
import com.staticum.diariocalorico.ui.settings.SettingsViewModel
import com.staticum.diariocalorico.util.LambdaViewModelFactory

@Composable
fun DiarioCaloricoNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val app = context.applicationContext as DiarioCaloricoApplication

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = LambdaViewModelFactory {
                DashboardViewModel(app.repository, app.userPreferences, app.trackingRepository)
            })
            DashboardScreen(
                viewModel = vm,
                onAddMeal = { navController.navigate(Routes.ADD_MEAL) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onEditMeal = { mealId -> navController.navigate(Routes.editMeal(mealId)) },
                onOpenCoach = { navController.navigate(Routes.COACH) }
            )
        }
        composable(Routes.COACH) {
            val vm: CoachViewModel = viewModel(factory = LambdaViewModelFactory {
                CoachViewModel(app.repository, app.userPreferences, app.trackingRepository)
            })
            CoachScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADD_MEAL) {
            val vm: AddMealViewModel = viewModel(factory = LambdaViewModelFactory {
                AddMealViewModel(app.repository, app.userPreferences)
            })
            AddMealScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            Routes.EDIT_MEAL,
            arguments = listOf(navArgument("mealId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getLong("mealId") ?: return@composable
            val vm: AddMealViewModel = viewModel(factory = LambdaViewModelFactory {
                AddMealViewModel(app.repository, app.userPreferences)
            })
            androidx.compose.runtime.LaunchedEffect(mealId) { vm.loadForEdit(mealId) }
            AddMealScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(factory = LambdaViewModelFactory { HistoryViewModel(app.repository) })
            HistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditMeal = { mealId -> navController.navigate(Routes.editMeal(mealId)) }
            )
        }
        composable(Routes.REPORTS) {
            val vm: ReportsViewModel = viewModel(factory = LambdaViewModelFactory {
                ReportsViewModel(app.repository, app.userPreferences, app.trackingRepository)
            })
            ReportsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = LambdaViewModelFactory {
                SettingsViewModel(app.userPreferences, app)
            })
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
