package com.example.nanosonicproject.service

import com.example.nanosonicproject.data.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface to decouple MusicPlayerService from ViewModel to prevent Context leaks
 */
interface MusicPlayerController {
    val playbackState: StateFlow<PlaybackState>
    
    fun playTrack(track: Track, queue: List<Track>)
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun getCurrentPosition(): Long
}
