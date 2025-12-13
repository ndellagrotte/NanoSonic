package com.example.nanosonicproject.ui.screens.auth.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.nanosonicproject.ui.components.EmailTextField
import com.example.nanosonicproject.ui.components.PasswordTextField
import com.example.nanosonicproject.ui.components.UsernameTextField
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * Registration screen for new users
 * Collects username, email, and password
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
                            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
                        }, null),
    onNavigateToLogin: () -> Unit,
    onNavigateToWizard: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(state.isRegistrationComplete) {
        if (state.isRegistrationComplete) {
            onNavigateToWizard()
        }
    }

    RegisterScreenContent(
        state = state,
        onUsernameChanged = { viewModel.onUsernameChanged(it) },
        onEmailChanged = { viewModel.onEmailChanged(it) },
        onPasswordChanged = { viewModel.onPasswordChanged(it) },
        onConfirmPasswordChanged = { viewModel.onConfirmPasswordChanged(it) },
        onRegisterClick = { viewModel.onRegisterClicked() },
        onLoginClick = onNavigateToLogin
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    state: RegisterState,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Text(
                    text = "Welcome to NanoSonic",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create an account to sync your EQ profiles across devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Username Field
                UsernameTextField(
                    value = state.username,
                    onValueChange = onUsernameChanged,
                    isError = state.usernameError != null,
                    errorMessage = state.usernameError,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                EmailTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    isError = state.emailError != null,
                    errorMessage = state.emailError,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                PasswordTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    isError = state.passwordError != null,
                    errorMessage = state.passwordError,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                PasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChanged,
                    label = "Confirm Password",
                    isError = state.confirmPasswordError != null,
                    errorMessage = state.confirmPasswordError,
                    imeAction = ImeAction.Done,
                    onImeAction = onRegisterClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Requirements
                if (state.password.isNotEmpty()) {
                    PasswordRequirements(password = state.password)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Register Button
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onLoginClick) {
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error Snackbar
            if (state.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { /* Dismiss handled by ViewModel */ }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(state.errorMessage)
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirements(password: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Password Requirements:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            RequirementItem("At least 8 characters", password.length >= 8)
            RequirementItem("Contains uppercase letter", password.any { it.isUpperCase() })
            RequirementItem("Contains lowercase letter", password.any { it.isLowerCase() })
            RequirementItem("Contains number", password.any { it.isDigit() })
        }
    }
}

@Composable
private fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isMet) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    NanoSonicProjectTheme {
        RegisterScreenContent(
            state = RegisterState(),
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenWithErrorPreview() {
    NanoSonicProjectTheme {
        RegisterScreenContent(
            state = RegisterState(
                username = "john",
                email = "invalid-email",
                password = "pass",
                emailError = "Invalid email format",
                passwordError = "Password too short"
            ),
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}