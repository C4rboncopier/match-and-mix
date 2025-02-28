package com.mobdev.matchandmix.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobdev.matchandmix.navigation.Screen
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
            scope.launch {
                username = authViewModel.getUsernameFromFirestore(currentUser.uid)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFB2EBF2), Color(0xFF81D4FA), Color((0xFF64FFDA))) // Light Aqua to Soft Teal
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title Box
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val titleGrid = listOf(
                    listOf('M', 'A', 'T', 'C', 'H'),
                    listOf(null, null, '&', null, null),
                    listOf(null, 'M', 'I', 'X', null)
                )
                val colorMapping = mapOf(
                    'M' to Color(0xFFFFD54F), // Light Gold
                    'A' to Color(0xFFFF8A65), // Light Peach
                    'T' to Color(0xFF64FFDA),  // Soft Turquoise
                    'C' to Color(0xFFFFAB91), // Light Coral
                    'H' to Color(0xFFAED581), // Fresh Green
                    '&' to Color(0xFFE6EE9C), // Light Lime
                    'I' to Color(0xFF90CAF9), // Light Sky Blue
                    'X' to Color(0xFFFFCC80)  // Warm Apricot
                )
                val grayColor = Color.White // Light Gray for empty spaces
                val borderColor = Color.Gray

                titleGrid.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colorMapping[letter] ?: grayColor)
                                    .border(2.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (letter != null) {
                                    Text(
                                        text = letter.toString(),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
        Spacer(modifier = Modifier.padding(12.dp))

        // Welcome Message - only show if user is logged in
        if (username != null) {
            Text(
                text = "Welcome back, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
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
