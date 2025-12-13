package com.example.nanosonicproject.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * Custom text field component for NanoSonic app
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier for customization
 * @param label Label text displayed above the field
 * @param placeholder Hint text shown when field is empty
 * @param leadingIcon Icon displayed at the start of the field
 * @param isError Whether the field should show error state
 * @param errorMessage Error message to display below the field
 * @param helperText Helper text displayed below the field when no error
 * @param keyboardType Type of keyboard to show (text, email, number, etc.)
 * @param imeAction Action button on keyboard (done, next, search, etc.)
 * @param onImeAction Callback when IME action button is pressed
 * @param isPassword Whether this is a password field (shows/hides text)
 * @param enabled Whether the field is enabled for input
 * @param readOnly Whether the field is read-only
 * @param singleLine Whether the field should be single line
 * @param maxLines Maximum number of lines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NanoSonicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            trailingIcon = {
                when {
                    // Show error icon if there's an error
                    isError -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    // Show visibility toggle for password fields
                    isPassword -> {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    }
                }
            },
            isError = isError,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
                onGo = { onImeAction() },
                onSearch = { onImeAction() },
                onSend = { onImeAction() }
            ),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            shape = MaterialTheme.shapes.medium
        )

        // Show error message or helper text
        when {
            isError && errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            helperText != null -> {
                Text(
                    text = helperText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CONVENIENCE VARIANTS
// ═══════════════════════════════════════════════════════════════

/**
 * Email text field variant
 */
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    NanoSonicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        leadingIcon = Icons.Default.Email,
        keyboardType = KeyboardType.Email,
        imeAction = imeAction,
        onImeAction = onImeAction,
        isError = isError,
        errorMessage = errorMessage
    )
}

/**
 * Password text field variant
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    NanoSonicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isPassword = true,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        isError = isError,
        errorMessage = errorMessage
    )
}

/**
 * Username text field variant
 */
@Composable
fun UsernameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Username",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    NanoSonicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        leadingIcon = Icons.Default.Person,
        keyboardType = KeyboardType.Text,
        imeAction = imeAction,
        onImeAction = onImeAction,
        isError = isError,
        errorMessage = errorMessage
    )
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun NanoSonicTextFieldPreview() {
    NanoSonicProjectTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Normal text field
            var text1 by remember { mutableStateOf("") }
            NanoSonicTextField(
                value = text1,
                onValueChange = { text1 = it },
                label = "Normal Field",
                placeholder = "Enter text here",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Email field
            var email by remember { mutableStateOf("") }
            EmailTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Password field
            var password by remember { mutableStateOf("") }
            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Username field
            var username by remember { mutableStateOf("") }
            UsernameTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Error state
            var errorText by remember { mutableStateOf("invalid@") }
            EmailTextField(
                value = errorText,
                onValueChange = { errorText = it },
                isError = true,
                errorMessage = "Invalid email format",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // With helper text
            var helperText by remember { mutableStateOf("") }
            NanoSonicTextField(
                value = helperText,
                onValueChange = { helperText = it },
                label = "With Helper Text",
                helperText = "This is a helper message",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}