package com.example.nanosonicproject.ui.screens.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanosonicproject.data.Playlist
import com.example.nanosonicproject.data.PlaylistRepository
import com.example.nanosonicproject.data.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log.e

data class PlaylistDetailUiState(
    val isLoading: Boolean = true,
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val playlistId: String = savedStateHandle.get<String>("playlistId")!!

    init {
        loadPlaylistDetails()
    }

    private fun loadPlaylistDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val playlist = playlistRepository.getPlaylist(playlistId)
                val tracks = playlistRepository.getPlaylistTracks(playlistId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playlist = playlist,
                        tracks = tracks
                    )
                }
            }
            catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load playlist details."
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
