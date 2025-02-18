package com.mobdev.matchandmix.data.scores

import android.content.Context
import android.content.SharedPreferences

class LocalScoreRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHighScore(): Int {
        return prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    fun updateHighScore(newScore: Int) {
        val currentHighScore = getHighScore()
        if (newScore > currentHighScore) {
            prefs.edit().putInt(KEY_HIGH_SCORE, newScore).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "match_and_mix_scores"
        private const val KEY_HIGH_SCORE = "high_score"
    }
} 