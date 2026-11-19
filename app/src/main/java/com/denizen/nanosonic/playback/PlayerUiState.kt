package com.denizen.nanosonic.playback

import android.net.Uri
import com.denizen.nanosonic.core.model.PlaybackMode

data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
)

data class PlayerUiState(
    val nowPlaying: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val mode: PlaybackMode = PlaybackMode.CONTINUOUS,
)
