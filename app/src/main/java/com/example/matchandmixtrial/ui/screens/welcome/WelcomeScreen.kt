package com.example.matchandmixtrial.ui.screens.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.matchandmixtrial.navigation.Screen

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // This handles both status bar and navigation bar
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
            modifier = Modifier.padding(bottom = 48.dp)
        )

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
            text = "Login/Register",
            onClick = { navController.navigate(Screen.Login.route) }
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