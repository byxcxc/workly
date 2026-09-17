package com.workly.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workly.app.ui.home.HomeScreen
import com.workly.app.ui.records.RecordsScreen
import com.workly.app.ui.records.SessionDetailScreen
import com.workly.app.ui.records.SessionEditorMode
import com.workly.app.ui.records.SessionEditorScreen
import com.workly.app.ui.settings.AboutScreen
import com.workly.app.ui.settings.SettingsScreen
import com.workly.app.ui.settings.WorkTypesScreen
import com.workly.app.ui.statistics.StatisticsScreen
import java.time.LocalDate

/**
 * The navigation graph.
 *
 * Screens only receive lambdas, never the controller itself, so they stay easy to
 * preview and to test.
 */
@Composable
fun WorklyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRecord = { id -> navController.navigate(Routes.sessionDetail(id)) },
                onFinishWork = { id -> navController.navigate(Routes.sessionFinish(id)) },
                onSeeAllRecords = { navController.navigateToTab(Routes.RECORDS) },
                onAddRecord = { navController.navigate(Routes.addRecordOn(null)) },
            )
        }

        composable(Routes.RECORDS) {
            RecordsScreen(
                onOpenRecord = { id -> navController.navigate(Routes.sessionDetail(id)) },
                onAddRecord = { date -> navController.navigate(Routes.addRecordOn(date)) },
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenWorkTypes = { navController.navigate(Routes.WORK_TYPES) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }

        composable(Routes.WORK_TYPES) {
            WorkTypesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.ADD_RECORD,
            arguments = listOf(
                navArgument(Routes.ARG_DATE) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val epochDay = entry.arguments?.getLong(Routes.ARG_DATE) ?: -1L
            SessionEditorScreen(
                mode = SessionEditorMode.CREATE,
                sessionId = null,
                initialDate = if (epochDay >= 0) LocalDate.ofEpochDay(epochDay) else null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SESSION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong(Routes.ARG_SESSION_ID) ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.sessionEdit(id)) },
            )
        }

        composable(
            route = Routes.SESSION_EDIT,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong(Routes.ARG_SESSION_ID) ?: return@composable
            SessionEditorScreen(
                mode = SessionEditorMode.EDIT,
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SESSION_FINISH,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong(Routes.ARG_SESSION_ID) ?: return@composable
            SessionEditorScreen(
                mode = SessionEditorMode.FINISH,
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onSaved = {
                    // Come back to the dashboard after finishing a session.
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}

/** Switches bottom-bar tabs while keeping each tab's own scroll state. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
