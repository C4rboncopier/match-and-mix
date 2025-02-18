package com.mobdev.matchandmix.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobdev.matchandmix.data.auth.AuthRepository
import com.mobdev.matchandmix.data.auth.AuthResult
import com.mobdev.matchandmix.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var authState by mutableStateOf<AuthResult?>(null)
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            authState = repository.login(username, password)
        }
    }

    fun register(fullName: String, username: String, email: String, password: String) {
        viewModelScope.launch {
            authState = repository.register(fullName, username, email, password)
        }
    }

    fun getCurrentUser() = repository.getCurrentUser()

    fun signOut() = repository.signOut()

    suspend fun getUsernameFromFirestore(uid: String): String? {
        return try {
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            userDoc.getString("username")
        } catch (e: Exception) {
            null
        }
    }
} 