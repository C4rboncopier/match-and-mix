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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
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
        if (viewModel.gameState !is MultiplayerGameState.Initial && viewModel.gameState !is MultiplayerGameState.GameOver) {
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
            onDismissRequest = { 
                viewModel.clearError()
                gameCodeInput = "" // Clear the input when there's an error
            },
            title = { Text("Error") },
            text = { Text(viewModel.errorMessage!!) },
            confirmButton = {
                Button(onClick = { 
                    viewModel.clearError()
                    gameCodeInput = "" // Clear the input when there's an error
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Join Game Dialog
    if (showJoinDialog) {
        JoinGameDialog(
            gameCodeInput = gameCodeInput,
            onGameCodeChange = { gameCodeInput = it },
            onJoinGame = {
                if (gameCodeInput.isNotEmpty()) {
                    viewModel.joinGameWithCode(gameCodeInput)
                    gameCodeInput = "" // Clear the input
                    showJoinDialog = false
                }
            },
            onCancel = { 
                gameCodeInput = "" // Clear the input when canceling
                showJoinDialog = false 
            }
        )
    }

    // Exit confirmation dialog
    if (showExitConfirmation) {
        ExitConfirmationMP(
            onExit = {
                showExitConfirmation = false
                viewModel.exitGame()
                viewModel.gameState = MultiplayerGameState.Initial
            },
            onCancel = { showExitConfirmation = false }
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
                Box(modifier = Modifier
                ) {
                    IconButtonLogo(
                        clickedIconRes = R.drawable.backarrow,
                        defaultIconRes = R.drawable.backarrow,
                        onClick = { 
                            if (viewModel.gameState !is MultiplayerGameState.Initial && viewModel.gameState !is MultiplayerGameState.GameOver) {
                                showExitConfirmation = true
                            } else {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        },
                    )
                }

                Text(
                    text = "Multiplayer",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Thin,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    color = Color(0xff2962ff)
                )

                // Placeholder to center the title
                Box(modifier = Modifier.size(48.dp))
            }

            // Main content
            when {
                !viewModel.isUserLoggedIn() -> {
                    // Main container with centered title
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Login prompt card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(180.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Please login to your account to access multiplayer",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.padding(8.dp))

                                // Button with image and icon
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Welcome.route)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Transparent)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Image background
                                        Image(
                                            painter = painterResource(id = R.drawable.button_1_idle),
                                            contentDescription = "Button Background",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(1f)

                                        )

                                        // Row inside button (Icon + Text)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Login Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(23.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Login",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
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
                                    label = if (isHost) 
                                        "${viewModel.hostUsername ?: "..."} (You)"
                                    else 
                                        viewModel.hostUsername ?: "...",
                                    value = if (isHost) viewModel.myScore else viewModel.opponentScore,
                                    isReady = if (isHost) viewModel.amIReady else viewModel.isOpponentReady
                                )

                                // Timer (only show during preview or turn)
                                if ((viewModel.isPreviewPhase && viewModel.previewTimeLeft > 0) || 
                                    (viewModel.gameStarted && viewModel.turnTimeLeft >= 0)) {
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
                                                if (viewModel.turnTimeLeft >= 0) viewModel.turnTimeLeft.toString() else "...",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if ((viewModel.isPreviewPhase && viewModel.previewTimeLeft <= 10) ||
                                                (!viewModel.isPreviewPhase && viewModel.turnTimeLeft in 0..5)
                                            )
                                                Color.Red
                                            else
                                                Color(0xff2962ff)
                                        )
                                    }
                                }

                                // Player 2
                                StatColumn(
                                    label = if (!isHost)
                                        "${viewModel.guestUsername ?: "..."} (You)"
                                    else 
                                        viewModel.guestUsername ?: "...",
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
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable { viewModel.setReady() }
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.button_1_clicked),
                                        contentDescription = "Ready to Play!",
                                        modifier = Modifier.size(200.dp, 60.dp),
                                        contentScale = ContentScale.FillBounds
                                    )
                                    Text(
                                        text = "Ready to Play!",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(8.dp),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
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
                                highlightedPositions = if (viewModel.isSelectingMove) viewModel.movableTilePositions else emptySet(),
                                isMultiplayer = true
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

                        if (!viewModel.wantToStartGame) {
                            // Show only the start game button
                            Box(
                                modifier = Modifier
                                    .clickable { viewModel.setWantToStartGame() }
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_1_clicked), // Replace with your actual image
                                    contentDescription = "Start Game",
                                    modifier = Modifier.size(200.dp,80.dp),
                                    contentScale = ContentScale.FillBounds
                                )
                                Text(
                                    text = "Start Game",
                                    modifier = Modifier.align(Alignment.Center),
                                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Reveal card when waiting for opponent
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (viewModel.opponentWantsToStartGame)
                                            "Starting game..."
                                        else
                                            "Waiting for opponent to start...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = FontFamily(Font(R.font.ovoregular)),
                                        fontWeight = FontWeight.Bold,
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
                                text = if (viewModel.opponentLeft) "You Won!" else if (isWinner) "You Won!" else "Game Over",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.opponentLeft || isWinner) Color(0xff2e7d32) else Color(0xff2962ff)
                            )
                            Text(
                                text = if (viewModel.opponentLeft)
                                    "Your opponent has left the game"
                                else
                                    if (isWinner) "Congratulations!" else "Better luck next time!",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily(Font(R.font.ovoregular)),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        viewModel.exitGame()
                                        navController.navigate(Screen.Welcome.route) {
                                            popUpTo(Screen.Welcome.route) { inclusive = true }
                                        }
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_1_clicked),
                                    contentDescription = "Return to Menu",
                                    modifier = Modifier
                                        .size(200.dp, 60.dp),
                                    contentScale = ContentScale.FillBounds
                                )
                                Text(
                                    text = "Return to Menu",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(8.dp),
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
    }
}
@Composable
fun JoinGameDialog(
    gameCodeInput: String,
    onGameCodeChange: (String) -> Unit,
    onJoinGame: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = { onCancel() }) {
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
                .height(175.dp)
                .fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = "Join Lobby",
                    fontSize = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Blue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Field for Game Code
                TextField(
                    value = gameCodeInput,
                    onValueChange = { onGameCodeChange(it.uppercase()) },
                    label = { Text("Enter Game Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cancel Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { onCancel() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_2_click),
                            contentDescription = "Cancel",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Join Game Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { onJoinGame() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_1_clicked),
                            contentDescription = "Join",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Join",
                            fontSize = 16.sp,
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
fun ExitConfirmationMP(
    onExit: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = { onCancel() }) {
        Box(
            modifier = Modifier
                .background(Color(0xFFFFF9C4), shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
                .height(150.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = "Exit Multiplayer",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirmation Text
                Text(
                    text = "Are you sure you want to exit?",
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Exit Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clickable { onExit() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_2_click),
                            contentDescription = "Exit",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Yes, Exit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Cancel Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clickable { onCancel() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_1_clicked),
                            contentDescription = "Cancel",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
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
        modifier = Modifier.size(40.dp)
    ) {
        Image(
            painter = painterResource(id = if (isClicked) clickedIconRes else defaultIconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(25.dp)
        )
    }
}