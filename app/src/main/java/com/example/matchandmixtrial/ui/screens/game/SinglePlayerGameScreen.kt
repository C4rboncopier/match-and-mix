package com.example.matchandmixtrial.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.matchandmixtrial.R
import com.example.matchandmixtrial.data.models.SelectedNumber
import com.example.matchandmixtrial.data.models.Tile
import com.example.matchandmixtrial.navigation.Screen
import com.example.matchandmixtrial.ui.screens.game.components.*
import com.example.matchandmixtrial.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SinglePlayerGameScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gameState by remember { mutableStateOf(GameState.PREVIEW) }
    var tiles by remember { mutableStateOf(generateTiles()) }
    var selectedNumbers by remember { mutableStateOf<List<SelectedNumber>>(emptyList()) }
    var score by remember { mutableIntStateOf(0) }
    var chances by remember { mutableIntStateOf(3) }
    var timeLeft by remember { mutableIntStateOf(60) }
    var pairSelectTimeLeft by remember { mutableIntStateOf(10) }
    var correctPairsCounter by remember { mutableIntStateOf(0) }
    var emptyPosition by remember { mutableIntStateOf(8) }
    var isProcessing by remember { mutableStateOf(false) }
    var totalMatchedPairs by remember { mutableIntStateOf(0) }

    // Back button confirmation dialog state
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Back button confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = { navController.navigate(Screen.Welcome.route) }
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

    LaunchedEffect(gameState) {
        when (gameState) {
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
            GameState.GAME_OVER -> {}
            GameState.WIN -> {}
        }
    }

    LaunchedEffect(gameState, selectedNumbers) {
        if (gameState == GameState.PLAYING) {
            if (selectedNumbers.isEmpty()) {
                pairSelectTimeLeft = 10
            }
            while (pairSelectTimeLeft > 0 && selectedNumbers.size < 2 && gameState == GameState.PLAYING) {
                delay(1000)
                pairSelectTimeLeft--
            }
            if (pairSelectTimeLeft == 0 && selectedNumbers.size < 2) {
                chances--
                if (chances <= 0) {
                    gameState = GameState.GAME_OVER
                } else {
                    tiles = tiles.map { it.copy(isRevealed = List(5) { false }) }
                    selectedNumbers = emptyList()
                    
                    val adjacentPositions = getAdjacentPositions(emptyPosition)
                    val movableTiles = tiles.filter { it.position in adjacentPositions }
                    if (movableTiles.isNotEmpty()) {
                        val tileToMove = movableTiles.random()
                        val oldPosition = tileToMove.position
                        tiles = moveTile(tiles, oldPosition, emptyPosition)
                        emptyPosition = oldPosition
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(context.getColor(R.color.background_light_gray)))
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
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
                text = stringResource(R.string.game_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(context.getColor(R.color.material_blue))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(stringResource(R.string.stat_score), score)
                StatItem(stringResource(R.string.stat_chances), chances)
                when (gameState) {
                    GameState.PREVIEW -> StatItem(stringResource(R.string.stat_time), timeLeft)
                    GameState.PLAYING -> StatItem(stringResource(R.string.stat_timer), pairSelectTimeLeft)
                    GameState.GAME_OVER -> StatItem(stringResource(R.string.stat_game_over), 0)
                    GameState.WIN -> StatItem(stringResource(R.string.stat_you_won), score)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game board
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(context.getColor(R.color.white)))
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
                                    score++
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

                                    if (correctPairsCounter == 5) {
                                        delay(500)
                                        
                                        val adjacentPositions = getAdjacentPositions(emptyPosition)
                                        val movableTiles = tiles.filter { it.position in adjacentPositions }
                                        if (movableTiles.isNotEmpty()) {
                                            val tileToMove = movableTiles.random()
                                            val oldPosition = tileToMove.position
                                            tiles = moveTile(tiles, oldPosition, emptyPosition)
                                            emptyPosition = oldPosition
                                        }
                                        
                                        correctPairsCounter = 0
                                    }

                                    if (totalMatchedPairs == 20) {
                                        gameState = GameState.WIN
                                    }
                                } else {
                                    chances--
                                    delay(1000)
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
                                    
                                    val adjacentPositions = getAdjacentPositions(emptyPosition)
                                    val movableTiles = tiles.filter { it.position in adjacentPositions }
                                    if (movableTiles.isNotEmpty()) {
                                        val tileToMove = movableTiles.random()
                                        val oldPosition = tileToMove.position
                                        tiles = moveTile(tiles, oldPosition, emptyPosition)
                                        emptyPosition = oldPosition
                                    }
                                    
                                    if (chances <= 0) {
                                        gameState = GameState.GAME_OVER
                                    }
                                }
                                delay(300)
                                selectedNumbers = emptyList()
                                isProcessing = false
                            }
                        }
                    }
                }
            )
        }

        // Start Game button
        if (gameState == GameState.PREVIEW) {
            Spacer(modifier = Modifier.height(24.dp))
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
        if (gameState == GameState.GAME_OVER) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.dialog_game_over_title)) },
                text = { Text(stringResource(R.string.dialog_final_score, score)) },
                confirmButton = {
                    Button(onClick = {
                        tiles = generateTiles()
                        score = 0
                        chances = 3
                        timeLeft = 60
                        selectedNumbers = emptyList()
                        correctPairsCounter = 0
                        totalMatchedPairs = 0
                        emptyPosition = 8
                        gameState = GameState.PREVIEW
                    }) {
                        Text(stringResource(R.string.button_play_again))
                    }
                }
            )
        }

        // Win dialog
        if (gameState == GameState.WIN) {
            AlertDialog(
                onDismissRequest = {},
                title = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_congratulations),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(context.getColor(R.color.material_green))
                        )
                        Text(
                            text = stringResource(R.string.dialog_you_won),
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                text = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dialog_final_score, score))
                        Text(stringResource(R.string.dialog_pairs_matched, totalMatchedPairs))
                        Text(stringResource(R.string.dialog_chances_remaining, chances))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            tiles = generateTiles()
                            score = 0
                            chances = 3
                            timeLeft = 60
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
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SinglePlayerGameScreenPreview() {
    SinglePlayerGameScreen(navController = NavController(LocalContext.current))
}