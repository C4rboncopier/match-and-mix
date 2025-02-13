package com.mobdev.matchandmix.utils

import com.mobdev.matchandmix.data.models.Tile

fun generateTiles(): List<Tile> {
    // Create the full set of numbers (1-20, each number appears twice)
    val allNumbers = ((1..20) + (1..20)).toList().shuffled()

    // Divide the 40 numbers into exactly 8 groups of 5 numbers each
    val numberGroups = allNumbers.take(40).chunked(5).take(8)

    // Create exactly 8 tiles with the groups of numbers
    return numberGroups.mapIndexed { index, numbers ->
        Tile(
            id = index,
            numbers = numbers,
            isRevealed = List(5) { true },  // All revealed during preview
            isMatched = List(5) { false },
            position = index
        )
    }
}

fun getAdjacentPositions(position: Int): List<Int> {
    if (position !in 0..8) return emptyList() // Invalid position

    val row = position / 3
    val col = position % 3

    return buildList {
        if (row > 0) add(position - 3)  // North
        if (row < 2) add(position + 3)  // South
        if (col > 0) add(position - 1)  // West
        if (col < 2) add(position + 1)  // East
    }.filter { it in 0..7 } // Only return valid tile positions (0-7, excluding empty space at 8)
}

fun moveTile(tiles: List<Tile>, fromPosition: Int, toPosition: Int): List<Tile> {
    // Validate positions
    if (fromPosition !in 0..8 || toPosition !in 0..8) return tiles

    return tiles.map { tile ->
        when (tile.position) {
            fromPosition -> tile.copy(position = toPosition)
            else -> tile
        }
    }
}

// Helper function to start the game
fun startGame(
    currentTiles: List<Tile>,
    currentGameState: GameState,
    updateGame: (List<Tile>) -> Unit
) {
    if (currentGameState == GameState.PREVIEW) {
        // Hide all numbers and start the game
        val newTiles = currentTiles.map {
            it.copy(isRevealed = List(5) { false })
        }
        updateGame(newTiles)
    }
}