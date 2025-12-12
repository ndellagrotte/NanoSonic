package com.example.nanosonicproject.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * Settings Dialog - Placeholder implementation
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Theme Settings
                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = "System default",
                        onClick = { /* TODO: Implement theme selection */ }
                    )

                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Follow system setting",
                        onClick = { /* TODO: Implement dark mode toggle */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Settings
                SettingsSection(title = "Audio") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Audio Quality",
                        subtitle = "High (320 kbps)",
                        onClick = { /* TODO: Implement audio quality settings */ }
                    )

                    SettingsItem(
                        icon = Icons.Default.Equalizer,
                        title = "EQ Settings",
                        subtitle = "Manage equalizer profiles",
                        onClick = { /* TODO: Navigate to EQ settings */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data Settings
                SettingsSection(title = "Data & Storage") {
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Settings",
                        subtitle = "Sync EQ profiles across devices",
                        onClick = { /* TODO: Implement sync settings */ }
                    )

                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Storage Usage",
                        subtitle = "Manage app data",
                        onClick = { /* TODO: Show storage usage */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Settings
                SettingsSection(title = "Privacy") {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Permissions",
                        subtitle = "Manage app permissions",
                        onClick = { /* TODO: Open permission settings */ }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDialogPreview() {
    NanoSonicProjectTheme {
        SettingsDialog(
            onDismiss = {}
        )
    }
}