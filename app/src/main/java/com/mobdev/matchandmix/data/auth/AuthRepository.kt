package com.mobdev.matchandmix.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.auth.GoogleAuthProvider

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    suspend fun login(username: String, password: String): AuthResult
    suspend fun register(fullName: String, username: String, email: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun registerWithGoogle(idToken: String, username: String): AuthResult
    fun getCurrentUser(): FirebaseUser?
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun login(username: String, password: String): AuthResult {
        return try {
            Log.d("FirebaseAuth", "Starting login process for username: $username")
            
            val userQuery = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (userQuery.isEmpty) {
                Log.d("FirebaseAuth", "No user found with username: $username")
                return AuthResult.Error("No account found with this username")
            }

            val userDoc = userQuery.documents.first()
            val email = userDoc.getString("email")
            
            if (email == null) {
                Log.e("FirebaseAuth", "Email not found for username: $username")
                return AuthResult.Error("Account data error")
            }
            
            Log.d("FirebaseAuth", "Found email for username: $username, attempting login")

            val result = auth.signInWithEmailAndPassword(email, password).await()
            
            result.user?.let { user ->
                firestore.collection("users")
                    .document(user.uid)
                    .update("lastLoginAt", Date())
                    .await()

                Log.d("FirebaseAuth", "Login successful for username: $username")
                AuthResult.Success(user, username)
            } ?: run {
                Log.e("FirebaseAuth", "Login failed: user is null")
                AuthResult.Error("Login failed")
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Login error: ${e.message}", e)
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String
    ): AuthResult {
        return try {
            val usernameQuery = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) {
                return AuthResult.Error("Username already exists")
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            
            result.user?.let { user ->
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "fullName" to fullName,
                    "username" to username,
                    "email" to email,
                    "createdAt" to Date(),
                    "lastLoginAt" to Date(),
                    "highScore" to 0
                )

                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()

                // Sign out the user after successful registration
                auth.signOut()

                AuthResult.Success(user, username)
            } ?: AuthResult.Error("Registration failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            result.user?.let { user ->
                // Check if user exists in Firestore
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    val username = userDoc.getString("username") ?: return AuthResult.Error("Username not found")
                    AuthResult.Success(user, username)
                } else {
                    // User doesn't exist in Firestore
                    auth.signOut()
                    AuthResult.Error("NEW_GOOGLE_USER")
                }
            } ?: AuthResult.Error("Sign in failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun registerWithGoogle(idToken: String, username: String): AuthResult {
        return try {
            // First check if username is available
            val usernameQuery = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) {
                return AuthResult.Error("Username already exists")
            }

            // Sign in with Google credentials
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            result.user?.let { user ->
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "fullName" to user.displayName,
                    "username" to username,
                    "email" to user.email,
                    "createdAt" to Date(),
                    "lastLoginAt" to Date(),
                    "highScore" to 0
                )

                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()

                AuthResult.Success(user, username)
            } ?: AuthResult.Error("Registration failed")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun signOut() {
        auth.signOut()
    }
} 