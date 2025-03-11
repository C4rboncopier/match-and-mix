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
    import androidx.compose.ui.text.font.Font
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.window.Dialog
    import androidx.navigation.NavController
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.mobdev.matchandmix.R
    import com.mobdev.matchandmix.navigation.Screen
    import com.mobdev.matchandmix.ui.screens.auth.AuthViewModel
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch

    @Composable
    fun WelcomeScreen(
        navController: NavController,
        authViewModel: AuthViewModel = viewModel()
    ) {
        val scope = rememberCoroutineScope()
        var logoutDialog by remember { mutableStateOf(false)}
        val username by authViewModel.currentUsername.collectAsState()

        // This effect will run once when the screen is created
        LaunchedEffect(Unit) {
            authViewModel.checkAndUpdateCurrentUser()
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
                    Image(
                        painter = painterResource(id = R.drawable.m_mlogo),
                        contentDescription = "Background",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(320.dp)
                    )
                }

                // Welcome Message - only show if user is logged in
                if (!username.isNullOrEmpty()) {
                    Text(
                        text = "Welcome back, \n $username!",
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
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
                            defaultImageRes = R.drawable.button_1_idle,
                            clickedImageRes = R.drawable.button_1_clicked,
                            text = "Singleplayer",
                            onClick = { navController.navigate(Screen.SinglePlayer.route) },
                            modifier = Modifier.weight(1f)
                        )

                        ImageButtonWithLabel(
                            defaultImageRes = R.drawable.button_1_idle,
                            clickedImageRes = R.drawable.button_1_clicked,
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
                            clickedIconRes = R.drawable.instructions_clicked,
                            defaultIconRes = R.drawable.instructions_idle,
                            onClick = { navController.navigate(Screen.Instructions.route) },
                            modifier = Modifier.weight(1f)
                        )

                        IconButtonLogo(
                            clickedIconRes = R.drawable.leaderboards_clicked,
                            defaultIconRes = R.drawable.leaderboard_idle,
                            onClick = { navController.navigate(Screen.Leaderboards.route) },
                            modifier = Modifier.weight(1f)
                        )
                        var showLogoutDialog by remember { mutableStateOf(false) }

                        IconButtonLogo(
                            defaultIconRes = if (username.isNullOrEmpty()) R.drawable.login_idle else R.drawable.logout_idle,
                            clickedIconRes = if (username.isNullOrEmpty()) R.drawable.login_clicked else R.drawable.logout_clicked,
                            onClick = {
                                if (username.isNullOrEmpty()) {
                                    navController.navigate(Screen.Login.route)
                                } else {
                                    showLogoutDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (showLogoutDialog) {
                            LogoutDialog(
                                onDismiss = { showLogoutDialog = false },
                                onConfirm = {
                                    showLogoutDialog = false
                                    authViewModel.signOut()
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        IconButtonLogo(
                            clickedIconRes = R.drawable.settings_clicked,
                            defaultIconRes = R.drawable.settings_idle,
                            onClick = { navController.navigate(Screen.Credits.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    @Composable
    fun LogoutDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF9C4), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .height(160.dp)
                    .width(250.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Dialog Title
                    Text(
                        text = "Logout Confirmation",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message
                    Text(
                        text = "Are you sure you want to logout?",
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontFamily = FontFamily(Font(R.font.ovoregular)),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Buttons (Images with Text Overlay)
                    Row {
                        // Logout Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .clickable { onConfirm() }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.button_2_click),
                                contentDescription = "Logout",
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "Logout",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Cancel Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .clickable { onDismiss() }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.button_1_clicked),
                                contentDescription = "Cancel",
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "Cancel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
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
        defaultImageRes: Int,
        clickedImageRes: Int,
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var isClicked by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    isClicked = true
                    onClick()
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(100) // Reset image after 100ms
                        isClicked = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = if (isClicked) clickedImageRes else defaultImageRes),
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
        defaultIconRes: Int,
        clickedIconRes: Int,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var isClicked by remember { mutableStateOf(false) }

        IconButton(
            onClick = {
                isClicked = true
                onClick()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100) // Reset image after 100ms
                    isClicked = false
                }
            },
            modifier = Modifier.size(68.dp)
        ) {
            Image(
                painter = painterResource(id = if (isClicked) clickedIconRes else defaultIconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(55.dp)
            )
        }
    }
