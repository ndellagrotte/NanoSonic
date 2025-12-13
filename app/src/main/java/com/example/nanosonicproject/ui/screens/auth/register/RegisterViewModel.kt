package com.example.nanosonicproject.ui.screens.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    // ← ALL THESE FUNCTIONS ARE CALLED FROM THE UI
    fun onUsernameChanged(username: String) {
        _state.update {
            it.copy(
                username = username,
                usernameError = null
            )
        }
    }

    fun onEmailChanged(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null
            )
        }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null
            )
        }
    }

    fun onRegisterClicked() {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // Simulate API call
            delay(1500)

            // Success
            _state.update {
                it.copy(
                    isLoading = false,
                    isRegistrationComplete = true
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        val usernameError = when {
            currentState.username.isBlank() -> {
                isValid = false
                "Username is required"
            }
            currentState.username.length < 3 -> {
                isValid = false
                "Username must be at least 3 characters"
            }
            else -> null
        }

        val emailError = when {
            currentState.email.isBlank() -> {
                isValid = false
                "Email is required"
            }
            !isValidEmail(currentState.email) -> {
                isValid = false
                "Invalid email format"
            }
            else -> null
        }

        val passwordError = when {
            currentState.password.isBlank() -> {
                isValid = false
                "Password is required"
            }
            currentState.password.length < 8 -> {
                isValid = false
                "Password must be at least 8 characters"
            }
            else -> null
        }

        val confirmPasswordError = when {
            currentState.confirmPassword != currentState.password -> {
                isValid = false
                "Passwords do not match"
            }
            else -> null
        }

        _state.update {
            it.copy(
                usernameError = usernameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}