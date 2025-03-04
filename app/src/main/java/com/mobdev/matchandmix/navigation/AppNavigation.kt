package com.mobdev.matchandmix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobdev.matchandmix.ui.screens.auth.LoginScreen
import com.mobdev.matchandmix.ui.screens.auth.RegisterScreen
import com.mobdev.matchandmix.ui.screens.game.SinglePlayerGameScreen
import com.mobdev.matchandmix.ui.screens.welcome.WelcomeScreen
import com.mobdev.matchandmix.ui.screens.instructions.InstructionsScreen
import com.mobdev.matchandmix.ui.screens.leaderboard.LeaderboardsScreen

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SinglePlayer : Screen("single_player")
    object Multiplayer : Screen("multiplayer")
    object Leaderboards : Screen("leaderboards")
    object Settings : Screen("settings")
    object Login : Screen("login")
    object Register : Screen("register")
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

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateBack = { 
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onLoginSuccess = { _ ->
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { 
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Multiplayer.route) {
            // TODO: Add multiplayer screen
        }

        composable(Screen.Leaderboards.route) {
            LeaderboardsScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            // TODO: Add settings screen
        }

        composable(Screen.Instructions.route) {
            InstructionsScreen(navController = navController)
        }
    }
}