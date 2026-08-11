package com.staticum.mientreno.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.ui.history.HistoryScreen
import com.staticum.mientreno.ui.history.HistoryViewModel
import com.staticum.mientreno.ui.history.SessionDetailScreen
import com.staticum.mientreno.ui.history.SessionDetailViewModel
import com.staticum.mientreno.ui.library.ExerciseLibraryScreen
import com.staticum.mientreno.ui.library.ExerciseLibraryViewModel
import com.staticum.mientreno.ui.progress.ProgressScreen
import com.staticum.mientreno.ui.progress.ProgressViewModel
import com.staticum.mientreno.ui.quicklog.QuickLogScreen
import com.staticum.mientreno.ui.quicklog.QuickLogViewModel
import com.staticum.mientreno.ui.routines.RoutineEditorScreen
import com.staticum.mientreno.ui.routines.RoutineEditorViewModel
import com.staticum.mientreno.ui.routines.RoutineListScreen
import com.staticum.mientreno.ui.routines.RoutineListViewModel
import com.staticum.mientreno.ui.settings.SettingsScreen
import com.staticum.mientreno.ui.settings.SettingsViewModel
import com.staticum.mientreno.ui.workout.RoutineExecutionScreen
import com.staticum.mientreno.ui.workout.RoutineExecutionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.staticum.mientreno.util.historyViewModelFactory
import com.staticum.mientreno.util.libraryViewModelFactory
import com.staticum.mientreno.util.progressViewModelFactory
import com.staticum.mientreno.util.quickLogViewModelFactory
import com.staticum.mientreno.util.routineEditorViewModelFactory
import com.staticum.mientreno.util.routineExecutionViewModelFactory
import com.staticum.mientreno.util.routineListViewModelFactory
import com.staticum.mientreno.util.sessionDetailViewModelFactory
import com.staticum.mientreno.util.settingsViewModelFactory

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.ROUTINES, "Rutinas", Icons.Filled.FitnessCenter),
    BottomDestination(Routes.LIBRARY, "Biblioteca", Icons.Filled.List),
    BottomDestination(Routes.HISTORY, "Historial", Icons.Filled.History),
    BottomDestination(Routes.PROGRESS, "Progreso", Icons.Filled.ShowChart),
    BottomDestination(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun MiEntrenoNavHost(repository: FitnessRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val isTopLevel = bottomDestinations.any { dest -> currentRoute?.hierarchy?.any { it.route == dest.route } == true }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ROUTINES,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ROUTINES) {
                val vm: RoutineListViewModel = viewModel(factory = routineListViewModelFactory(repository))
                RoutineListScreen(
                    viewModel = vm,
                    onCreateRoutine = { navController.navigate(Routes.routineEditor()) },
                    onEditRoutine = { id -> navController.navigate(Routes.routineEditor(id)) },
                    onExecuteRoutine = { id -> navController.navigate(Routes.routineExecution(id)) }
                )
            }

            composable(Routes.LIBRARY) {
                val vm: ExerciseLibraryViewModel = viewModel(factory = libraryViewModelFactory(repository))
                ExerciseLibraryScreen(viewModel = vm)
            }

            composable(Routes.HISTORY) {
                val vm: HistoryViewModel = viewModel(factory = historyViewModelFactory(repository))
                HistoryScreen(
                    viewModel = vm,
                    onAddQuickLog = { navController.navigate(Routes.QUICK_LOG) },
                    onOpenSession = { id -> navController.navigate(Routes.sessionDetail(id)) }
                )
            }

            composable(Routes.PROGRESS) {
                val vm: ProgressViewModel = viewModel(factory = progressViewModelFactory(repository))
                ProgressScreen(viewModel = vm)
            }

            composable(Routes.SETTINGS) {
                val appContext = LocalContext.current.applicationContext
                val vm: SettingsViewModel = viewModel(factory = settingsViewModelFactory(appContext))
                SettingsScreen(viewModel = vm)
            }

            composable(
                route = Routes.ROUTINE_EDITOR,
                arguments = listOf(navArgument("routineId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStack ->
                val routineId = backStack.arguments?.getLong("routineId")?.takeIf { it >= 0 }
                val vm: RoutineEditorViewModel = viewModel(
                    key = "routine_editor_${routineId ?: "new"}",
                    factory = routineEditorViewModelFactory(repository, routineId)
                )
                RoutineEditorScreen(
                    viewModel = vm,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ROUTINE_EXECUTION,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { backStack ->
                val routineId = backStack.arguments?.getLong("routineId") ?: -1L
                val vm: RoutineExecutionViewModel = viewModel(
                    key = "routine_execution_$routineId",
                    factory = routineExecutionViewModelFactory(repository, routineId)
                )
                RoutineExecutionScreen(
                    viewModel = vm,
                    onFinished = { navController.popBackStack() }
                )
            }

            composable(Routes.QUICK_LOG) {
                val vm: QuickLogViewModel = viewModel(factory = quickLogViewModelFactory(repository))
                QuickLogScreen(
                    viewModel = vm,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.SESSION_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: -1L
                val vm: SessionDetailViewModel = viewModel(
                    key = "session_detail_$sessionId",
                    factory = sessionDetailViewModelFactory(repository, sessionId)
                )
                SessionDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
