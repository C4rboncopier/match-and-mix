package com.mobdev.matchandmix.utils

object ScoreUtils {
    private const val MAX_POINTS_PER_PAIR = 50
    private const val MAX_TIME_SECONDS = 15

    fun calculateScore(timeLeft: Int): Int {
        // Calculate score based on time left (out of 15 seconds)
        // If all time is used (timeLeft = 0), minimum score is 10 points
        // If matched instantly (timeLeft = 15), maximum score is 50 points
        return ((timeLeft.toFloat() / MAX_TIME_SECONDS) * MAX_POINTS_PER_PAIR).toInt().coerceIn(10, MAX_POINTS_PER_PAIR)
    }
} 