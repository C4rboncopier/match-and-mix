package com.mobdev.matchandmix.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdev.matchandmix.data.auth.AuthRepository
import com.mobdev.matchandmix.data.auth.AuthResult
import com.mobdev.matchandmix.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var authState by mutableStateOf<AuthResult?>(null)
        private set

    var isGoogleSignInInProgress by mutableStateOf(false)
        private set

    private var pendingGoogleIdToken: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _currentUsername = MutableStateFlow<String>("")
    val currentUsername: StateFlow<String> = _currentUsername

    init {
        checkAndUpdateCurrentUser()
    }

    fun checkAndUpdateCurrentUser() {
        viewModelScope.launch {
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                val username = getUsernameFromFirestore(currentUser.uid)
                _currentUsername.value = username ?: ""
            } else {
                _currentUsername.value = ""
            }
        }
    }

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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            if (result is AuthResult.Error && result.message == "NEW_GOOGLE_USER") {
                pendingGoogleIdToken = idToken
                isGoogleSignInInProgress = true
            } else {
                authState = result
            }
        }
    }

    fun completeGoogleSignUp(username: String) {
        viewModelScope.launch {
            pendingGoogleIdToken?.let { idToken ->
                authState = repository.registerWithGoogle(idToken, username)
                pendingGoogleIdToken = null
                isGoogleSignInInProgress = false
            }
        }
    }

    fun cancelGoogleSignIn() {
        pendingGoogleIdToken = null
        isGoogleSignInInProgress = false
        repository.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
        _currentUsername.value = ""
        repository.signOut()
    }

    suspend fun getUsernameFromFirestore(uid: String): String? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.getString("username")
        } catch (e: Exception) {
            null
        }
    }
} 