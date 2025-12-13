package com.example.nanosonicproject.ui.screens.auth.login

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
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun onLoginClicked() {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            delay(1500)

            val success = true

            if (success) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoginComplete = true
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid email or password"
                    )
                }
            }
        }
    }

    fun onForgotPasswordClicked() {
        _state.update {
            it.copy(
                errorMessage = "Forgot password feature coming soon!"
            )
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

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
            currentState.password.length < 6 -> {
                isValid = false
                "Password is too short"
            }
            else -> null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError
            )
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

