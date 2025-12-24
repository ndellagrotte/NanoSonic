package com.denizen.nanosonic.service

import com.denizen.nanosonic.data.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback mode to control behavior at the end of the queue
 */
enum class PlaybackMode {
    /** Continuous playback - loops back to the first track when reaching the end */
    CONTINUOUS,
    /** Album playback - stops after the last track in the album */
    ALBUM
}

/**
 * Interface to decouple MusicPlayerService from ViewModel to prevent Context leaks
 */
interface MusicPlayerController {
    val playbackState: StateFlow<PlaybackState>

    fun playTrack(track: Track, queue: List<Track>, mode: PlaybackMode = PlaybackMode.CONTINUOUS)
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun getCurrentPosition(): Long
}
