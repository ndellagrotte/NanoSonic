package com.example.nanosonicproject.ui.screens.eq

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nanosonicproject.data.SavedEQProfile
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * EQ Screen - Manage and select EQ profiles
 */
@Composable
fun EqScreen(
    viewModel: EQViewModel = hiltViewModel(),
    onNavigateToWizard: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // File picker for custom EQ import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    // Extract file name from URI
                    val fileName = uri.lastPathSegment ?: "custom_eq.txt"

                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = {
                            showSuccess = true
                        },
                        onError = { error ->
                            showError = error
                        }
                    )
                } else {
                    showError = "Could not read file"
                }
            } catch (e: Exception) {
                showError = "Failed to open file: ${e.message}"
            }
        }
    }

    EqScreenContent(
        profiles = state.profiles,
        activeProfileId = state.activeProfileId,
        onProfileSelected = { viewModel.selectProfile(it) },
        onImportViaWizard = onNavigateToWizard,
        onImportCustomEQ = {
            // Launch file picker for .txt files
            filePickerLauncher.launch("text/plain")
        },
        onDeleteProfile = { viewModel.deleteProfile(it) }
    )

    // Success Snackbar
    if (showSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccess = false
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSuccess = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text("Custom EQ profile imported successfully")
        }
    }

    // Error dialog
    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Import Error") },
            text = { Text(showError ?: "") },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun EqScreenContent(
    profiles: List<SavedEQProfile>,
    activeProfileId: String?,
    onProfileSelected: (String?) -> Unit,
    onImportViaWizard: () -> Unit,
    onImportCustomEQ: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Profile list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)

        ) {
            // Header
            item {
                Text(
                    text = "Equalizer Profiles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Select an EQ Profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose one profile to apply to your music. Select \"No Equalization\" to disable EQ.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // "No Equalization" option (always first)
            item {
                NoEqualizationItem(
                    isSelected = activeProfileId == null,
                    onSelected = { onProfileSelected(null) }
                )
            }

            // Saved EQ profiles with section headers
            val autoEqProfiles = profiles.filter { !it.isCustom }
            val customProfiles = profiles.filter { it.isCustom }

            // AutoEQ profiles section
            if (autoEqProfiles.isNotEmpty()) {
                item {
                    Text(
                        text = "AutoEQ Profiles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                items(autoEqProfiles) { profile ->
                    EQProfileItem(
                        profile = profile,
                        isSelected = activeProfileId == profile.id,
                        onSelected = { onProfileSelected(profile.id) },
                        onDelete = { onDeleteProfile(profile.id) }
                    )
                }
            }

            // Custom profiles section
            if (customProfiles.isNotEmpty()) {
                item {
                    Text(
                        text = "Custom Profiles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                items(customProfiles) { profile ->
                    EQProfileItem(
                        profile = profile,
                        isSelected = activeProfileId == profile.id,
                        onSelected = { onProfileSelected(profile.id) },
                        onDelete = { onDeleteProfile(profile.id) }
                    )
                }
            }

            // Empty state
            if (profiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No EQ profiles yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Import profiles using the + button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button with popup menu
        FloatingActionButton(
            onClick = { showMenu = true },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import EQ Profile"
            )
        }

        // Dropdown menu for import options
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = (-16).dp, y = (-16).dp),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Import via Wizard")
                    }
                },
                onClick = {
                    showMenu = false
                    onImportViaWizard()
                }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text("Import Custom EQ")
                    }
                },
                onClick = {
                    showMenu = false
                    onImportCustomEQ()
                }
            )
        }
    }
}

@Composable
private fun NoEqualizationItem(
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Equalization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Flat frequency response (no EQ applied)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EQProfileItem(
    profile: SavedEQProfile,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Device model name with custom badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.deviceModel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (profile.isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "CUSTOM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (profile.source != "unknown" && profile.source != "Custom Import") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source: ${profile.source} • Rig: ${profile.rig}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${profile.bands.size} frequency bands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete profile",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete \"${profile.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EqScreenPreview() {
    NanoSonicProjectTheme {
        EqScreenContent(
            profiles = emptyList(),
            activeProfileId = null,
            onProfileSelected = {},
            onImportViaWizard = {},
            onImportCustomEQ = {},
            onDeleteProfile = {}
        )
    }
}