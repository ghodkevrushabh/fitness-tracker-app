package com.shadowfox.fittrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person // Using Person for Weight/Profile if specific icons fail
import androidx.compose.material.icons.filled.Star // Using Star for Achievements
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home_screen", "Home", Icons.Default.Home)
    object Run : Screen("run_screen", "Run", Icons.AutoMirrored.Filled.DirectionsRun)
    object Water : Screen("water_screen", "Hydration", Icons.Default.LocalDrink)
    object Weight : Screen("weight_screen", "Weight", Icons.Default.Person)
    object Profile : Screen("profile_screen", "Profile", Icons.Default.AccountCircle)

    // CRITICAL FIX: This was missing, causing the error in HomeScreen
    object Achievements : Screen("achievements_screen", "Badges", Icons.Default.Star)
}