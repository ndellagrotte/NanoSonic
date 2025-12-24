package com.denizen.nanosonic.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.compose.SubcomposeAsyncImage
import com.denizen.nanosonic.ui.screens.library.LibraryViewModel
import com.denizen.nanosonic.data.Track
import com.denizen.nanosonic.service.PlaybackMode
import com.denizen.nanosonic.ui.screens.about.AboutDialog
import com.denizen.nanosonic.ui.screens.settings.SettingsDialog

/**
 * Album data class representing a music album with its tracks
 */
data class Album(
    val albumId: String,
    val albumName: String,
    val artist: String,
    val artworkUri: String?,
    val trackCount: Int,
    val tracks: List<Track>
)

/**
 * Main AlbumScreen composable that displays albums in a grid layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onPlayTrack: (Track, List<Track>, PlaybackMode) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Show dialogs
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Group tracks by albumId to create Album objects
    val albums = state.tracks
        .groupBy { it.albumId }
        .map { (albumId, tracks) ->
            // Sort tracks by track number (with fallback to title for tracks without track numbers)
            val sortedTracks = tracks.sortedWith(
                compareBy(
                    { it.trackNumber ?: Int.MAX_VALUE }, // Tracks without numbers go to the end
                    { it.title } // Secondary sort by title for tracks without track numbers
                )
            )

            // Use the first track's album info for the album card
            val firstTrack = sortedTracks.first()
            Album(
                albumId = albumId,
                albumName = firstTrack.album ?: "Unknown Album",
                artist = firstTrack.artist,
                artworkUri = firstTrack.artworkUri,
                trackCount = sortedTracks.size,
                tracks = sortedTracks // Sorted list of tracks for the album
            )
        }
        .sortedBy { it.albumName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Albums")
                        Text(
                            text = "${albums.size} ${if (albums.size == 1) "album" else "albums"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    OverflowMenu(
                        onShowSettings = { showSettingsDialog = true },
                        onShowAbout = { showAboutDialog = true }
                    )
                }
            )
        }
    ) { paddingValues ->
        AlbumScreenContent(
            albums = albums,
            onAlbumClick = { album ->
                // When an album is clicked, play the first track with all album tracks as queue
                if (album.tracks.isNotEmpty()) {
                    onPlayTrack(album.tracks.first(), album.tracks, com.denizen.nanosonic.service.PlaybackMode.ALBUM)
                }
            },
            modifier = Modifier.padding(paddingValues)
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

/**
 * Content composable for the album screen
 */
@Composable
private fun AlbumScreenContent(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        EmptyAlbumScreen(modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(albums) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
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

/**
 * Album card composable displaying album artwork and details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Album artwork with 1:1 aspect ratio
            AlbumArtwork(
                artworkUri = album.artworkUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Album name - bold, single line with ellipsis
            Text(
                text = album.albumName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Artist name - smaller text, gray color
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Track count
            Text(
                text = "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Album artwork component with loading and error states using SubcomposeAsyncImage
 */
@Composable
private fun AlbumArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = artworkUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
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
 * Placeholder for album art when unavailable or loading
 */
@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun EmptyAlbumScreen(
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
                text = "No Albums Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add music files with album tags to your Music folder to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
