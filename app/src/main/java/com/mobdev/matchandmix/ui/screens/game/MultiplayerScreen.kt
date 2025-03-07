package com.mobdev.matchandmix.ui.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.navigation.Screen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.mobdev.matchandmix.ui.screens.game.components.GameBoard
import com.mobdev.matchandmix.ui.screens.welcome.ImageButtonWithLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun StatColumn(
    label: String,
    value: Int,
    isReady: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xff2962ff),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xff2962ff),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isReady) "Ready!" else "Not Ready",
            style = MaterialTheme.typography.bodySmall,
            color = if (isReady) Color(0xff2e7d32) else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: MultiplayerViewModel = viewModel()
    var showJoinDialog by remember { mutableStateOf(false) }
    var gameCodeInput by remember { mutableStateOf("") }

    // Back button confirmation dialog state
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler(enabled = true) {
        if (viewModel.gameState !is MultiplayerGameState.Initial) {
            showExitConfirmation = true
        } else {
            navController.navigate(Screen.Welcome.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
            }
        }
    }

    // Error dialog state
    if (viewModel.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(viewModel.errorMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Join Game Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Game") },
            text = {
                TextField(
                    value = gameCodeInput,
                    onValueChange = { gameCodeInput = it.uppercase() },
                    label = { Text("Enter Game Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (gameCodeInput.isNotEmpty()) {
                            viewModel.joinGameWithCode(gameCodeInput)
                            showJoinDialog = false
                        }
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                Button(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exit confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit Multiplayer") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        viewModel.exitGame()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Yes, Exit")
                }
            },
            dismissButton = {
                Button(onClick = { showExitConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Opponent left dialog
    if (viewModel.opponentLeft) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Ended") },
            text = { Text("Your opponent has left the game.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exitGame()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Return to Menu")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.multiplayerbg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(top = 25.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showExitConfirmation = true }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to menu",
                        tint = Color(context.getColor(R.color.material_blue))
                    )
                }

                Text(
                    text = "Multiplayer",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraLight,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    color = Color(0xff2962ff)
                )

                // Placeholder to center the title
                Box(modifier = Modifier.size(48.dp))
            }

            // Main content
            when {
                !viewModel.isUserLoggedIn() -> {
                    // Show login prompt
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Please log in to play multiplayer",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Welcome.route)
                                    }
                                }
                            ) {
                                Text("Login")
                            }
                        }
                    }
                }
                viewModel.gameState is MultiplayerGameState.Initial -> {
                    // Show multiplayer options
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Host Game",
                                fontFamily = FontFamily(Font(R.font.ovoregular)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xff2962ff)
                            )
                            ImageButtonWithLabelSolo(
                                defaultImageRes = R.drawable.button_1_idle,
                                clickedImageRes = R.drawable.button_1_clicked,
                                text = "Create Lobby",
                                onClick = { viewModel.createPrivateGame()  }

                            )
                            Divider()
                            Text(
                                text = "Join Game",
                                fontFamily = FontFamily(Font(R.font.ovoregular)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xff2962ff)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ImageButtonWithLabelRow(
                                    defaultImageRes = R.drawable.button_1_idle,
                                    clickedImageRes = R.drawable.button_1_clicked,
                                    text = "Enter Code",
                                    onClick = { showJoinDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                ImageButtonWithLabelRow(
                                    defaultImageRes = R.drawable.button_1_idle,
                                    clickedImageRes = R.drawable.button_1_clicked,
                                    text = "Quick Play",
                                    onClick = { viewModel.joinRandomGame() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                viewModel.gameState is MultiplayerGameState.WaitingForPlayers -> {
                    // Show waiting screen with game code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Waiting for opponent...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xff2962ff)
                            )
                            if (viewModel.isPrivateGame && viewModel.gameCode != null) {
                                Text(
                                    text = "Game Code:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xff2962ff)
                                )
                                Text(
                                    text = viewModel.gameCode!!,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xff2962ff)
                                )
                                Text(
                                    text = "Share this code with your friend",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xff2962ff)
                                )
                            } else {
                                Text(
                                    text = "Looking for players...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            CircularProgressIndicator(
                                color = Color(0xff2962ff)
                            )
                        }
                    }
                }
                viewModel.gameState is MultiplayerGameState.InGame -> {
                    // Show game screen
                    val isHost = (viewModel.gameState as MultiplayerGameState.InGame).isHost

                    // Game stats card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(context.getColor(R.color.white)))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // First row: Player names and scores
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player 1
                                StatColumn(
                                    label = if (isHost) "You (P1)" else "Player 1",
                                    value = if (isHost) viewModel.myScore else viewModel.opponentScore,
                                    isReady = if (isHost) viewModel.amIReady else viewModel.isOpponentReady
                                )

                                // Timer (only show during preview or turn)
                                if (viewModel.isPreviewPhase || viewModel.gameStarted) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (viewModel.isPreviewPhase) "Preview Time" else "Turn Time",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xff2962ff)
                                        )
                                        Text(
                                            text = if (viewModel.isPreviewPhase) 
                                                viewModel.previewTimeLeft.toString() 
                                            else 
                                                viewModel.turnTimeLeft.toString(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if ((viewModel.isPreviewPhase && viewModel.previewTimeLeft <= 10) || 
                                                (!viewModel.isPreviewPhase && viewModel.turnTimeLeft <= 5)) 
                                                Color.Red 
                                            else 
                                                Color(0xff2962ff)
                                        )
                                    }
                                }

                                // Player 2
                                StatColumn(
                                    label = if (!isHost) "You (P2)" else "Player 2",
                                    value = if (!isHost) viewModel.myScore else viewModel.opponentScore,
                                    isReady = if (!isHost) viewModel.amIReady else viewModel.isOpponentReady
                                )
                            }

                            // Divider
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = Color(0xff2979ff),
                                thickness = 1.dp
                            )

                            // Game status and controls
                            if (!viewModel.amIReady) {
                                Button(
                                    onClick = { viewModel.setReady() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xff2e7d32)
                                    )
                                ) {
                                    Text("Ready to Play!")
                                }
                            } else if (!viewModel.isPreviewPhase && !viewModel.isOpponentReady) {
                                Text(
                                    text = "Waiting for opponent to be ready...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xff2962ff),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else if (viewModel.isPreviewPhase) {
                                Text(
                                    text = "Memorize the numbers!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xff2e7d32),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else if (viewModel.isMyTurn) {
                                if (viewModel.isSelectingMove) {
                                    Text(
                                        text = "Select a highlighted tile to move",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xff2962ff),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                } else {
                                    Text(
                                        text = "Your turn - Select matching numbers",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xff2e7d32),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Opponent's turn",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xff2962ff),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Game board card
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(context.getColor(R.color.white)))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GameBoard(
                                tiles = viewModel.tiles,
                                emptyPosition = viewModel.emptyPosition,
                                onNumberClick = { tile, numberIndex ->
                                    if (viewModel.isMyTurn && !viewModel.isPreviewPhase) {
                                        viewModel.onNumberSelected(tile, numberIndex)
                                    }
                                },
                                onTileClick = { position ->
                                    if (viewModel.isSelectingMove && viewModel.isMyTurn) {
                                        viewModel.moveTile(position)
                                    }
                                },
                                incorrectPair = viewModel.incorrectPair,
                                highlightedPositions = if (viewModel.isSelectingMove) viewModel.movableTilePositions else emptySet()
                            )
                        }
                    }

                    // Game instructions
                    if (viewModel.isSelectingMove && viewModel.isMyTurn) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xff2962ff))
                        ) {
                            Text(
                                text = "Select a highlighted tile to move",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    // Start Game button during preview phase
                    if (viewModel.isPreviewPhase) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!viewModel.wantToStartGame) Color(0xff2e7d32) else Color.Gray
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!viewModel.wantToStartGame) {
                                    Button(
                                        onClick = { viewModel.setWantToStartGame() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xff2e7d32)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = "Start Game",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (viewModel.opponentWantsToStartGame)
                                            "Starting game..."
                                        else
                                            "Waiting for opponent to start...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                viewModel.gameState is MultiplayerGameState.GameOver -> {
                    // Show game ended screen
                    val isWinner = (viewModel.gameState as MultiplayerGameState.GameOver).isWinner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isWinner) "You Won!" else "Game Over",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isWinner) Color(0xff2e7d32) else Color(0xff2962ff)
                            )
                            Text(
                                text = if (viewModel.opponentLeft) 
                                    "Your opponent has left the game" 
                                else 
                                    if (isWinner) "Congratulations!" else "Better luck next time!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    viewModel.exitGame()
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            ) {
                                Text("Return to Menu")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ImageButtonWithLabelSolo(
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
            modifier = Modifier
                .height(200.dp)
                .width(200.dp)
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
fun ImageButtonWithLabelRow(
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