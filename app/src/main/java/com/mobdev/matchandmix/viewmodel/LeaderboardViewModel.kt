package com.mobdev.matchandmix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdev.matchandmix.model.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LeaderboardViewModel : ViewModel() {
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val db = FirebaseFirestore.getInstance()
                val querySnapshot = db.collection("users")
                    .orderBy("highScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val entries = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(LeaderboardEntry::class.java)
                }
                
                _leaderboardEntries.value = entries
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while fetching leaderboard data"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 