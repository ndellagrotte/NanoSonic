package com.example.nanosonicproject.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * A reusable error dialog composable.
 *
 * @param errorMessage The message to display in the dialog.
 * @param onDismissRequest The lambda to be invoked when the user wants to dismiss the dialog.
 * @param modifier The modifier to be applied to the dialog.
 * @param showDialog A boolean to control the visibility of the dialog.
 */
@Composable
fun NsErrorDialog(
    errorMessage: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showDialog: Boolean = true,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = "Error")
            },
            text = {
                Text(text = errorMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text(text = "OK")
                }
            },
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NsErrorDialogPreview() {
    NanoSonicProjectTheme {
        NsErrorDialog(
            errorMessage = "This is a sample error message. Please try again later.",
            onDismissRequest = {}
        )
    }
}
