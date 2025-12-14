package com.example.nanosonicproject.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing music playback state and service communication
 */
@UnstableApi
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // Store reference to the interface, not the concrete Service class (which is a Context)
    private var musicPlayerController: MusicPlayerController? = null
    private var isBound = false
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        @OptIn(UnstableApi::class)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicPlayerBinder
            // The service implements MusicPlayerController
            musicPlayerController = binder.getService()
            isBound = true

            // Start observing service playback state
            viewModelScope.launch {
                musicPlayerController?.playbackState?.collect { state ->
                    _playbackState.value = state
                }
            }

            // Start position updates
            startPositionUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicPlayerController = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    /**
     * Bind to the MusicPlayerService
     */
    @OptIn(UnstableApi::class)
    private fun bindService() {
        val intent = Intent(getApplication(), MusicPlayerService::class.java)
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Play a track with a queue
     */
    @OptIn(UnstableApi::class)
    fun playTrack(track: com.example.nanosonicproject.data.Track, queue: List<com.example.nanosonicproject.data.Track>) {
        musicPlayerController?.playTrack(track, queue)
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Play/Resume
     */
    @OptIn(UnstableApi::class)
    fun play() {
        musicPlayerController?.play()
    }

    /**
     * Pause
     */
    @OptIn(UnstableApi::class)
    fun pause() {
        musicPlayerController?.pause()
    }

    /**
     * Skip to next track
     */
    @OptIn(UnstableApi::class)
    fun next() {
        musicPlayerController?.next()
    }

    /**
     * Skip to previous track
     */
    @OptIn(UnstableApi::class)
    fun previous() {
        musicPlayerController?.previous()
    }

    /**
     * Seek to position
     */
    @OptIn(UnstableApi::class)
    fun seekTo(positionMs: Long) {
        musicPlayerController?.seekTo(positionMs)
    }

    /**
     * Start periodic position updates
     */
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (_playbackState.value.isPlaying) {
                    val currentPosition = musicPlayerController?.getCurrentPosition() ?: 0L
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = currentPosition
                    )
                }
                delay(500) // Update every 500ms
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
