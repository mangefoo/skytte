package se.mindphaser.skytte.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import se.mindphaser.skytte.R
import se.mindphaser.skytte.ui.ammunition.AmmunitionScreen
import se.mindphaser.skytte.ui.sessions.SessionEditScreen
import se.mindphaser.skytte.ui.sessions.SessionListScreen
import se.mindphaser.skytte.ui.weapons.WeaponsScreen

object Routes {
    const val SESSIONS = "sessions"
    const val WEAPONS = "weapons"
    const val AMMUNITION = "ammunition"
    const val SESSION_EDIT = "session_edit"
    const val SESSION_EDIT_ARG = "sessionId"
    const val SESSION_EDIT_PATTERN = "$SESSION_EDIT?$SESSION_EDIT_ARG={$SESSION_EDIT_ARG}"
    fun sessionEdit(id: Long? = null) =
        if (id == null) SESSION_EDIT else "$SESSION_EDIT?$SESSION_EDIT_ARG=$id"
}

private data class TopTab(val route: String, val labelRes: Int, val icon: @Composable () -> Unit)

@Composable
fun SkytteAppRoot() {
    val nav = rememberNavController()
    val tabs = listOf(
        TopTab(Routes.SESSIONS, R.string.tab_sessions) {
            Icon(Icons.Outlined.GpsFixed, contentDescription = null)
        },
        TopTab(Routes.WEAPONS, R.string.tab_weapons) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null)
        },
        TopTab(Routes.AMMUNITION, R.string.tab_ammunition) {
            Icon(Icons.Filled.Inventory2, contentDescription = null)
        }
    )

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val showBottomBar = tabs.any { tab -> currentDest?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDest?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Routes.SESSIONS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SESSIONS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SESSIONS) {
                SessionListScreen(
                    onAdd = { nav.navigate(Routes.sessionEdit()) },
                    onOpen = { id -> nav.navigate(Routes.sessionEdit(id)) }
                )
            }
            composable(Routes.WEAPONS) { WeaponsScreen() }
            composable(Routes.AMMUNITION) { AmmunitionScreen() }
            composable(
                route = Routes.SESSION_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.SESSION_EDIT_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val raw = entry.arguments?.getString(Routes.SESSION_EDIT_ARG)
                val id = raw?.toLongOrNull()
                SessionEditScreen(
                    sessionId = id,
                    onDone = { nav.popBackStack() }
                )
            }
        }
    }
}
