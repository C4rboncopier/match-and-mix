package com.mobdev.matchandmix.ui.screens.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mobdev.matchandmix.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobdev.matchandmix.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = authViewModel.getCurrentUser()
        if (currentUser != null) {
            // Get username from Firestore using the user's UID
            scope.launch {
                username = authViewModel.getUsernameFromFirestore(currentUser.uid)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Game Title
        Text(
            text = "Match & Mix",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = if (username != null) 8.dp else 48.dp)
        )

        // Welcome Message - only show if user is logged in
        if (username != null) {
            Text(
                text = "Welcome back, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // Navigation Buttons
        NavigationButton(
            text = "Single Player",
            onClick = { navController.navigate(Screen.SinglePlayer.route) }
        )

        NavigationButton(
            text = "Multiplayer",
            onClick = { navController.navigate(Screen.Multiplayer.route) }
        )

        NavigationButton(
            text = "Instructions",
            onClick = { navController.navigate(Screen.Instructions.route) }
        )

        NavigationButton(
            text = "Leaderboards",
            onClick = { navController.navigate(Screen.Leaderboards.route) }
        )

        NavigationButton(
            text = if (username == null) "Login/Register" else "Logout",
            onClick = {
                if (username == null) {
                    navController.navigate(Screen.Login.route)
                } else {
                    authViewModel.signOut()
                    username = null
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            }
        )

        NavigationButton(
            text = "Settings",
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

@Composable
private fun NavigationButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}