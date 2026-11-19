package com.denizen.nanosonic.ui.library

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denizen.nanosonic.R
import com.denizen.nanosonic.core.model.Track
import com.denizen.nanosonic.core.util.formatDuration
import com.denizen.nanosonic.data.library.LibraryState
import com.denizen.nanosonic.data.library.MediaStoreUris
import com.denizen.nanosonic.ui.components.ArtworkImage
import com.denizen.nanosonic.ui.components.EmptyState
import com.denizen.nanosonic.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPage(
    state: LibraryState,
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Track) -> Unit,
) {
    when (state) {
        LibraryState.NoPermission -> PermissionRequest(permissionDenied, onRequestPermission, onOpenAppSettings)
        LibraryState.Loading -> LoadingState(
            title = stringResource(R.string.library_scanning_title),
            message = stringResource(R.string.library_scanning_message),
        )
        is LibraryState.Error -> EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.library_error_title),
            message = state.message,
            action = { Button(onClick = onRefresh) { Text(stringResource(R.string.action_retry)) } },
        )
        is LibraryState.Loaded -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
            if (state.tracks.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(R.string.library_empty_title),
                    message = stringResource(R.string.library_empty_message),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    items(state.tracks, key = { it.id }) { track -> TrackRow(track, onClick = { onPlay(track) }) }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.track_subtitle, track.album, formatDuration(track.durationMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = { ArtworkImage(uri = MediaStoreUris.albumArt(track.albumId), modifier = Modifier.size(56.dp)) },
        trailingContent = {
            IconButton(onClick = onClick) { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play)) }
        },
    )
}

@Composable
private fun PermissionRequest(denied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(if (denied) R.string.permission_message_rationale else R.string.permission_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.permission_scope), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(32.dp))
            if (denied) {
                WideButton(Icons.Default.Refresh, stringResource(R.string.permission_try_again), onRequest)
                Spacer(Modifier.height(12.dp))
                WideButton(Icons.Default.Settings, stringResource(R.string.permission_open_settings), onOpenSettings, outlined = true)
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.permission_required_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.permission_required_message), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                WideButton(Icons.Default.Check, stringResource(R.string.permission_grant), onRequest)
            }
        }
    }
}

@Composable
private fun WideButton(icon: ImageVector, text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val modifier = Modifier.fillMaxWidth().height(56.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = if (outlined) null else FontWeight.SemiBold)
    }
    if (outlined) OutlinedButton(onClick = onClick, modifier = modifier) { content() }
    else Button(onClick = onClick, modifier = modifier) { content() }
}
