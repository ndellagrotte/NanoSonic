package com.example.nanosonicproject.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.ui.components.NsErrorDialog

@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaylistDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onPlayTrack = { track -> onPlayTrack(track, uiState.tracks) },
        onClearError = viewModel::clearErrorMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailContent(
    uiState: PlaylistDetailUiState,
    onNavigateBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onClearError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.playlist?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                NsErrorDialog(
                    errorMessage = uiState.errorMessage,
                    onDismissRequest = onClearError
                )
            } else {
                LazyColumn {
                    items(uiState.tracks) { track ->
                        TrackListItem(track = track, onClick = { onPlayTrack(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: Track,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(track.title) },
        supportingContent = { Text(track.artist) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
