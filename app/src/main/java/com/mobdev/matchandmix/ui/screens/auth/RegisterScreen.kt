package com.mobdev.matchandmix.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn.getClient
import com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount
import com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_FAILED
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.ConnectionResult.NETWORK_ERROR
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.auth.AuthResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var googleUsername by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignInClient = remember {
        getClient(
            context,
            GoogleSignInOptions.Builder(DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()
        )
    }

    val firestore = FirebaseFirestore.getInstance()

    // Add debug check for last signed in account
    LaunchedEffect(Unit) {
        val account = getLastSignedInAccount(context)
        if (account != null) {
            // Clear previous sign-in state
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d("GoogleSignIn", "Previous sign-in state cleared")
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleSignIn", "Result code: ${result.resultCode}")
        
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = getSignedInAccountFromIntent(result.data)
                try {
                    Log.d("GoogleSignIn", "Attempting to get Google account")
                    val account = task.getResult(ApiException::class.java)
                    if (account == null) {
                        Log.e("GoogleSignIn", "Account is null")
                        showError = true
                        errorMessage = "Failed to get Google account information"
                        isLoading = false
                        return@rememberLauncherForActivityResult
                    }

                    Log.d("GoogleSignIn", "Email: ${account.email}, Name: ${account.displayName}")
                    
                    // First check if the Google account already exists
                    account.email?.let { email ->
                        scope.launch {
                            try {
                                val userQuery = firestore.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .await()

                                if (!userQuery.isEmpty) {
                                    Log.d("GoogleSignIn", "Account already exists with email: $email")
                                    showError = true
                                    errorMessage = "An account with this Google email already exists. Please use the login screen."
                                    isLoading = false
                                    googleSignInClient.signOut()
                                    return@launch
                                }

                                // If account doesn't exist, proceed with registration
                                account.idToken?.let { token ->
                                    Log.d("GoogleSignIn", "Got ID token, attempting sign in")
                                    viewModel.signInWithGoogle(token)
                                } ?: run {
                                    Log.e("GoogleSignIn", "ID token is null")
                                    showError = true
                                    errorMessage = "Failed to get authentication token"
                                    isLoading = false
                                }
                            } catch (e: Exception) {
                                Log.e("GoogleSignIn", "Error checking existing account", e)
                                showError = true
                                errorMessage = "Error checking account status"
                                isLoading = false
                            }
                        }
                    } ?: run {
                        Log.e("GoogleSignIn", "Email is null")
                        showError = true
                        errorMessage = "Failed to get email from Google account"
                        isLoading = false
                    }
                } catch (e: ApiException) {
                    Log.e("GoogleSignIn", "Sign in failed", e)
                    showError = true
                    when (e.statusCode) {
                        SIGN_IN_FAILED -> {
                            Log.e("GoogleSignIn", "Sign in failed with code: ${e.statusCode}")
                            errorMessage = "Sign in failed. Please try again."
                            googleSignInClient.signOut().addOnCompleteListener {
                                Log.d("GoogleSignIn", "Signed out after failure")
                            }
                        }
                        SIGN_IN_CANCELLED -> {
                            Log.d("GoogleSignIn", "Sign in cancelled by user")
                            errorMessage = "Sign in cancelled"
                        }
                        NETWORK_ERROR -> {
                            Log.e("GoogleSignIn", "Network error")
                            errorMessage = "Network error. Please check your connection."
                        }
                        else -> {
                            Log.e("GoogleSignIn", "Unknown error: ${e.statusCode}")
                            errorMessage = "Google sign in failed: ${e.message}"
                        }
                    }
                    isLoading = false
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("GoogleSignIn", "Sign in cancelled by user (Result Canceled)")
                isLoading = false
                showError = true
                errorMessage = "Sign in cancelled"
            }
            else -> {
                Log.e("GoogleSignIn", "Unknown result code: ${result.resultCode}")
                isLoading = false
                showError = true
                errorMessage = "Unknown error occurred"
            }
        }
    }

    // Update the Google Sign In handler
    fun handleGoogleSignIn() {
        try {
            isLoading = true
            Log.d("GoogleSignIn", "Starting Google Sign In process")
            
            // Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            if (resultCode != ConnectionResult.SUCCESS) {
                Log.e("GoogleSignIn", "Google Play Services not available: $resultCode")
                isLoading = false
                showError = true
                errorMessage = "Google Play Services is not available"
                return
            }

            // Clear any existing Google Sign In state
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d("GoogleSignIn", "Previous sign-in state cleared, launching sign in")
                launcher.launch(googleSignInClient.signInIntent)
            }.addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Failed to clear previous sign-in state", e)
                isLoading = false
                showError = true
                errorMessage = "Failed to initialize Google Sign In"
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Exception during sign in", e)
            isLoading = false
            showError = true
            errorMessage = "Failed to initialize Google Sign In"
        }
    }

    // Add this function to handle normal registration
    fun handleNormalRegistration() {
        if (password != confirmPassword) {
            showError = true
            errorMessage = "Passwords do not match"
            return
        }

        if (fullName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            showError = true
            errorMessage = "Please fill in all fields"
            return
        }

        isLoading = true
        showError = false
        scope.launch {
            try {
                // Check if username or email already exists
                val usernameQuery = firestore.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (!usernameQuery.isEmpty) {
                    showError = true
                    errorMessage = "Username already exists"
                    isLoading = false
                    return@launch
                }

                val emailQuery = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!emailQuery.isEmpty) {
                    showError = true
                    errorMessage = "Email already exists"
                    isLoading = false
                    return@launch
                }

                // If both username and email are available, proceed with registration
                viewModel.register(fullName, username, email, password)
            } catch (e: Exception) {
                Log.e("Registration", "Error during registration", e)
                showError = true
                errorMessage = "Registration failed: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(viewModel.authState) {
        when (val state = viewModel.authState) {
            is AuthResult.Success -> {
                isLoading = false
                if (viewModel.isGoogleSignInInProgress) {
                    onRegisterSuccess()
                } else {
                    showSuccessDialog = true
                }
            }
            is AuthResult.Error -> {
                isLoading = false
                if (state.message != "NEW_GOOGLE_USER") {
                    showError = true
                    errorMessage = state.message
                }
            }
            null -> {
                isLoading = false
            }
        }
    }

    if (viewModel.isGoogleSignInInProgress) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelGoogleSignIn() },
            title = {
                Text(
                    "Choose a Username",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        "Please choose a username for your account:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = googleUsername,
                        onValueChange = { googleUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleUsername.isNotBlank()) {
                            viewModel.completeGoogleSignUp(googleUsername)
                        }
                    },
                    enabled = googleUsername.isNotBlank()
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelGoogleSignIn() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { 
                Text(
                    "Registration Successful",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = { 
                Text(
                    "Your account has been created successfully! Please login to continue.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToLogin()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Login")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(top = 25.dp, start = 16.dp, end = 16.dp)
    ) {
        // Top Bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Register",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // Empty box for alignment
            Box(modifier = Modifier.size(48.dp))
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Fill in your details to get started",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Button(
                    onClick = { handleNormalRegistration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && fullName.isNotBlank() && username.isNotBlank() && 
                             email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Social Login Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "or continue with",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                // Social Login Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { handleGoogleSignIn() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.icons8_google),
                                    contentDescription = "Google Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Google",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { /* Facebook sign in */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icons8_facebook),
                                contentDescription = "Facebook Icon",
                                modifier = Modifier.size(28.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Facebook",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (showError) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Login section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Login",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
} 