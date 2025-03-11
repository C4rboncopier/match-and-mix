package com.mobdev.matchandmix.ui.screens.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mobdev.matchandmix.data.auth.FirebaseAuthRepository
import com.mobdev.matchandmix.data.models.Tile
import com.mobdev.matchandmix.data.models.SelectedNumber
import com.mobdev.matchandmix.utils.generateTiles
import com.mobdev.matchandmix.utils.getAdjacentPositions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class MultiplayerGameState {
    object Initial : MultiplayerGameState()
    object WaitingForPlayers : MultiplayerGameState()
    data class InGame(val isHost: Boolean) : MultiplayerGameState()
    data class GameOver(val isWinner: Boolean) : MultiplayerGameState()
}

class MultiplayerViewModel : ViewModel() {
    private val auth = FirebaseAuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private var gameSessionListener: ListenerRegistration? = null
    private var gameBoardListener: ListenerRegistration? = null

    var gameState by mutableStateOf<MultiplayerGameState>(MultiplayerGameState.Initial)
        private set

    var gameCode by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var opponentLeft by mutableStateOf(false)
        private set

    var isPrivateGame by mutableStateOf(false)
        private set

    // Game board state
    var tiles by mutableStateOf<List<Tile>>(emptyList())
        private set
    
    var emptyPosition by mutableStateOf(8)
        private set

    var selectedNumbers by mutableStateOf<List<SelectedNumber>>(emptyList())
        private set

    var incorrectPair by mutableStateOf<List<SelectedNumber>>(emptyList())
        private set

    var isMyTurn by mutableStateOf(false)
        private set

    var myScore by mutableStateOf(0)
        private set

    var opponentScore by mutableStateOf(0)
        private set

    var movableTilePositions by mutableStateOf<Set<Int>>(emptySet())
        private set

    var isSelectingMove by mutableStateOf(false)
        private set

    // Ready state and timers
    var amIReady by mutableStateOf(false)
        private set

    var isOpponentReady by mutableStateOf(false)
        private set

    var isPreviewPhase by mutableStateOf(false)
        private set

    var previewTimeLeft by mutableStateOf(90)
        private set

    var turnTimeLeft by mutableStateOf(15)
        private set

    var wantToStartGame by mutableStateOf(false)
        private set

    var opponentWantsToStartGame by mutableStateOf(false)
        private set

    var gameStarted by mutableStateOf(false)
        private set

    private var previewTimerJob: kotlinx.coroutines.Job? = null
    private var turnTimerJob: kotlinx.coroutines.Job? = null

    // Add a new field to track the number of revealed numbers in the current turn
    private var revealedNumbersCount by mutableStateOf(0)
    
    // Add a field to track the last timer sync timestamp from Firebase
    private var lastTimerSyncTimestamp: Long = 0
    
    // Add a field to store the server-synchronized turn start time
    private var turnStartTimestamp by mutableStateOf<Long?>(null)

    // Add a variable to track if we're currently updating the timer
    private var isUpdatingTimer = false

    // Add a buffer variable to prevent visual glitches during timer updates
    private var isTimerTransitioning = false

    override fun onCleared() {
        super.onCleared()
        previewTimerJob?.cancel()
        turnTimerJob?.cancel()
        viewModelScope.launch {
            cleanupGameSession()
        }
        gameSessionListener?.remove()
        gameBoardListener?.remove()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.getCurrentUser() != null
    }

    private fun initializeGameBoard() {
        if (gameState is MultiplayerGameState.InGame && (gameState as MultiplayerGameState.InGame).isHost) {
            val initialTiles = generateTiles().map { tile ->
                tile.copy(isRevealed = List(5) { false }) // Start with all numbers hidden
            }
            tiles = initialTiles
            saveBoardToFirebase(initialTiles)
        }
    }

    private fun saveBoardToFirebase(tiles: List<Tile>) {
        viewModelScope.launch {
            try {
                val boardData = tiles.map { tile ->
                    mapOf(
                        "id" to tile.id.toString(),
                        "position" to tile.position,
                        "numbers" to tile.numbers,
                        "isRevealed" to tile.isRevealed,
                        "isMatched" to tile.isMatched
                    )
                }

                gameCode?.let { code ->
                    firestore.collection("game_sessions")
                        .document(code)
                        .update(
                            mapOf(
                                "board" to boardData,
                                "emptyPosition" to emptyPosition,
                                "currentTurn" to auth.getCurrentUser()?.uid,
                                "incorrectPair" to incorrectPair.map { selectedNumber ->
                                    mapOf(
                                        "tileId" to selectedNumber.tile.id.toString(),
                                        "numberIndex" to selectedNumber.numberIndex
                                    )
                                }
                            )
                        )
                        .await()
                }
            } catch (e: Exception) {
                errorMessage = "Failed to save board: ${e.message}"
            }
        }
    }

    private fun startPreviewTimer() {
        previewTimerJob?.cancel()
        previewTimerJob = viewModelScope.launch {
            previewTimeLeft = 90 // Reset timer to 90 seconds
            while (previewTimeLeft > 0) {
                delay(1000)
                previewTimeLeft--
                
                if (previewTimeLeft == 0) {
                    startActualGame()
                }
            }
        }
    }

    private fun startTurnTimer() {
        // Prevent multiple timer starts during transitions
        if (isTimerTransitioning) return
        
        // Set transitioning flag
        isTimerTransitioning = true
        
        // Cancel any existing timer job first
        turnTimerJob?.cancel()
        turnTimerJob = null
        
        // Don't start timer if player is selecting a move
        if (isSelectingMove) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    turnTimeLeft = 15
                    isTimerTransitioning = false
                }
            }
            return
        }
        
        viewModelScope.launch {
            try {
                // Set initial state
                withContext(Dispatchers.Main) {
                    turnTimeLeft = 15
                }
                
                gameCode?.let { code ->
                    val startTimestamp = System.currentTimeMillis()
                    
                    // Update Firebase with start timestamp only
                    firestore.collection("game_sessions")
                        .document(code)
                        .update(mapOf(
                            "turnStartTimestamp" to startTimestamp,
                            "isSelectingMove" to false
                        ))
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        turnStartTimestamp = startTimestamp
                        isTimerTransitioning = false
                        
                        // Start local timer based on timestamp
                        turnTimerJob = viewModelScope.launch {
                            while (isMyTurn && !isSelectingMove && !isPreviewPhase) {
                                val currentTime = System.currentTimeMillis()
                                val elapsedSeconds = ((currentTime - startTimestamp) / 1000).toInt()
                                val newTimeLeft = maxOf(15 - elapsedSeconds, 0)
                                
                                withContext(Dispatchers.Main) {
                                    if (!isTimerTransitioning && turnTimeLeft != newTimeLeft) {
                                        turnTimeLeft = newTimeLeft
                                    }
                                }
                                
                                if (newTimeLeft == 0 && isMyTurn && !isSelectingMove && gameStarted && !isPreviewPhase) {
                                    handleTimerExpired()
                                    break
                                }
                                
                                delay(100) // Update frequently for smooth countdown
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to start turn timer: ${e.message}"
                isTimerTransitioning = false
                
                // Fallback to local timer if Firebase fails
                withContext(Dispatchers.Main) {
                    turnTimeLeft = 15
                    val startTimestamp = System.currentTimeMillis()
                    
                    turnTimerJob = viewModelScope.launch {
                        while (isMyTurn && !isSelectingMove && !isPreviewPhase) {
                            val currentTime = System.currentTimeMillis()
                            val elapsedSeconds = ((currentTime - startTimestamp) / 1000).toInt()
                            val newTimeLeft = maxOf(15 - elapsedSeconds, 0)
                            
                            withContext(Dispatchers.Main) {
                                if (!isTimerTransitioning && turnTimeLeft != newTimeLeft) {
                                    turnTimeLeft = newTimeLeft
                                }
                            }
                            
                            if (newTimeLeft == 0 && isMyTurn && !isSelectingMove && gameStarted && !isPreviewPhase) {
                                handleTimerExpired()
                                break
                            }
                            
                            delay(100)
                        }
                    }
                }
            }
        }
    }

    private fun updateTimerInFirebase(timeLeft: Int) {
        // No longer needed - we're using timestamp-based timing
    }

    private fun handleTimerExpired() {
        // Use a flag to prevent multiple simultaneous calls
        if (isSelectingMove || !isMyTurn || isTimerTransitioning) return
        
        // Set transitioning flag
        isTimerTransitioning = true
        
        viewModelScope.launch {
            try {
                // Immediately stop the timer and calculate new state
                turnTimerJob?.cancel()
                turnTimerJob = null
                val newMovableTilePositions = getAdjacentPositions(emptyPosition).toSet()
                
                // Update Firebase first in a single atomic transaction
                gameCode?.let { code ->
                    val currentTimestamp = System.currentTimeMillis()
                    
                    firestore.runTransaction { transaction ->
                        val docRef = firestore.collection("game_sessions").document(code)
                        transaction.update(docRef, mapOf(
                            "turnStartTimestamp" to currentTimestamp,
                            "isSelectingMove" to true,
                            "incorrectPair" to emptyList<Map<String, Any>>()
                        ))
                    }.await()
                    
                    // After Firebase update is confirmed, update all local state at once
                    withContext(Dispatchers.Main) {
                        isSelectingMove = true
                        selectedNumbers = emptyList()
                        revealedNumbersCount = 0
                        turnTimeLeft = 15
                        movableTilePositions = newMovableTilePositions
                        isTimerTransitioning = false
                    }
                }
            } catch (e: Exception) {
                // If Firebase update fails, still ensure the game is playable
                withContext(Dispatchers.Main) {
                    isSelectingMove = true
                    selectedNumbers = emptyList()
                    revealedNumbersCount = 0
                    turnTimeLeft = 15
                    movableTilePositions = getAdjacentPositions(emptyPosition).toSet()
                    isTimerTransitioning = false
                }
            }
        }
    }

    private fun startActualGame() {
        viewModelScope.launch {
            try {
                // Cancel any existing timers first
                previewTimerJob?.cancel()
                previewTimerJob = null
                turnTimerJob?.cancel()
                turnTimerJob = null
                
                // Hide all numbers while preserving matched states
                val updatedTiles = tiles.map { tile ->
                    val newRevealed = tile.isRevealed.mapIndexed { index, _ ->
                        tile.isMatched[index] // Keep revealed if matched, hide otherwise
                    }
                    tile.copy(isRevealed = newRevealed)
                }
                
                // Update local state immediately
                tiles = updatedTiles
                isPreviewPhase = false // Set this before starting the turn timer
                isSelectingMove = false // Make sure we're not in selection mode
                
                // Get host ID before updating Firebase
                val hostId = getHostId()
                val currentTimestamp = System.currentTimeMillis()
                
                gameCode?.let { code ->
                    firestore.collection("game_sessions")
                        .document(code)
                        .update(mapOf(
                            "board" to updatedTiles.map { tile ->
                                mapOf(
                                    "id" to tile.id.toString(),
                                    "position" to tile.position,
                                    "numbers" to tile.numbers,
                                    "isRevealed" to tile.isRevealed,
                                    "isMatched" to tile.isMatched
                                )
                            },
                            "hostWantsToStart" to false,
                            "guestWantsToStart" to false,
                            "gameStarted" to true,
                            "currentTurn" to hostId, // Host always starts first
                            "isSelectingMove" to false, // Make sure selection mode is off
                            "turnStartTimestamp" to currentTimestamp // Reset timestamp
                        ))
                        .await()
                }
                
                gameStarted = true
                
                // Start the turn timer only if I'm the host
                val isHost = (gameState as? MultiplayerGameState.InGame)?.isHost == true
                if (isHost) {
                    isMyTurn = true
                    // Start the turn timer after everything is set up
                    startTurnTimer()
                } else {
                    isMyTurn = false
                    turnTimeLeft = 15 // Just set the timer value for display
                }
            } catch (e: Exception) {
                errorMessage = "Failed to start game: ${e.message}"
            }
        }
    }

    private suspend fun getHostId(): String? {
        return gameCode?.let { code ->
            val gameSession = firestore.collection("game_sessions")
                .document(code)
                .get()
                .await()
            gameSession.getString("hostId")
        }
    }

    fun onNumberSelected(tile: Tile, numberIndex: Int) {
        if (!isMyTurn || isSelectingMove || isPreviewPhase || !gameStarted) return

        viewModelScope.launch {
            try {
                val newSelectedNumbers = selectedNumbers + SelectedNumber(tile, numberIndex)
                selectedNumbers = newSelectedNumbers
                revealedNumbersCount++ // Increment the counter

                // Update the revealed state in Firebase while preserving matched state
                val updatedTiles = tiles.map {
                    if (it.id == tile.id) {
                        val newRevealed = it.isRevealed.toMutableList().apply {
                            set(numberIndex, true)
                        }
                        it.copy(isRevealed = newRevealed, isMatched = it.isMatched) // Preserve matched state
                    } else it
                }
                
                // Update local state first
                tiles = updatedTiles
                
                // Then update Firebase
                saveBoardToFirebase(updatedTiles)

                if (newSelectedNumbers.size == 2) {
                    // Check if numbers match
                    val first = newSelectedNumbers[0]
                    val second = newSelectedNumbers[1]
                    
                    if (first.tile.numbers[first.numberIndex] == second.tile.numbers[second.numberIndex]) {
                        // Correct pair - handle it first before any other updates
                        handleCorrectPair(first.tile.numbers[first.numberIndex])
                    } else {
                        // Incorrect pair - show red background briefly
                        incorrectPair = newSelectedNumbers
                        delay(1000) // Show the incorrect pair briefly
                        incorrectPair = emptyList() // Clear incorrect pair
                        
                        // Update tiles while preserving matched state
                        val updatedTilesAfterDelay = tiles.map { tile ->
                            if (tile.id == first.tile.id || tile.id == second.tile.id) {
                                val newRevealed = tile.isRevealed.toMutableList().apply {
                                    if (tile.id == first.tile.id) set(first.numberIndex, false)
                                    if (tile.id == second.tile.id) set(second.numberIndex, false)
                                }
                                tile.copy(isRevealed = newRevealed, isMatched = tile.isMatched) // Preserve matched state
                            } else tile
                        }
                        
                        // Set isSelectingMove to true BEFORE updating Firebase
                        // This ensures the timer stops immediately
                        isSelectingMove = true
                        selectedNumbers = emptyList()
                        revealedNumbersCount = 0 // Reset counter after handling the pair
                        
                        // Cancel any existing timer job immediately
                        turnTimerJob?.cancel()
                        turnTimerJob = null
                        turnTimeLeft = 15
                        
                        // Update local state
                        tiles = updatedTilesAfterDelay
                        
                        // Then update Firebase with the updated tiles and reset timer
                        gameCode?.let { code ->
                            val currentTimestamp = System.currentTimeMillis()
                            
                            firestore.collection("game_sessions")
                                .document(code)
                                .update(
                                    mapOf(
                                        "board" to updatedTilesAfterDelay.map { tile ->
                                            mapOf(
                                                "id" to tile.id.toString(),
                                                "position" to tile.position,
                                                "numbers" to tile.numbers,
                                                "isRevealed" to tile.isRevealed,
                                                "isMatched" to tile.isMatched
                                            )
                                        },
                                        "incorrectPair" to emptyList<Map<String, Any>>(),
                                        "turnStartTimestamp" to currentTimestamp,
                                        "isSelectingMove" to true
                                    )
                                )
                                .await()
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to process move: ${e.message}"
            }
        }
    }

    private suspend fun handleCorrectPair(matchedNumber: Int) {
        try {
            // Update matched state for all tiles with this number
            val updatedTiles = tiles.map { tile ->
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

            // Set transitioning flag
            isTimerTransitioning = true
            
            // Update local state first
            withContext(Dispatchers.Main) {
                tiles = updatedTiles
                turnTimeLeft = 15 // Set immediately to prevent glitches
            }

            // Reset local timer
            turnTimerJob?.cancel()
            turnTimerJob = null

            // Then update Firebase with a transaction to ensure consistency
            gameCode?.let { code ->
                val isHost = (gameState as? MultiplayerGameState.InGame)?.isHost == true
                val scoreField = if (isHost) "hostScore" else "guestScore"
                val newScore = myScore + 1  // Always increment the current player's score
                val currentTimestamp = System.currentTimeMillis()

                firestore.runTransaction { transaction ->
                    val docRef = firestore.collection("game_sessions").document(code)
                    transaction.update(docRef, mapOf(
                        "board" to updatedTiles.map { tile ->
                            mapOf(
                                "id" to tile.id.toString(),
                                "position" to tile.position,
                                "numbers" to tile.numbers,
                                "isRevealed" to tile.isRevealed,
                                "isMatched" to tile.isMatched
                            )
                        },
                        scoreField to newScore,
                        "incorrectPair" to emptyList<Map<String, Any>>(),
                        "lastMatchTimestamp" to com.google.firebase.Timestamp.now(),
                        "turnStartTimestamp" to currentTimestamp,
                        "isSelectingMove" to false
                    ))
                }.await()

                withContext(Dispatchers.Main) {
                    selectedNumbers = emptyList()
                    incorrectPair = emptyList()
                    revealedNumbersCount = 0
                    isTimerTransitioning = false
                }
                
                // Start a new timer job for consistent behavior
                startTurnTimer()
            }
        } catch (e: Exception) {
            isTimerTransitioning = false
            errorMessage = "Failed to handle correct pair: ${e.message}"
        }
    }

    fun moveTile(fromPosition: Int) {
        if (!isSelectingMove || !movableTilePositions.contains(fromPosition)) return

        // Set transitioning flag
        isTimerTransitioning = true
        
        viewModelScope.launch {
            try {
                // Update tile positions
                val updatedTiles = tiles.map { tile ->
                    when (tile.position) {
                        fromPosition -> tile.copy(position = emptyPosition)
                        emptyPosition -> tile.copy(position = fromPosition)
                        else -> tile
                    }
                }

                // Get opponent ID before updating Firebase
                val opponentId = getOpponentId()
                
                // Update local state
                withContext(Dispatchers.Main) {
                    tiles = updatedTiles.toList()
                    emptyPosition = fromPosition
                    turnTimeLeft = 15 // Set immediately to prevent glitches
                    
                    // Reset states
                    isSelectingMove = false
                    selectedNumbers = emptyList()
                    revealedNumbersCount = 0 // Reset counter when turn ends
                }
                
                // Reset local timer for UI consistency
                turnTimerJob?.cancel()
                turnTimerJob = null

                // Update Firebase
                gameCode?.let { code ->
                    val currentTimestamp = System.currentTimeMillis()
                    
                    firestore.collection("game_sessions")
                        .document(code)
                        .update(
                            mapOf(
                                "board" to updatedTiles.map { tile ->
                                    mapOf(
                                        "id" to tile.id.toString(),
                                        "position" to tile.position,
                                        "numbers" to tile.numbers,
                                        "isRevealed" to tile.isRevealed,
                                        "isMatched" to tile.isMatched
                                    )
                                },
                                "emptyPosition" to fromPosition,
                                "currentTurn" to opponentId, // Switch turns
                                "turnStartTimestamp" to currentTimestamp, // Reset timer for next player
                                "isSelectingMove" to false // Reset selection state
                            )
                        )
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        isTimerTransitioning = false
                    }
                }
            } catch (e: Exception) {
                isTimerTransitioning = false
                errorMessage = "Failed to move tile: ${e.message}"
            }
        }
    }

    private suspend fun getOpponentId(): String? {
        return gameCode?.let { code ->
            val gameSession = firestore.collection("game_sessions")
                .document(code)
                .get()
                .await()

            val currentUserId = auth.getCurrentUser()?.uid
            if (currentUserId == gameSession.getString("hostId")) {
                gameSession.getString("guestId")
            } else {
                gameSession.getString("hostId")
            }
        }
    }

    private fun startListeningForOpponent(gameCode: String) {
        gameSessionListener?.remove()
        gameSessionListener = firestore.collection("game_sessions")
            .document(gameCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = error.message
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    val currentUser = auth.getCurrentUser()
                    val hostId = snapshot.getString("hostId")
                    val guestId = snapshot.getString("guestId")

                    when (status) {
                        "waiting" -> {
                            if (currentUser?.uid == hostId) {
                                gameState = MultiplayerGameState.WaitingForPlayers
                            }
                        }
                        "in_progress" -> {
                            // Set game state for both host and guest
                            when (currentUser?.uid) {
                                hostId -> gameState = MultiplayerGameState.InGame(isHost = true)
                                guestId -> gameState = MultiplayerGameState.InGame(isHost = false)
                            }
                        }
                        "ended" -> {
                            val endReason = snapshot.getString("endReason")
                            when (endReason) {
                                "host_left" -> {
                                    if (currentUser?.uid == guestId) {
                                        opponentLeft = true
                                        gameState = MultiplayerGameState.GameOver(false)
                                    }
                                }
                                "guest_left" -> {
                                    if (currentUser?.uid == hostId) {
                                        opponentLeft = true
                                        gameState = MultiplayerGameState.GameOver(true)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Game session no longer exists
                    opponentLeft = true
                    gameState = MultiplayerGameState.GameOver(false)
                }
            }
    }

    private fun startListeningToGameBoard() {
        gameBoardListener?.remove()
        gameBoardListener = gameCode?.let { code ->
            firestore.collection("game_sessions")
                .document(code)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.message
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val currentUserId = auth.getCurrentUser()?.uid
                        val currentTurn = snapshot.getString("currentTurn")
                        val wasMyTurn = isMyTurn
                        val wasSelectingMove = isSelectingMove
                        isMyTurn = currentTurn == currentUserId
                        
                        // Get selection state from Firebase
                        val serverIsSelectingMove = snapshot.getBoolean("isSelectingMove") ?: false
                        
                        // Update local selection state if it's my turn
                        if (isMyTurn) {
                            val selectionChanged = isSelectingMove != serverIsSelectingMove
                            isSelectingMove = serverIsSelectingMove
                            
                            // If selection state changed, make sure to cancel any running timer
                            if (selectionChanged && isSelectingMove) {
                                turnTimerJob?.cancel()
                                turnTimerJob = null
                                turnTimeLeft = 15
                            }
                        }

                        // Handle turn changes and timer
                        val serverTurnStartTimestamp = snapshot.getLong("turnStartTimestamp")
                        
                        // If turn just changed to me, start a new timer
                        if (!wasMyTurn && isMyTurn) {
                            // Cancel any existing timer first
                            turnTimerJob?.cancel()
                            turnTimerJob = null
                            
                            if (!isSelectingMove) {
                                // Start a new timer
                                startTurnTimer()
                            } else {
                                // If we're selecting a move, just set the timer to 15 but don't start it
                                turnTimeLeft = 15
                            }
                        }
                        // If it's not my turn, calculate time based on server timestamp
                        else if (!isMyTurn && serverTurnStartTimestamp != null) {
                            // Cancel any existing timer
                            turnTimerJob?.cancel()
                            turnTimerJob = null
                            
                            // Start a display timer for opponent's turn
                            if (!isPreviewPhase && gameStarted && !serverIsSelectingMove) {
                                turnTimerJob = viewModelScope.launch {
                                    while (!isMyTurn && !isPreviewPhase) {
                                        val currentTime = System.currentTimeMillis()
                                        val elapsedSeconds = ((currentTime - serverTurnStartTimestamp) / 1000).toInt()
                                        val newTimeLeft = maxOf(15 - elapsedSeconds, 0)
                                        
                                        withContext(Dispatchers.Main) {
                                            turnTimeLeft = newTimeLeft
                                        }
                                        
                                        delay(100)
                                    }
                                }
                            }
                        }

                        // Update ready states
                        val hostReady = snapshot.getBoolean("hostReady") ?: false
                        val guestReady = snapshot.getBoolean("guestReady") ?: false
                        val hostWantsToStart = snapshot.getBoolean("hostWantsToStart") ?: false
                        val guestWantsToStart = snapshot.getBoolean("guestWantsToStart") ?: false
                        val gameStarted = snapshot.getBoolean("gameStarted") ?: false
                        val isHost = (gameState as? MultiplayerGameState.InGame)?.isHost == true
                        
                        amIReady = if (isHost) hostReady else guestReady
                        isOpponentReady = if (isHost) guestReady else hostReady
                        wantToStartGame = if (isHost) hostWantsToStart else guestWantsToStart
                        opponentWantsToStartGame = if (isHost) guestWantsToStart else hostWantsToStart
                        this@MultiplayerViewModel.gameStarted = gameStarted

                        // Start preview phase when both players are ready
                        if (hostReady && guestReady && !isPreviewPhase && !gameStarted) {
                            isPreviewPhase = true
                            // Show all numbers for preview
                            val updatedTiles = tiles.map { tile ->
                                tile.copy(isRevealed = List(5) { true })
                            }
                            saveBoardToFirebase(updatedTiles)
                            startPreviewTimer()
                        }

                        // Start game early if both players want to
                        if (isPreviewPhase && hostWantsToStart && guestWantsToStart) {
                            previewTimerJob?.cancel()
                            startActualGame()
                        }

                        // Update board state and scores
                        val boardData = snapshot.get("board") as? List<Map<*, *>>
                        if (boardData != null) {
                            try {
                                // Update tiles with Firebase data
                                tiles = boardData.map { tileData ->
                                    val tileId = (tileData["id"] as String).toInt()
                                    val isMatched = (tileData["isMatched"] as List<Boolean>)
                                    val isRevealed = (tileData["isRevealed"] as List<Boolean>)
                                    
                                    Tile(
                                        id = tileId,
                                        position = (tileData["position"] as Number).toInt(),
                                        numbers = (tileData["numbers"] as List<*>).map { (it as Number).toInt() },
                                        isRevealed = isRevealed.mapIndexed { index, revealed ->
                                            revealed || isMatched[index] // Keep matched numbers revealed
                                        },
                                        isMatched = isMatched // Use matched state from Firebase
                                    )
                                }
                                
                                emptyPosition = (snapshot.getLong("emptyPosition") ?: 8).toInt()
                                
                                // Update incorrect pair state
                                val incorrectPairData = snapshot.get("incorrectPair") as? List<Map<*, *>>
                                incorrectPair = incorrectPairData?.mapNotNull { pairData ->
                                    val tileId = (pairData["tileId"] as String).toInt()
                                    val numberIndex = (pairData["numberIndex"] as Number).toInt()
                                    val tile = tiles.find { it.id == tileId }
                                    if (tile != null) {
                                        SelectedNumber(tile, numberIndex)
                                    } else null
                                } ?: emptyList()
                                
                                if (!isSelectingMove) {
                                    movableTilePositions = getAdjacentPositions(emptyPosition).toSet()
                                }

                                // Update scores
                                val hostScore = snapshot.getLong("hostScore")?.toInt() ?: 0
                                val guestScore = snapshot.getLong("guestScore")?.toInt() ?: 0
                                
                                if (isHost) {
                                    myScore = hostScore
                                    opponentScore = guestScore
                                } else {
                                    myScore = guestScore
                                    opponentScore = hostScore
                                }

                                // Check for game over
                                if (hostScore >= 11 || guestScore >= 11) {
                                    val isWinner = if (isHost) {
                                        hostScore > guestScore
                                    } else {
                                        guestScore > hostScore
                                    }
                                    gameState = MultiplayerGameState.GameOver(isWinner)
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error parsing board data: ${e.message}"
                            }
                        }
                    }
                }
        }
    }

    private suspend fun getCurrentUsername(): String {
        val currentUser = auth.getCurrentUser()
        if (currentUser != null) {
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            return userDoc.getString("username") ?: "Unknown"
        }
        return "Unknown"
    }

    fun clearError() {
        errorMessage = null
    }

    fun setReady() {
        if (amIReady) return
        
        viewModelScope.launch {
            try {
                gameCode?.let { code ->
                    val currentUser = auth.getCurrentUser()
                    if (currentUser != null) {
                        val isHost = (gameState as? MultiplayerGameState.InGame)?.isHost == true
                        val readyField = if (isHost) "hostReady" else "guestReady"
                        
                        firestore.collection("game_sessions")
                            .document(code)
                            .update(mapOf(readyField to true))
                            .await()
                        
                        amIReady = true
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to set ready state: ${e.message}"
            }
        }
    }

    fun setWantToStartGame() {
        if (!isPreviewPhase || wantToStartGame) return
        
        viewModelScope.launch {
            try {
                gameCode?.let { code ->
                    val currentUser = auth.getCurrentUser()
                    if (currentUser != null) {
                        val isHost = (gameState as? MultiplayerGameState.InGame)?.isHost == true
                        val startField = if (isHost) "hostWantsToStart" else "guestWantsToStart"
                        
                        firestore.collection("game_sessions")
                            .document(code)
                            .update(mapOf(startField to true))
                            .await()
                        
                        wantToStartGame = true
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to set start game state: ${e.message}"
            }
        }
    }

    fun exitGame() {
        viewModelScope.launch {
            try {
                cleanupGameSession()
                gameSessionListener?.remove()
                gameBoardListener?.remove()
                previewTimerJob?.cancel() // Cancel any running timers
                turnTimerJob?.cancel()
                gameState = MultiplayerGameState.Initial
                gameCode = null
                opponentLeft = false
                isPrivateGame = false
                tiles = emptyList()
                selectedNumbers = emptyList()
                myScore = 0
                opponentScore = 0
                isSelectingMove = false
                movableTilePositions = emptySet()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to exit game"
            }
        }
    }

    private suspend fun cleanupGameSession() {
        gameCode?.let { code ->
            val currentUser = auth.getCurrentUser()
            if (currentUser != null) {
                try {
                    val gameSession = firestore.collection("game_sessions")
                        .document(code)
                        .get()
                        .await()

                    if (gameSession.exists()) {
                        val hostId = gameSession.getString("hostId")
                        val guestId = gameSession.getString("guestId")

                        // Update game session based on who's leaving
                        firestore.collection("game_sessions")
                            .document(code)
                            .update(
                                when (currentUser.uid) {
                                    hostId -> mapOf(
                                        "status" to "ended",
                                        "endReason" to "host_left"
                                    )
                                    guestId -> mapOf(
                                        "status" to "ended",
                                        "endReason" to "guest_left",
                                        "guestId" to null,
                                        "guestUsername" to null
                                    )
                                    else -> mapOf() // No changes if not a player
                                }
                            )
                            .await()
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to cleanup game session"
                }
            }
        }
    }

    fun createPrivateGame() {
        viewModelScope.launch {
            try {
                val currentUser = auth.getCurrentUser()
                if (currentUser == null) {
                    errorMessage = "You must be logged in to create a game"
                    return@launch
                }

                // Generate a unique game code
                val newGameCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
                gameCode = newGameCode
                isPrivateGame = true

                // Generate initial tiles with all numbers hidden
                val initialTiles = generateTiles().map { tile ->
                    tile.copy(isRevealed = List(5) { false })
                }
                tiles = initialTiles

                // Create a new game session in Firestore
                val gameSession = hashMapOf(
                    "gameCode" to newGameCode,
                    "hostId" to currentUser.uid,
                    "hostUsername" to getCurrentUsername(),
                    "guestId" to null,
                    "guestUsername" to null,
                    "isPrivate" to true,
                    "status" to "waiting",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "board" to initialTiles.map { tile ->
                        mapOf(
                            "id" to tile.id.toString(),
                            "position" to tile.position,
                            "numbers" to tile.numbers,
                            "isRevealed" to tile.isRevealed,
                            "isMatched" to tile.isMatched
                        )
                    },
                    "emptyPosition" to 8,
                    "hostScore" to 0,
                    "guestScore" to 0,
                    "currentTurn" to currentUser.uid,
                    "hostReady" to false,
                    "guestReady" to false,
                    "hostWantsToStart" to false,
                    "guestWantsToStart" to false,
                    "turnStartTimestamp" to System.currentTimeMillis(),
                    "isSelectingMove" to false
                )

                firestore.collection("game_sessions")
                    .document(newGameCode)
                    .set(gameSession)
                    .await()

                gameState = MultiplayerGameState.WaitingForPlayers
                startListeningForOpponent(newGameCode)
                startListeningToGameBoard()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to create game"
                gameState = MultiplayerGameState.Initial
            }
        }
    }

    fun joinGameWithCode(code: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.getCurrentUser()
                if (currentUser == null) {
                    errorMessage = "You must be logged in to join a game"
                    return@launch
                }

                val gameSession = firestore.collection("game_sessions")
                    .document(code)
                    .get()
                    .await()

                if (!gameSession.exists()) {
                    errorMessage = "Game not found"
                    return@launch
                }

                if (gameSession.getString("status") != "waiting") {
                    errorMessage = "Game is no longer available"
                    return@launch
                }

                // Update the game session with the guest's information
                firestore.collection("game_sessions")
                    .document(code)
                    .update(
                        mapOf(
                            "guestId" to currentUser.uid,
                            "guestUsername" to getCurrentUsername(),
                            "status" to "in_progress"
                        )
                    )
                    .await()

                gameCode = code
                startListeningForOpponent(code) // Start listening before setting game state
                startListeningToGameBoard()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to join game"
                gameState = MultiplayerGameState.GameOver(false)
            }
        }
    }

    fun joinRandomGame() {
        viewModelScope.launch {
            try {
                val currentUser = auth.getCurrentUser()
                if (currentUser == null) {
                    errorMessage = "You must be logged in to join a game"
                    return@launch
                }

                // Find an available public game
                val availableGame = firestore.collection("game_sessions")
                    .whereEqualTo("status", "waiting")
                    .whereEqualTo("guestId", null)
                    .whereEqualTo("isPrivate", false)
                    .limit(1)
                    .get()
                    .await()

                if (availableGame.isEmpty) {
                    // No available games, create a new one
                    val newGameCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
                    gameCode = newGameCode
                    isPrivateGame = false

                    // Generate initial tiles with all numbers hidden
                    val initialTiles = generateTiles().map { tile ->
                        tile.copy(isRevealed = List(5) { false })
                    }
                    tiles = initialTiles

                    // Create a new game session in Firestore
                    val gameSession = hashMapOf(
                        "gameCode" to newGameCode,
                        "hostId" to currentUser.uid,
                        "hostUsername" to getCurrentUsername(),
                        "guestId" to null,
                        "guestUsername" to null,
                        "isPrivate" to false,
                        "status" to "waiting",
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "board" to initialTiles.map { tile ->
                            mapOf(
                                "id" to tile.id.toString(),
                                "position" to tile.position,
                                "numbers" to tile.numbers,
                                "isRevealed" to tile.isRevealed,
                                "isMatched" to tile.isMatched
                            )
                        },
                        "emptyPosition" to 8,
                        "hostScore" to 0,
                        "guestScore" to 0,
                        "currentTurn" to currentUser.uid,
                        "hostReady" to false,
                        "guestReady" to false,
                        "hostWantsToStart" to false,
                        "guestWantsToStart" to false,
                        "turnStartTimestamp" to System.currentTimeMillis(),
                        "isSelectingMove" to false
                    )

                    firestore.collection("game_sessions")
                        .document(newGameCode)
                        .set(gameSession)
                        .await()

                    gameState = MultiplayerGameState.WaitingForPlayers
                    startListeningForOpponent(newGameCode)
                    startListeningToGameBoard()
                } else {
                    // Join the available game
                    val gameSession = availableGame.documents[0]
                    val gameCode = gameSession.getString("gameCode")!!
                    joinGameWithCode(gameCode)
                    startListeningToGameBoard()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to join or create game"
                gameState = MultiplayerGameState.GameOver(false)
            }
        }
    }

    // Add a helper function to update the selection state in Firebase
    private fun updateSelectionStateInFirebase(isSelecting: Boolean) {
        viewModelScope.launch {
            try {
                gameCode?.let { code ->
                    firestore.collection("game_sessions")
                        .document(code)
                        .update(mapOf(
                            "isSelectingMove" to isSelecting
                        ))
                        .await() // Wait for the update to complete
                }
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }
}