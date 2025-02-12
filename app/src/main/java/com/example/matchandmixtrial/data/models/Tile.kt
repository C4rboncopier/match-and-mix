package com.example.matchandmixtrial.data.models

data class Tile(
    val id: Int,
    val numbers: List<Int>,
    var isRevealed: List<Boolean> = List(5) { false },  // Track revealed state for each number
    var isMatched: List<Boolean> = List(5) { false },   // Track matched state for each number
    var position: Int = id  // Track tile position in the grid
) 