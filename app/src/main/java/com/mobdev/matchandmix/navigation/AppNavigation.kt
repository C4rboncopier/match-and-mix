package com.mobdev.matchandmix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobdev.matchandmix.ui.screens.game.SinglePlayerGameScreen
import com.mobdev.matchandmix.ui.screens.welcome.WelcomeScreen
import com.mobdev.matchandmix.ui.screens.instructions.InstructionsScreen

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SinglePlayer : Screen("single_player")
    object Multiplayer : Screen("multiplayer")
    object Leaderboards : Screen("leaderboards")
    object Settings : Screen("settings")
    object Login : Screen("login")
    object Instructions : Screen("instructions")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(Screen.SinglePlayer.route) {
            SinglePlayerGameScreen(navController = navController)
        }
        // Other screens will be added later
        composable(Screen.Multiplayer.route) {
            // TODO: Add multiplayer screen
        }
        composable(Screen.Leaderboards.route) {
            // TODO: Add leaderboards screen
        }
        composable(Screen.Settings.route) {
            // TODO: Add settings screen
        }
        composable(Screen.Login.route) {
            // TODO: Add login screen
        }
        composable(Screen.Instructions.route) {
            InstructionsScreen(navController = navController)
        }
    }
}