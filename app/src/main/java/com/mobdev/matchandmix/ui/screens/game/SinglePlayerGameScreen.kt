package com.mobdev.matchandmix.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.models.SelectedNumber
import com.mobdev.matchandmix.data.models.Tile
import com.mobdev.matchandmix.navigation.Screen
import com.mobdev.matchandmix.ui.screens.game.components.*
import com.mobdev.matchandmix.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun SinglePlayerGameScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameViewModel: GameViewModel = viewModel()

    var gameState by remember { mutableStateOf(GameState.INITIAL) }
    var tiles by remember { mutableStateOf(generateTiles().map {
        it.copy(isRevealed = List(5) { false })
    }) }
    var selectedNumbers by remember { mutableStateOf<List<SelectedNumber>>(emptyList()) }
    var correctPairs by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var chances by remember { mutableIntStateOf(5) }
    var timeLeft by remember { mutableIntStateOf(90) }
    var pairSelectTimeLeft by remember { mutableIntStateOf(15) }
    var correctPairsCounter by remember { mutableIntStateOf(0) }
    var emptyPosition by remember { mutableIntStateOf(8) }
    var isProcessing by remember { mutableStateOf(false) }
    var totalMatchedPairs by remember { mutableIntStateOf(0) }
    var incorrectPair by remember { mutableStateOf<List<SelectedNumber>>(emptyList()) }
    var highlightedPositions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var highlightJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Function to handle tile movement with highlighting
    fun handleTileMovement(fromPosition: Int, toPosition: Int) {
        // Don't allow movement if game is over
        if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) return

        // Cancel any existing highlight job
        highlightJob?.cancel()

        tiles = moveTile(tiles, fromPosition, toPosition)
        emptyPosition = fromPosition

        // Set the highlighted positions
        highlightedPositions = setOf(fromPosition, toPosition)

        // Start new highlight job
        highlightJob = scope.launch {
            delay(2000)
            highlightedPositions = emptySet()
        }
    }

    // Back button confirmation dialog state
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Handle system back button press
    BackHandler {
        if (gameState == GameState.INITIAL) {
            navController.popBackStack(
                route = Screen.Welcome.route,
                inclusive = false
            )
        } else {
            showExitConfirmation = true
        }
    }

    // Back button confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        if (gameState == GameState.PREVIEW) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Yes, Exit")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Game Over dialog state
    var showGameOverDialog by remember { mutableStateOf(false) }

    // Win dialog state
    var showWinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(gameState) {
        when (gameState) {
            GameState.INITIAL -> {
                showGameOverDialog = false
                showWinDialog = false
            }
            GameState.PREVIEW -> {
                while (timeLeft > 0 && gameState == GameState.PREVIEW) {
                    delay(1000)
                    timeLeft--
                    if (timeLeft == 0) {
                        startGame(tiles, gameState) { tiles = it; gameState = GameState.PLAYING }
                    }
                }
            }
            GameState.PLAYING -> {}
            GameState.GAME_OVER -> {
                showGameOverDialog = true
            }
            GameState.WIN -> {
                showWinDialog = true
            }
        }
    }

    LaunchedEffect(gameState, selectedNumbers) {
        if (gameState == GameState.PLAYING) {
            // Reset timer when no numbers are selected
            if (selectedNumbers.isEmpty()) {
                pairSelectTimeLeft = 15
            }

            // Keep counting down while game is playing and not all numbers are selected
            while (gameState == GameState.PLAYING && selectedNumbers.size < 2) {
                if (pairSelectTimeLeft > 0) {
                    delay(1000)
                    pairSelectTimeLeft--
                } else {
                    // Timer ran out
                    chances--
                    if (chances <= 0) {
                        gameState = GameState.GAME_OVER
                    } else {
                        // Reset only unmatched tiles
                        tiles = tiles.map { tile ->
                            val newRevealed = tile.isRevealed.toMutableList()
                            tile.numbers.forEachIndexed { index, _ ->
                                // Only hide numbers that aren't matched
                                if (!tile.isMatched[index]) {
                                    newRevealed[index] = false
                                }
                            }
                            tile.copy(isRevealed = newRevealed)
                        }
                        selectedNumbers = emptyList()
                        pairSelectTimeLeft = 15  // Reset timer back to 15

                        val adjacentPositions = getAdjacentPositions(emptyPosition)
                        val movableTiles = tiles.filter { it.position in adjacentPositions }
                        if (movableTiles.isNotEmpty()) {
                            val tileToMove = movableTiles.random()
                            val oldPosition = tileToMove.position
                            handleTileMovement(oldPosition, emptyPosition)
                        }
                    }
                }
            }
        }
    }
    Box(
            modifier = Modifier
                .fillMaxSize()
            ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.testbackground),
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
                    onClick = {
                        if (gameState == GameState.INITIAL) {
                            navController.popBackStack(
                                route = Screen.Welcome.route,
                                inclusive = false
                            )
                        } else {
                            showExitConfirmation = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to menu",
                        tint = Color(context.getColor(R.color.material_blue))
                    )
                }

                Text(
                    text = stringResource(R.string.game_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color( 0xff2962ff))


                // Placeholder to center the title
                Box(modifier = Modifier.size(48.dp))
            }

            // Game stats
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
                    // First row: Score, Correct, Chances
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Score", score)
                        StatItem("Correct", correctPairs)
                        StatItem(stringResource(R.string.stat_chances), chances)
                    }

                    // Divider
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color( 0xff2979ff),
                        thickness = 1.dp
                    )

                    // Second row: High Score and Timer/Time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            stringResource(R.string.stat_high_score),
                            gameViewModel.highScore
                        )
                        when (gameState) {
                            GameState.INITIAL -> StatItem(stringResource(R.string.stat_time), timeLeft)
                            GameState.PREVIEW -> StatItem(stringResource(R.string.stat_time), timeLeft)
                            GameState.PLAYING -> StatItem(stringResource(R.string.stat_timer), pairSelectTimeLeft)
                            GameState.GAME_OVER -> StatItem(stringResource(R.string.stat_game_over), 0)
                            GameState.WIN -> StatItem(stringResource(R.string.stat_you_won), score)
                        }
                    }
                }
            }

            // Login message if not logged in and in INITIAL state
            if (!gameViewModel.isUserLoggedIn() && gameState == GameState.INITIAL) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Save your scores!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Welcome.route)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                "Login",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game board
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f), // Make the card square
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
                        tiles = tiles,
                        emptyPosition = emptyPosition,
                        onNumberClick = { tile, numberIndex ->
                            if (gameState == GameState.PLAYING &&
                                !tile.isMatched[numberIndex] &&
                                selectedNumbers.size < 2 &&
                                !isProcessing) {

                                val newTiles = tiles.map {
                                    if (it.id == tile.id) {
                                        it.copy(isRevealed = it.isRevealed.toMutableList().apply {
                                            set(numberIndex, true)
                                        })
                                    } else it
                                }
                                tiles = newTiles
                                selectedNumbers = selectedNumbers + SelectedNumber(tile, numberIndex)

                                if (selectedNumbers.size == 2) {
                                    isProcessing = true
                                    scope.launch {
                                        val firstNumber = selectedNumbers[0]
                                        val secondNumber = selectedNumbers[1]

                                        if (firstNumber.tile.numbers[firstNumber.numberIndex] ==
                                            secondNumber.tile.numbers[secondNumber.numberIndex]) {
                                            // Matched pair
                                            correctPairs++
                                            val pointsEarned = ScoreUtils.calculateScore(pairSelectTimeLeft)
                                            score += pointsEarned
                                            correctPairsCounter++
                                            totalMatchedPairs++

                                            val matchedNumber = firstNumber.tile.numbers[firstNumber.numberIndex]
                                            tiles = tiles.map { tile ->
                                                val newMatched = tile.isMatched.toMutableList()
                                                val newRevealed = tile.isRevealed.toMutableList()

                                                tile.numbers.forEachIndexed { index, number ->
                                                    if (number == matchedNumber) {
                                                        newMatched[index] = true
                                                        newRevealed[index] = true
                                                    }
                                                }

                                                tile.copy(
                                                    isMatched = newMatched,
                                                    isRevealed = newRevealed
                                                )
                                            }

                                            delay(100)

                                            if (totalMatchedPairs == 20) {
                                                gameState = GameState.WIN
                                            }
                                        } else {
                                            chances--
                                            // Set the incorrect pair to show red background
                                            incorrectPair = selectedNumbers
                                            delay(1000)
                                            // Reset the incorrect pair
                                            incorrectPair = emptyList()
                                            tiles = tiles.map {
                                                if (it.id == firstNumber.tile.id || it.id == secondNumber.tile.id) {
                                                    it.copy(isRevealed = it.isRevealed.toMutableList().apply {
                                                        if (it.id == firstNumber.tile.id) {
                                                            set(firstNumber.numberIndex, false)
                                                        }
                                                        if (it.id == secondNumber.tile.id) {
                                                            set(secondNumber.numberIndex, false)
                                                        }
                                                    })
                                                } else it
                                            }

                                            if (chances <= 0) {
                                                gameState = GameState.GAME_OVER
                                            } else {
                                                val adjacentPositions = getAdjacentPositions(emptyPosition)
                                                val movableTiles = tiles.filter { it.position in adjacentPositions }
                                                if (movableTiles.isNotEmpty()) {
                                                    val tileToMove = movableTiles.random()
                                                    val oldPosition = tileToMove.position
                                                    handleTileMovement(oldPosition, emptyPosition)
                                                }
                                            }
                                        }
                                        delay(300)
                                        selectedNumbers = emptyList()
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        incorrectPair = incorrectPair,
                        highlightedPositions = highlightedPositions
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Game button (shown in INITIAL state)
            if (gameState == GameState.INITIAL) {
                Button(
                    onClick = {
                        tiles = generateTiles()  // Generate new tiles with visible numbers
                        gameState = GameState.PREVIEW  // Move to preview state
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color( 0xff2962ff)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .width(200.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.button_play_game),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Start Game button (shown in PREVIEW state)
            if (gameState == GameState.PREVIEW) {
                Button(
                    onClick = {
                        startGame(tiles, gameState) { tiles = it; gameState = GameState.PLAYING }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(context.getColor(R.color.material_green))
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .width(200.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.button_start_game),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Game Over dialog
            if (showGameOverDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.dialog_game_over_title)) },
                    text = { Text(stringResource(R.string.dialog_final_score, score)) },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showGameOverDialog = false
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(context.getColor(R.color.material_blue))
                                )
                            ) {
                                Text(stringResource(R.string.button_back_to_main))
                            }
                            Button(
                                onClick = {
                                    showGameOverDialog = false
                                    tiles = generateTiles()
                                    score = 0
                                    chances = 5
                                    timeLeft = 90
                                    selectedNumbers = emptyList()
                                    correctPairsCounter = 0
                                    totalMatchedPairs = 0
                                    emptyPosition = 8
                                    gameState = GameState.PREVIEW
                                }
                            ) {
                                Text(stringResource(R.string.button_play_again))
                            }
                        }
                    }
                )
            }

            // Win dialog
            if (showWinDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_congratulations),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(context.getColor(R.color.material_green)),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.dialog_you_won),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_final_score, score),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.dialog_pairs_matched, totalMatchedPairs),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.dialog_chances_remaining, chances),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showWinDialog = false
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(context.getColor(R.color.material_blue))
                                )
                            ) {
                                Text(stringResource(R.string.button_back_to_main))
                            }
                            Button(
                                onClick = {
                                    showWinDialog = false
                                    tiles = generateTiles()
                                    score = 0
                                    chances = 5
                                    timeLeft = 90
                                    selectedNumbers = emptyList()
                                    correctPairsCounter = 0
                                    totalMatchedPairs = 0
                                    emptyPosition = 8
                                    gameState = GameState.PREVIEW
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(context.getColor(R.color.material_green))
                                )
                            ) {
                                Text(stringResource(R.string.button_play_again))
                            }
                        }
                    }
                )
            }

            // Update high score when game is over (either win or lose)
            LaunchedEffect(gameState) {
                if (gameState == GameState.WIN || gameState == GameState.GAME_OVER) {
                    gameViewModel.updateHighScore(score)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SinglePlayerGameScreenPreview() {
    SinglePlayerGameScreen(navController = NavController(LocalContext.current))
}