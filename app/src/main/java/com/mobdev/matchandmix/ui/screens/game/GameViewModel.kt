package com.mobdev.matchandmix.ui.screens.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdev.matchandmix.data.auth.FirebaseAuthRepository
import com.mobdev.matchandmix.data.scores.LocalScoreRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val localScoreRepository = LocalScoreRepository(application)

    var highScore by mutableStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadHighScore()
    }

    private fun loadHighScore() {
        viewModelScope.launch {
            isLoading = true
            try {
                val currentUser = auth.getCurrentUser()
                if (currentUser != null) {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                    
                    highScore = userDoc.getLong("highScore")?.toInt() ?: 0
                } else {
                    highScore = localScoreRepository.getHighScore()
                }
            } catch (e: Exception) {
                highScore = localScoreRepository.getHighScore()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateHighScore(newScore: Int) {
        viewModelScope.launch {
            try {
                val currentUser = auth.getCurrentUser()
                if (currentUser != null) {
                    if (newScore > highScore) {
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .update("highScore", newScore)
                            .await()
                        highScore = newScore
                    }
                } else {
                    localScoreRepository.updateHighScore(newScore)
                    highScore = localScoreRepository.getHighScore()
                }
            } catch (e: Exception) {
                localScoreRepository.updateHighScore(newScore)
                highScore = localScoreRepository.getHighScore()
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.getCurrentUser() != null
    }
} 