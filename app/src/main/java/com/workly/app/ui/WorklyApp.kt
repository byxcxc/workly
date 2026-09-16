package com.workly.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workly.app.R
import com.workly.app.ui.components.BarChartIcon
import com.workly.app.ui.components.HomeIcon
import com.workly.app.ui.components.ListIcon
import com.workly.app.ui.components.LocalSnackbarHostState
import com.workly.app.ui.components.SettingsIcon
import com.workly.app.ui.navigation.Routes
import com.workly.app.ui.navigation.WorklyNavHost
import com.workly.app.ui.navigation.navigateToTab

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.tab_home, HomeIcon),
    BottomTab(Routes.RECORDS, R.string.tab_records, ListIcon),
    BottomTab(Routes.STATISTICS, R.string.tab_statistics, BarChartIcon),
    BottomTab(Routes.SETTINGS, R.string.tab_settings, SettingsIcon),
)

/**
 * Application shell: one snackbar host, one bottom bar, one navigation graph.
 */
@Composable
fun WorklyApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in Routes.bottomBarRoutes

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            WorklyNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
