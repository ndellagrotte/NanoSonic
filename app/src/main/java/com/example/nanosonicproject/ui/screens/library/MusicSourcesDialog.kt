package com.example.nanosonicproject.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme
import com.example.nanosonicproject.util.PermissionUtil

/**
 * Music Sources Dialog
 * Allows users to choose between System scanning or File picker
 */
@Composable
fun MusicSourcesDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    viewModel: MusicSourcesViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        }
    }

    MusicSourcesDialogContent(
        state = state,
        onDismiss = onDismiss,
        onSave = {
            viewModel.onSaveSettings()
            onSave()
        },
        onSourceTypeSelected = { viewModel.onSourceTypeSelected(it) },
        onFolderToggled = { viewModel.onFolderToggled(it) },
        onRequestPermission = {
            permissionLauncher.launch(PermissionUtil.getAudioPermission())
        }
    )
}

@Composable
private fun MusicSourcesDialogContent(
    state: MusicSourcesState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSourceTypeSelected: (MusicSourceType) -> Unit,
    onFolderToggled: (MusicFolder) -> Unit,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Music Sources",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Load From",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Source type selection
                SourceTypeSelector(
                    selectedType = state.sourceType,
                    onTypeSelected = onSourceTypeSelected,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Description based on selected type
                Text(
                    text = when (state.sourceType) {
                        MusicSourceType.SYSTEM -> "Scan all audio files that Android detects automatically. Faster and more comprehensive."
                        MusicSourceType.FILE_PICKER -> "Load music from specific folders that you select. More control over what's included."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Storage permission banner
                if (!state.hasStoragePermission) {
                    StoragePermissionBanner(
                        onRequestPermission = onRequestPermission,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Folder selection (only for FILE_PICKER)
                if (state.sourceType == MusicSourceType.FILE_PICKER && state.hasStoragePermission) {
                    FolderSelection(
                        folders = state.selectedFolders,
                        isLoading = state.isLoading,
                        onFolderToggled = onFolderToggled
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.isLoading && (state.hasStoragePermission || state.sourceType == MusicSourceType.SYSTEM)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
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
private fun SourceTypeSelector(
    selectedType: MusicSourceType,
    onTypeSelected: (MusicSourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            // System option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == MusicSourceType.SYSTEM,
                        onClick = { onTypeSelected(MusicSourceType.SYSTEM) },
                        role = Role.RadioButton
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == MusicSourceType.SYSTEM,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "System",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalDivider()

            // File picker option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == MusicSourceType.FILE_PICKER,
                        onClick = { onTypeSelected(MusicSourceType.FILE_PICKER) },
                        role = Role.RadioButton
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == MusicSourceType.FILE_PICKER,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "File picker",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StoragePermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRequestPermission() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage access required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Grant permissions to scan your music library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Grant permission",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FolderSelection(
    folders: List<MusicFolder>,
    isLoading: Boolean,
    onFolderToggled: (MusicFolder) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Music Folders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (folders.isEmpty() && !isLoading) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No music folders found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // fixed by using the correct `items` extension function
                items(folders) { folder ->
                    FolderItem(
                        folder = folder,
                        onToggle = { onFolderToggled(folder) }
                    )
                }
            }
        }
    }
}


@Composable
private fun FolderItem(
    folder: MusicFolder,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.trackCount} tracks • ${folder.path}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun MusicSourcesDialogPreview() {
    NanoSonicProjectTheme {
        MusicSourcesDialogContent(
            state = MusicSourcesState(
                sourceType = MusicSourceType.FILE_PICKER,
                hasStoragePermission = true,
                selectedFolders = listOf(
                    MusicFolder("/storage/emulated/0/Music", "Music", 45),
                    MusicFolder("/storage/emulated/0/Download", "Download", 12)
                )
            ),
            onDismiss = {},
            onSave = {},
            onSourceTypeSelected = {},
            onFolderToggled = {},
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MusicSourcesDialogNoPermissionPreview() {
    NanoSonicProjectTheme {
        MusicSourcesDialogContent(
            state = MusicSourcesState(
                sourceType = MusicSourceType.SYSTEM,
                hasStoragePermission = false
            ),
            onDismiss = {},
            onSave = {},
            onSourceTypeSelected = {},
            onFolderToggled = {},
            onRequestPermission = {}
        )
    }
}