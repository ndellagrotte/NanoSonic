package com.denizen.nanosonic.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denizen.nanosonic.R
import com.denizen.nanosonic.core.model.Album
import com.denizen.nanosonic.data.library.LibraryState
import com.denizen.nanosonic.data.library.MediaStoreUris
import com.denizen.nanosonic.ui.components.ArtworkImage
import com.denizen.nanosonic.ui.components.EmptyState
import com.denizen.nanosonic.ui.components.LoadingState

@Composable
fun AlbumsPage(state: LibraryState, onPlay: (Album) -> Unit) {
    when {
        state is LibraryState.Loading -> LoadingState(
            title = stringResource(R.string.library_scanning_title),
            message = stringResource(R.string.library_scanning_message),
        )
        state is LibraryState.Loaded && state.albums.isNotEmpty() -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.albums, key = { it.id }) { album -> AlbumCard(album, onClick = { onPlay(album) }) }
        }
        else -> EmptyState(
            icon = Icons.Default.MusicOff,
            title = stringResource(R.string.albums_empty_title),
            message = stringResource(R.string.albums_empty_message),
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ArtworkImage(
                uri = MediaStoreUris.albumArt(album.id),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.medium,
                placeholderIconSize = 48.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                pluralStringResource(R.plurals.album_track_count, album.tracks.size, album.tracks.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
