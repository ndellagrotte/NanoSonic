package com.example.nanosonicproject.ui.screens.auth.login

/**
 * UI State for Login Screen
 * Contains all form fields, validation errors, and UI flags
 */
data class LoginState(
    // Form fields
    val email: String = "",
    val password: String = "",

    // Field-specific errors
    val emailError: String? = null,
    val passwordError: String? = null,

    // UI state flags
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginComplete: Boolean = false
) {
    /**
     * Check if all required fields are filled
     */
    val hasAllRequiredFields: Boolean
        get() = email.isNotBlank() && password.isNotBlank()

    /**
     * Check if form has any errors
     */
    val hasErrors: Boolean
        get() = emailError != null || passwordError != null

    /**
     * Check if login button should be enabled
     */
    val canLogin: Boolean
        get() = hasAllRequiredFields && !hasErrors && !isLoading
}