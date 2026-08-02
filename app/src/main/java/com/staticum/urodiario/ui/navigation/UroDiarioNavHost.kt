package com.staticum.urodiario.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.staticum.urodiario.data.MicturitionRepository
import com.staticum.urodiario.ui.detail.RecordDetailScreen
import com.staticum.urodiario.ui.form.RecordFormScreen
import com.staticum.urodiario.ui.form.RecordFormViewModel
import com.staticum.urodiario.ui.history.HistoryScreen
import com.staticum.urodiario.ui.history.HistoryViewModel
import com.staticum.urodiario.ui.reports.ReportsScreen
import com.staticum.urodiario.ui.reports.ReportsViewModel
import com.staticum.urodiario.util.historyViewModelFactory
import com.staticum.urodiario.util.recordFormViewModelFactory
import com.staticum.urodiario.util.reportsViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun UroDiarioNavHost(repository: MicturitionRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HISTORY) {
        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(factory = historyViewModelFactory(repository))
            HistoryScreen(
                viewModel = vm,
                onAddRecord = { navController.navigate(Routes.recordForm()) },
                onOpenRecord = { id -> navController.navigate(Routes.recordDetail(id)) },
                onOpenReports = { navController.navigate(Routes.REPORTS) }
            )
        }

        composable(
            route = Routes.RECORD_FORM,
            arguments = listOf(navArgument("recordId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId")?.takeIf { it >= 0 }
            val vm: RecordFormViewModel = viewModel(
                key = "form_${recordId ?: "new"}",
                factory = recordFormViewModelFactory(repository, recordId)
            )
            RecordFormScreen(
                viewModel = vm,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RECORD_DETAIL,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            val itemFlow = remember(recordId) { repository.observeById(recordId) }
            val item by itemFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            RecordDetailScreen(
                recordWithPhotosProvider = { item },
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.recordForm(recordId)) },
                onDelete = {
                    item?.let { current -> scope.launch { repository.deleteRecord(current.record) } }
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.REPORTS) {
            val vm: ReportsViewModel = viewModel(factory = reportsViewModelFactory(repository))
            ReportsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
