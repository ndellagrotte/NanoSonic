package com.example.nanosonicproject.ui.screens.auth.register
//------------------------------------------------
// UI State for Register Screen
// Contains all form fields, validation errors, and UI flags
//------------------------------------------------

data class RegisterState(
    // Form fields
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    // Field-specific errors
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    // UI state flags
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationComplete: Boolean = false
) {
    /**
     * Check if all required fields are filled
     */
    val hasAllRequiredFields: Boolean
        get() = username.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank()

    /**
     * Check if form has any errors
     */
    val hasErrors: Boolean
        get() = usernameError != null ||
                emailError != null ||
                passwordError != null ||
                confirmPasswordError != null

    /**
     * Check if register button should be enabled
     */
    val canRegister: Boolean
        get() = hasAllRequiredFields && !hasErrors && !isLoading
}