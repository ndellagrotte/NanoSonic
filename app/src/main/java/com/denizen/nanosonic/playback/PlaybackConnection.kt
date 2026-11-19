package com.denizen.nanosonic.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.denizen.nanosonic.core.model.PlaybackMode
import com.denizen.nanosonic.core.model.Track
import com.denizen.nanosonic.di.AppScope
import com.denizen.nanosonic.playback.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's connection to [PlaybackService] through a Media3 [MediaController]. The controller is
 * connected while anything observes [state] or a command is pending, and released shortly after.
 * All access happens on the main thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val scope: CoroutineScope,
) {
    private val controller: SharedFlow<MediaController?> = callbackFlow {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = try {
                    future.get()
                } catch (e: Exception) {
                    Log.e(TAG, "Could not connect to the playback service", e)
                    null
                }
                trySend(connected)
            },
            MoreExecutors.directExecutor(),
        )
        awaitClose { MediaController.releaseFuture(future) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    val state: StateFlow<PlayerUiState> = controller
        .flatMapLatest { it?.uiStates() ?: flowOf(PlayerUiState()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlayerUiState())

    /** Replaces the queue with [tracks] and starts playing at [startIndex]. */
    fun playQueue(tracks: List<Track>, startIndex: Int, mode: PlaybackMode) = command {
        if (tracks.isEmpty()) return@command
        it.shuffleModeEnabled = false
        it.repeatMode = GaplessPolicy.repeatModeFor(mode)
        it.setMediaItems(tracks.map(MediaItemMapper::toMediaItem), startIndex.coerceIn(tracks.indices), 0)
        it.prepare()
        it.play()
    }

    fun togglePlayPause() = command {
        if (it.isPlaying) {
            it.pause()
        } else {
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition(0)
            it.play()
        }
    }

    fun next() = command { it.seekToNext() }

    /** Restarts the current track if it has played for a few seconds, otherwise goes to the previous one. */
    fun previous() = command { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = command { it.seekTo(positionMs) }

    private fun command(block: (MediaController) -> Unit) {
        scope.launch { controller.filterNotNull().first().let(block) }
    }

    private fun MediaController.uiStates(): Flow<PlayerUiState> = callbackFlow {
        // With gapless off the player pauses briefly at the end of each track and the service resumes
        // it. Report that as playing so the UI doesn't flicker.
        var pausedAtItemEnd = false
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                pausedAtItemEnd = !playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
            }

            override fun onEvents(player: Player, events: Player.Events) {
                trySend(snapshot(pausedAtItemEnd && hasNextMediaItem()))
            }
        }
        addListener(listener)
        send(snapshot(false))
        awaitClose { removeListener(listener) }
    }
        .conflate()
        .flatMapLatest { snapshot ->
            if (snapshot.isPlaying) {
                flow {
                    while (true) {
                        emit(snapshot.copy(positionMs = currentPosition))
                        delay(POSITION_UPDATE_MS)
                    }
                }
            } else {
                flowOf(snapshot)
            }
        }

    private fun MediaController.snapshot(pausedAtItemEnd: Boolean): PlayerUiState {
        val item = currentMediaItem
        val metadata = item?.mediaMetadata
        return PlayerUiState(
            nowPlaying = item?.let {
                NowPlaying(
                    mediaId = it.mediaId,
                    title = metadata?.title?.toString().orEmpty(),
                    artist = metadata?.artist?.toString().orEmpty(),
                    artworkUri = metadata?.artworkUri,
                )
            },
            isPlaying = isPlaying || pausedAtItemEnd,
            isBuffering = playbackState == Player.STATE_BUFFERING,
            positionMs = currentPosition,
            durationMs = duration.takeIf { it != C.TIME_UNSET } ?: metadata?.durationMs ?: 0,
            hasNext = hasNextMediaItem(),
            hasPrevious = item != null,
            mode = GaplessPolicy.modeFor(repeatMode),
        )
    }

    private companion object {
        const val TAG = "PlaybackConnection"
        const val STOP_TIMEOUT_MS = 5_000L
        const val POSITION_UPDATE_MS = 500L
    }
}
