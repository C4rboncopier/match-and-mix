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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import com.mobdev.matchandmix.ui.screens.welcome.ImageButtonWithLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun SinglePlayerGameScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameViewModel: GameViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf(GameState.INITIAL) }
    var tiles by remember {
        mutableStateOf(generateTiles().map {
            it.copy(isRevealed = List(5) { false })
        })
    }
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
    //Exit Game Dialog
    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onDismiss = { showExitConfirmation = false },
            onConfirm = {
                showExitConfirmation = false
                if (gameState == GameState.PREVIEW) {
                    navController.popBackStack()
                } else {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
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
                Box(modifier = Modifier
                ) {
                    IconButtonLogo(
                        clickedIconRes = R.drawable.backarrow,
                        defaultIconRes = R.drawable.backarrow,
                        onClick = {
                            if (gameState == GameState.INITIAL) {
                                navController.popBackStack(
                                    route = Screen.Welcome.route,
                                    inclusive = false
                                )
                            } else {
                                showExitConfirmation = true
                            }
                        },
                    )
                }

                Text(
                    text = stringResource(R.string.game_title),
                    fontSize = 33.sp,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    fontWeight = FontWeight.ExtraLight,
                    color = Color(0xff2962ff)
                )


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
                        color = Color(0xff2979ff),
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
                            GameState.INITIAL -> StatItem(
                                stringResource(R.string.stat_time),
                                timeLeft
                            )

                            GameState.PREVIEW -> StatItem(
                                stringResource(R.string.stat_time),
                                timeLeft
                            )

                            GameState.PLAYING -> StatItem(
                                stringResource(R.string.stat_timer),
                                pairSelectTimeLeft
                            )

                            GameState.GAME_OVER -> StatItem(
                                stringResource(R.string.stat_game_over),
                                0
                            )

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
                        .height(80.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            fontFamily = FontFamily(Font(R.font.ovoregular)),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .padding(end = 16.dp)
                                .clickable {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Welcome.route)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.button_1_clicked),
                                contentDescription = "Login",
                                modifier = Modifier.size(70.dp)
                            )
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
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
                                    !isProcessing
                                ) {

                                    val newTiles = tiles.map {
                                        if (it.id == tile.id) {
                                            it.copy(
                                                isRevealed = it.isRevealed.toMutableList().apply {
                                                    set(numberIndex, true)
                                                })
                                        } else it
                                    }
                                    tiles = newTiles
                                    selectedNumbers =
                                        selectedNumbers + SelectedNumber(tile, numberIndex)

                                    if (selectedNumbers.size == 2) {
                                        isProcessing = true
                                        scope.launch {
                                            val firstNumber = selectedNumbers[0]
                                            val secondNumber = selectedNumbers[1]

                                            if (firstNumber.tile.numbers[firstNumber.numberIndex] ==
                                                secondNumber.tile.numbers[secondNumber.numberIndex]
                                            ) {
                                                // Matched pair
                                                correctPairs++
                                                val pointsEarned =
                                                    ScoreUtils.calculateScore(pairSelectTimeLeft)
                                                score += pointsEarned
                                                correctPairsCounter++
                                                totalMatchedPairs++

                                                val matchedNumber =
                                                    firstNumber.tile.numbers[firstNumber.numberIndex]
                                                tiles = tiles.map { tile ->
                                                    val newMatched = tile.isMatched.toMutableList()
                                                    val newRevealed =
                                                        tile.isRevealed.toMutableList()

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
                                                        it.copy(
                                                            isRevealed = it.isRevealed.toMutableList()
                                                                .apply {
                                                                    if (it.id == firstNumber.tile.id) {
                                                                        set(
                                                                            firstNumber.numberIndex,
                                                                            false
                                                                        )
                                                                    }
                                                                    if (it.id == secondNumber.tile.id) {
                                                                        set(
                                                                            secondNumber.numberIndex,
                                                                            false
                                                                        )
                                                                    }
                                                                })
                                                    } else it
                                                }

                                                if (chances <= 0) {
                                                    gameState = GameState.GAME_OVER
                                                } else {
                                                    val adjacentPositions =
                                                        getAdjacentPositions(emptyPosition)
                                                    val movableTiles =
                                                        tiles.filter { it.position in adjacentPositions }
                                                    if (movableTiles.isNotEmpty()) {
                                                        val tileToMove = movableTiles.random()
                                                        val oldPosition = tileToMove.position
                                                        handleTileMovement(
                                                            oldPosition,
                                                            emptyPosition
                                                        )
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
                            onTileClick = { position ->
                                if (position in highlightedPositions) {
                                    handleTileMovement(position, emptyPosition)
                                }
                            },
                            incorrectPair = incorrectPair,
                            highlightedPositions = highlightedPositions,
                            isMultiplayer = false
                        )
                    }
                }


                // Play Game button (shown in INITIAL state)
                if (gameState == GameState.INITIAL) {
                    ImageButtonWithLabelSP(
                        defaultImageRes = R.drawable.button_1_idle,
                        clickedImageRes = R.drawable.button_1_clicked,
                        text = "Play Game",

                        onClick = {
                            tiles = generateTiles()  // Generate new tiles with visible numbers
                            coroutineScope.launch{
                                delay(130)
                                gameState = GameState.PREVIEW  // Move to preview state
                            }
                        },
                        modifier = Modifier.scale(0.9f)
                    )
                }
                // Start Game button (shown in PREVIEW state)
                if (gameState == GameState.PREVIEW) {
                    ImageButtonWithLabelSP(
                        defaultImageRes = R.drawable.button_2_idle,
                        clickedImageRes = R.drawable.button_2_click,
                        text = "Start Game",
                        onClick = {
                            coroutineScope.launch {
                                delay(130)
                                startGame(tiles, gameState) {
                                    tiles = it; gameState = GameState.PLAYING
                                }
                            }
                        },
                        modifier = Modifier.scale(0.9f)
                    )
                }


                // Game Over dialog
                if (showGameOverDialog) {
                    GameOverDialog(
                        score = score,
                        onBackToMain = {
                            showGameOverDialog = false
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        },
                        onPlayAgain = {
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
                    )
                }

                // Win dialog
            if (showWinDialog) {
                WinDialog(
                    score = score,
                    totalMatchedPairs = totalMatchedPairs,
                    chances = chances,
                    onBackToMain = {
                        showWinDialog = false
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onPlayAgain = {
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
@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .background(Color(0xFFFFF9C4), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .height(180.dp)
                .width(250.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dialog Title
                Text(
                    text = "Exit Game",
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = "Are you sure you want to exit? Your progress will be lost.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons (Images with Text Overlay)
                Row {
                    // Exit Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clickable { onConfirm() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_2_click),
                            contentDescription = "Exit",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Exit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

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
fun WinDialog(
    score: Int,
    totalMatchedPairs: Int,
    chances: Int,
    onBackToMain: () -> Unit,
    onPlayAgain: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .background(Color(0xFFFFF9C4), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .height(250.dp)
                .width(350.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = stringResource(R.string.dialog_congratulations),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraLight,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    color = Color(0xFF9CCC65),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.dialog_you_won),
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Score Info
                Text(
                    text = stringResource(R.string.dialog_final_score, score),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.dialog_pairs_matched, totalMatchedPairs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.dialog_chances_remaining, chances),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row {
                    // Back to Main Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(135.dp)
                            .clickable { onBackToMain() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_2_click),
                            contentDescription = "Back to Main",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = stringResource(R.string.button_back_to_main),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))


                    // Play Again Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(135.dp)
                            .clickable { onPlayAgain() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_1_clicked),
                            contentDescription = "Play Again",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = stringResource(R.string.button_play_again),
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
fun GameOverDialog(
    score: Int,
    onBackToMain: () -> Unit,
    onPlayAgain: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .background(Color(0xFFFFF9C4), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .height(160.dp)
                .width(270.dp)

        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = stringResource(R.string.dialog_game_over_title),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraLight,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Score Display
                Text(
                    text = stringResource(R.string.dialog_final_score, score),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.ovoregular)),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))


                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Back to Main Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(115.dp)
                            .clickable { onBackToMain() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_2_click),
                            contentDescription = "Back to Main",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = stringResource(R.string.button_back_to_main),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Play Again Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(115.dp)
                            .clickable { onPlayAgain() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.button_1_clicked),
                            contentDescription = "Play Again",
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = stringResource(R.string.button_play_again),
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
fun ImageButtonWithLabelSP(
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
            fontSize = 19.sp,
            fontFamily = FontFamily(Font(R.font.sigmarregular)),
            fontWeight = FontWeight.Light,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SinglePlayerGameScreenPreview() {
    SinglePlayerGameScreen(navController = NavController(LocalContext.current))

}