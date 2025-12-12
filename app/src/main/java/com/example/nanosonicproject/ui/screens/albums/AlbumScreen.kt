package com.example.nanosonicproject.ui.screens.albums

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.compose.SubcomposeAsyncImage
import com.example.nanosonicproject.ui.screens.library.LibraryViewModel
import com.example.nanosonicproject.data.Track

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
    onPlayTrack: (com.example.nanosonicproject.data.Track, List<com.example.nanosonicproject.data.Track>) -> Unit,
    onShowSettings: () -> Unit = {},
    onShowAbout: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Group tracks by albumId to create Album objects
    val albums = state.tracks
        .groupBy { it.albumId }
        .map { (albumId, tracks) ->
            // Use the first track's album info for the album card
            val firstTrack = tracks.first()
            Album(
                albumId = albumId,
                albumName = firstTrack.album ?: "Unknown Album",
                artist = firstTrack.artist,
                artworkUri = firstTrack.artworkUri,
                trackCount = tracks.size,
                tracks = tracks // <-- This is the complete list of tracks for the album
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
                        onShowSettings = onShowSettings,
                        onShowAbout = onShowAbout
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
                    onPlayTrack(album.tracks.first(), album.tracks)
                }
            },
            modifier = Modifier.padding(paddingValues)
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
