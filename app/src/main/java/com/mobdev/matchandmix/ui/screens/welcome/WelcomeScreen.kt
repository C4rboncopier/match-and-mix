package com.mobdev.matchandmix.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobdev.matchandmix.R
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
    Box(
            modifier = Modifier
                .fillMaxSize()
            ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.screen1),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        'M' to Color(0xFFFFA500), // Bright Orange
                        'A' to Color(0xFF00E5FF), // Electric Cyan
                        'T' to Color(0xFF7CFC00), // Neon Green
                        'C' to Color(0xFFFFD700), // Vivid Gold
                        'H' to Color(0xFF40E0D0), // Turquoise
                        '&' to Color(0xFFFF69B4), // Hot Pink
                        'I' to Color(0xFFFFC0CB), // Light Pink
                        'X' to Color(0xFFFF8C00)  // Deep Saffron
                    )
                    val grayColor = Color.White // Light Gray for empty spaces
                    val borderColor = Color.Gray

                    titleGrid.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { letter ->
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(colorMapping[letter] ?: grayColor)
                                        .border(2.dp, borderColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (letter != null) {
                                        Text(
                                            text = letter.toString(),
                                            fontSize = 28.sp,
                                            fontFamily = FontFamily(Font(R.font.dangrekregular)),
                                            fontWeight = FontWeight.SemiBold,
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
                    fontSize = 30.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily(Font(R.font.dangrekregular)),
                    color = Color(0xff2962ff),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row for Single Player & Multiplayer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageButtonWithLabel(
                        imageRes = R.drawable.button_1,
                        text = "Singleplayer",
                        onClick = { navController.navigate(Screen.SinglePlayer.route) },
                        modifier = Modifier.weight(1f)

                    )

                    ImageButtonWithLabel(
                        imageRes = R.drawable.button_1,
                        text = "Multiplayer",
                        onClick = { navController.navigate(Screen.Multiplayer.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Row for Icons (Instructions, Leaderboards, Logout, Settings)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButtonLogo(
                        iconRes = R.drawable.instructions,
                        onClick = { navController.navigate(Screen.Instructions.route) },
                        modifier = Modifier.weight(1f)
                    )

                    IconButtonLogo(
                        iconRes = R.drawable.leaderboards,
                        onClick = { navController.navigate(Screen.Leaderboards.route) },
                        modifier = Modifier.weight(1f)
                    )

                    IconButtonLogo(
                        iconRes = if (username == null) R.drawable.login else R.drawable.logout,
                        onClick = {
                            if (username == null) {
                                navController.navigate(Screen.Login.route)
                            } else {
                                authViewModel.signOut()
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    IconButtonLogo(
                        iconRes = R.drawable.settings,
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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


@Composable
fun ImageButtonWithLabel(
    imageRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = text,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = text,
            fontSize = 17.sp,
            fontFamily = FontFamily(Font(R.font.sigmarregular)),
            fontWeight = FontWeight.Light,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun IconButtonLogo(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(65.dp)



        )
    }
}
