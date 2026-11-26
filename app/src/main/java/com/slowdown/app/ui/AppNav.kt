package com.slowdown.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import com.slowdown.app.ui.common.AppHeader
import com.slowdown.app.ui.common.containerViewModel
import com.slowdown.app.ui.home.HomeViewModel
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slowdown.app.R
import com.slowdown.app.ui.apps.AppsScreen
import com.slowdown.app.ui.common.rememberContainer
import com.slowdown.app.ui.home.HomeScreen
import com.slowdown.app.ui.onboarding.OnboardingScreen
import com.slowdown.app.ui.settings.SettingsScreen
import com.slowdown.app.ui.stats.StatsScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object StatsRoute
@Serializable data object SettingsRoute
@Serializable data object AppsRoute

private class Tab(val route: Any, val label: Int, val icon: ImageVector, val isOn: (androidx.navigation.NavDestination?) -> Boolean)

private val tabs = listOf(
    Tab(HomeRoute, R.string.nav_home, Icons.Filled.Spa) { d -> d?.hasRoute<HomeRoute>() == true },
    Tab(StatsRoute, R.string.nav_stats, Icons.Filled.BarChart) { d -> d?.hasRoute<StatsRoute>() == true },
    Tab(SettingsRoute, R.string.nav_settings, Icons.Filled.Tune) { d -> d?.hasRoute<SettingsRoute>() == true },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val c = rememberContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: run { Box(Modifier.fillMaxSize()); return }
    val nav = rememberNavController()
    val start: Any = if (s.onboardingDone) HomeRoute else OnboardingRoute
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val onTab = tabs.any { it.isOn(dest) }

    val homeUi by containerViewModel { HomeViewModel(it) }.state.collectAsStateWithLifecycle()
    val mode = homeUi.mode

    val host: @Composable (Modifier) -> Unit = { mod ->
        NavHost(nav, startDestination = start, modifier = mod) {
            composable<OnboardingRoute> {
                OnboardingScreen(onFinished = {
                    nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                })
            }
            composable<HomeRoute> { Screen(mode) { HomeScreen(it) } }
            composable<StatsRoute> { Screen(mode) { StatsScreen(it) } }
            composable<SettingsRoute> {
                Screen(mode) { SettingsScreen(onPickApps = { nav.navigate(AppsRoute) }, modifier = it) }
            }
            composable<AppsRoute> { AppsScreen(onBack = { nav.popBackStack() }) }
        }
    }

    if (onTab) {
        val cs = MaterialTheme.colorScheme
        NavigationSuiteScaffold(
            containerColor = cs.background,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = cs.surfaceContainerLow,
                navigationRailContainerColor = cs.surfaceContainerLow,
            ),
            navigationSuiteItems = {
                tabs.forEach { t ->
                    item(
                        selected = t.isOn(dest),
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            },
        ) { host(Modifier) }
    } else {
        host(Modifier)
    }
}

@Composable
private fun Screen(mode: com.slowdown.app.ui.home.HeroMode, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader(mode) },
    ) { pad -> content(Modifier.padding(pad)) }
}
