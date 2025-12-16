package com.example.nanosonicproject.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme
import androidx.compose.ui.platform.LocalUriHandler

/**
 * About Dialog - Placeholder implementation
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Logo Placeholder
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "NanoSonic Logo",
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NanoSonic",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Version 1.2.1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You're powered up, get in there! \uD83C\uDFB5",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Text(
//                    text = "NanoSonic provides professional-grade parametric EQ and an integrated database of device-specific EQ profiles.",
//                    style = MaterialTheme.typography.bodyMedium,
//                    textAlign = TextAlign.Center,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )

                Spacer(modifier = Modifier.height(24.dp))

                // Features Section
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FeatureItem("🎛️", "Custom Parametric EQ support")
                        FeatureItem("🎧", "Device-specific EQ profiles")
                        FeatureItem("📱", "Works 100% offline")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Credits Section
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // AutoEQ attribution
                        val autoEqLink = buildAnnotatedString {
                            append("- EQ profiles provided by ")
                            pushLink(
                                LinkAnnotation.Url(
                                    url = "https://github.com/jaakkopasanen/AutoEq",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline)
                                    )
                                )
                            )
                            append("AutoEQ project")
                            pop()
                        }

                        Text(
                            text = autoEqLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

// Jetpack Compose attribution
                        val jetpackComposeLink = buildAnnotatedString {
                            append("- Built with ")
                            pushLink(
                                LinkAnnotation.Url(
                                    url = "https://developer.android.com/jetpack/compose",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline)
                                    )
                                )
                            )
                            append("Android Jetpack Compose")
                            pop()
                        }

                        Text(
                            text = jetpackComposeLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

// Audio EQ Cookbook attribution
                        val eqCookbookLink = buildAnnotatedString {
                            append("- Biquad filter equations from ")
                            pushLink(
                                LinkAnnotation.Url(
                                    url = "https://webaudio.github.io/Audio-EQ-Cookbook/audio-eq-cookbook.html",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline)
                                    )
                                )
                            )
                            append("Audio EQ Cookbook")
                            pop()
                            append(" by Robert Bristow-Johnson")
                        }

                        Text(
                            text = eqCookbookLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legal Section
                Text(
                    text = "NanoSonic is licenced under the \n GNU General Public License v3.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FeatureItem(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutDialogPreview() {
    NanoSonicProjectTheme {
        AboutDialog(
            onDismiss = {}
        )
    }
}
