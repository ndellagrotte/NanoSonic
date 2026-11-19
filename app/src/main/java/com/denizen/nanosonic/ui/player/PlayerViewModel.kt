package com.denizen.nanosonic.ui.player

import androidx.lifecycle.ViewModel
import com.denizen.nanosonic.playback.PlaybackConnection
import com.denizen.nanosonic.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val playback: PlaybackConnection) : ViewModel() {
    val state: StateFlow<PlayerUiState> = playback.state

    fun togglePlayPause() = playback.togglePlayPause()
    fun next() = playback.next()
    fun previous() = playback.previous()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
}
