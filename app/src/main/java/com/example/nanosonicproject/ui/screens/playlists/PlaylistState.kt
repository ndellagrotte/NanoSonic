package com.example.nanosonicproject.ui.screens.playlists

import com.example.nanosonicproject.data.Playlist

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
    data class Success(val playlists: List<Playlist>) : PlaylistUiState
    data class Error(val message: String?) : PlaylistUiState
}
