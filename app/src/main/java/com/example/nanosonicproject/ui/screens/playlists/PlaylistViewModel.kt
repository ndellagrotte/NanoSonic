package com.example.nanosonicproject.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanosonicproject.data.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Expose a read-only StateFlow for the UI to observe
    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                // The repository now directly returns the list of playlists
                val playlists = playlistRepository.getAllPlaylists()
                _uiState.value = PlaylistUiState.Success(playlists)
            } catch (e: Exception) {
                // If there's an error, post the error state
                _uiState.value = PlaylistUiState.Error("Failed to load playlists: ${e.message}")
            }
        }
    }
}
