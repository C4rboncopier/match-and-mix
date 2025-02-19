package com.mobdev.matchandmix.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.auth.AuthResult
import androidx.compose.ui.graphics.Color
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()
        )
    }

    // Add debug check for last signed in account
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
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
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
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
                    
                    account.idToken?.let { token ->
                        Log.d("GoogleSignIn", "Got ID token, attempting sign in")
                        viewModel.signInWithGoogle(token)
                    } ?: run {
                        Log.e("GoogleSignIn", "ID token is null")
                        showError = true
                        errorMessage = "Failed to get authentication token"
                        isLoading = false
                    }
                } catch (e: ApiException) {
                    Log.e("GoogleSignIn", "Sign in failed", e)
                    showError = true
                    when (e.statusCode) {
                        GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                            Log.e("GoogleSignIn", "Sign in failed with code: ${e.statusCode}")
                            errorMessage = "Sign in failed. Please try again."
                            googleSignInClient.signOut().addOnCompleteListener {
                                Log.d("GoogleSignIn", "Signed out after failure")
                            }
                        }
                        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                            Log.d("GoogleSignIn", "Sign in cancelled by user")
                            errorMessage = "Sign in cancelled"
                        }
                        GoogleSignInStatusCodes.NETWORK_ERROR -> {
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

    LaunchedEffect(viewModel.authState) {
        when (val state = viewModel.authState) {
            is AuthResult.Success -> {
                isLoading = false
                showError = false
                onLoginSuccess(state.username)
            }
            is AuthResult.Error -> {
                isLoading = false
                showError = true
                errorMessage = state.message
            }
            null -> {
                isLoading = false
            }
        }
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
                .padding(bottom = 24.dp),
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
                text = "Login",
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Form section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Button(
                    onClick = { 
                        isLoading = true
                        viewModel.login(username, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Login",
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

            // Register section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
} 