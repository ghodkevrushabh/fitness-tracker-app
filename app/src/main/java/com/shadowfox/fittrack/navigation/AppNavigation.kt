package com.shadowfox.fittrack.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
// Import all screens
import com.shadowfox.fittrack.screens.auth.LoginScreen
import com.shadowfox.fittrack.screens.auth.SignUpScreen
import com.shadowfox.fittrack.screens.homescreen.HomeScreen
import com.shadowfox.fittrack.screens.runscreen.RunScreen
import com.shadowfox.fittrack.screens.waterscreen.WaterScreen
import com.shadowfox.fittrack.screens.weightscreen.WeightScreen
import com.shadowfox.fittrack.screens.profile.ProfileScreen
import com.shadowfox.fittrack.screens.achievements.AchievementsScreen // IMPORT THIS

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    val application = LocalContext.current.applicationContext as Application
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Define which screens show the bottom bar
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Run.route,
        Screen.Water.route,
        Screen.Weight.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val screens = listOf(
                        Screen.Home,
                        Screen.Run,
                        Screen.Water,
                        Screen.Weight,
                        Screen.Profile
                    )
                    screens.forEach { screen ->
                        NavigationBarItem(
                            label = { Text(text = screen.label) },
                            icon = { Icon(imageVector = screen.icon, contentDescription = null) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // --- AUTHENTICATION SCREENS ---
                composable("login_screen") {
                    LoginScreen(navController, application)
                }
                composable("signup_screen") {
                    SignUpScreen(navController, application)
                }

                // --- MAIN APP SCREENS ---
                composable(Screen.Home.route) {
                    HomeScreen(navController, application)
                }
                composable(Screen.Run.route) {
                    RunScreen(navController, application)
                }
                composable(Screen.Water.route) {
                    WaterScreen(navController, application)
                }
                composable(Screen.Weight.route) {
                    WeightScreen(navController, application)
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(navController, application)
                }

                // FIX: Added Achievements Route
                composable(Screen.Achievements.route) {
                    AchievementsScreen(navController, application)
                }
            }
        }
    }
}