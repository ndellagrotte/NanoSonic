package com.example.nanosonicproject.ui.screens.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.compose.SubcomposeAsyncImage
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme
import com.example.nanosonicproject.util.PermissionUtil
import kotlinx.coroutines.launch
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.data.formattedDuration
import com.example.nanosonicproject.ui.screens.about.AboutDialog
import com.example.nanosonicproject.ui.screens.settings.SettingsDialog

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }, null),
    onPlayTrack: (Track, List<Track>) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Show dialogs
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Permission launcher with enhanced debugging
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("LibraryScreen", "Permission result: $isGranted")
        Log.d("LibraryScreen", "Permission status after request: ${PermissionUtil.debugPermissionStatus(context)}")

        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Settings launcher for when user needs to manually enable permission
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permission status when returning from settings
        viewModel.checkPermission()
    }

    LibraryScreenContent(
        state = state,
        onRequestPermission = {
            val permission = PermissionUtil.getAudioPermission()
            Log.d("LibraryScreen", "Launching permission request for: $permission")
            Log.d("LibraryScreen", "Current permission status: ${PermissionUtil.debugPermissionStatus(context)}")
            permissionLauncher.launch(permission)
        },

        onOpenSettings = {
            // The 'intent' can be created and configured in one go.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            settingsLauncher.launch(intent)
        },

        onRefresh = { viewModel.onRefresh() },
        onPlayTrack = { track -> onPlayTrack(track, state.tracks) },
        onTrackLongPress = { trackId -> viewModel.onTrackLongPress(trackId) },
        onTrackSelected = { trackId -> viewModel.onTrackSelected(trackId) },
        onExitSelectionMode = { viewModel.onExitSelectionMode() },
        onAddToPlaylistClick = { showAddToPlaylistMenu = true },
        onErrorDismissed = { viewModel.onErrorDismissed() },
        onShowSettings = { showSettingsDialog = true },
        onShowAbout = { showAboutDialog = true }
    )

    // Add to Playlist Menu
    if (showAddToPlaylistMenu) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        AddToPlaylistMenu(
            playlists = playlists,
            onDismiss = { showAddToPlaylistMenu = false },
            onPlaylistSelected = { playlistId ->
                coroutineScope.launch {
                    viewModel.addSelectedTracksToPlaylist(playlistId)
                    showAddToPlaylistMenu = false
                }
            },
            onCreatePlaylist = {
                showAddToPlaylistMenu = false
                showCreatePlaylistDialog = true
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { playlistName ->
                coroutineScope.launch {
                    viewModel.createPlaylistAndAddSelectedTracks(playlistName)
                    showCreatePlaylistDialog = false
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenContent(
    state: LibraryState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onTrackLongPress: (String) -> Unit,
    onTrackSelected: (String) -> Unit,
    onExitSelectionMode: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onErrorDismissed: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit
) {
    when {
        state.showPermissionRequest -> {
            PermissionRequestScreen(
                showRationale = state.showPermissionRationale,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        state.isScanning -> {
            ScanningScreen(progress = state.scanProgress)
        }

        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (state.isSelectionMode) "Select songs" else "Library")
                                if (state.isSelectionMode) {
                                    Text(
                                        text = "${state.selectedTrackIds.size} selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (state.tracks.isNotEmpty()) {
                                    Text(
                                        text = "${state.totalTracks} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (state.isSelectionMode) {
                                IconButton(onClick = onExitSelectionMode) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit selection mode"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (state.isSelectionMode) {
                                // Show "Add to playlist" button when in selection mode
                                IconButton(
                                    onClick = onAddToPlaylistClick,
                                    enabled = state.selectedTrackIds.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to playlist"
                                    )
                                }
                            } else {
                                // Three dots overflow menu
                                OverflowMenu(
                                    onShowSettings = onShowSettings,
                                    onShowAbout = onShowAbout
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                if (state.showEmptyState) {
                    EmptyLibraryScreen(
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        TrackList(
                            tracks = state.tracks,
                            isSelectionMode = state.isSelectionMode,
                            selectedTrackIds = state.selectedTrackIds,
                            onPlayTrack = onPlayTrack,
                            onTrackLongPress = onTrackLongPress,
                            onTrackSelected = onTrackSelected
                        )
                    }
                }
            }
        }
    }

    // Error Snackbar
    if (state.error != null) {
        LaunchedEffect(state.error) {
            kotlinx.coroutines.delay(5000)
            onErrorDismissed()
        }
    }
}

@Composable
private fun OverflowMenu(
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    expanded = false
                    onShowSettings()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("About") },
                onClick = {
                    expanded = false
                    onShowAbout()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    showRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Access Your Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (showRationale) {
                    "NanoSonic needs access to your Music folder to display and play your songs with custom EQ profiles."
                } else {
                    "To show your music library, NanoSonic needs access to your Music folder."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Will access: /Music folder only",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showRationale) {
                // Show both options when permission was denied
                Column {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Try Again",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Settings",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If 'Try Again' doesn't work, use 'Open Settings' to manually enable the Audio permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // First time requesting permission
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Permission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ... keep the existing ScanningScreen, EmptyLibraryScreen, TrackList, and TrackItem composables ...

@Composable
private fun ScanningScreen(progress: ScanProgress?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress.percentage / 100f },
                    modifier = Modifier.size(64.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Scanning Music Folder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (progress != null) {
                Text(
                    text = progress.formattedProgress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (progress.currentFile.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progress.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "Finding your music...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Music Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add music files to your Music folder to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    isSelectionMode: Boolean,
    selectedTrackIds: Set<String>,
    onPlayTrack: (Track) -> Unit,
    onTrackLongPress: (String) -> Unit,
    onTrackSelected: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(tracks) { track ->
            TrackItem(
                track = track,
                isSelectionMode = isSelectionMode,
                isSelected = selectedTrackIds.contains(track.id),
                onPlayClick = { onPlayTrack(track) },
                onLongPress = { onTrackLongPress(track.id) },
                onSelectionClick = { onTrackSelected(track.id) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackItem(
    track: Track,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.album} • ${track.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            TrackAlbumArt(
                artworkUri = track.albumArtUri,
                modifier = Modifier.size(56.dp)
            )
        },
        trailingContent = {
            if (isSelectionMode) {
                // Show checkbox in selection mode
                IconButton(onClick = onSelectionClick) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (isSelected) "Deselect" else "Select",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Show play button in normal mode
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = {
                if (isSelectionMode) {
                    onSelectionClick()
                } else {
                    onPlayClick()
                }
            },
            onLongClick = {
                if (!isSelectionMode) {
                    onLongPress()
                }
            }
        )
    )
}

/**
 * Album Art component with fallback icon (using SubcomposeAsyncImage like ns_NowPlayingPanel)
 */
@Composable
private fun TrackAlbumArt(
    artworkUri: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = artworkUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
                error = {
                    AlbumArtPlaceholder()
                },
                loading = {
                    AlbumArtPlaceholder()
                }
            )
        } else {
            AlbumArtPlaceholder()
        }
    }
}

/**
 * Placeholder for album art when unavailable
 */
@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Playlist Management Dialogs
// ═══════════════════════════════════════════════════════════════

/**
 * Menu to select a playlist or create a new one
 */
@Composable
private fun AddToPlaylistMenu(
    playlists: List<com.example.nanosonicproject.data.Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet. Create one to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.trackCount} songs") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.combinedClickable(
                                    onClick = { onPlaylistSelected(playlist.id) }
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Create Playlist Button
                OutlinedButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create new playlist")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to create a new playlist
 */
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create playlist") },
        text = {
            Column {
                Text(
                    text = "Enter a name for your new playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onConfirm(playlistName.trim())
                    }
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun OverflowMenuPreview() {
    NanoSonicProjectTheme {
        OverflowMenu(
            onShowSettings = {},
            onShowAbout = {}
        )
    }
}