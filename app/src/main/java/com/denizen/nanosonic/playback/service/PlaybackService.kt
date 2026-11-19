package com.denizen.nanosonic.playback.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.denizen.nanosonic.MainActivity
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.data.eq.EqProfileRepository
import com.denizen.nanosonic.data.library.MediaStoreUris
import com.denizen.nanosonic.data.settings.SettingsRepository
import com.denizen.nanosonic.playback.GaplessPolicy
import com.denizen.nanosonic.playback.eq.EqAudioProcessor
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the player and its media session. The UI and system surfaces (notification, lock screen,
 * Bluetooth) all control playback through the session, so they behave identically. Media3 manages
 * the notification and foreground state.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var eqProfiles: EqProfileRepository
    @Inject lateinit var settings: SettingsRepository

    private val scope = MainScope()
    private val eq = EqAudioProcessor()
    private var session: MediaSession? = null
    private var gaplessMode = GaplessMode.ALBUMS_ONLY

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this, EqRenderersFactory(this, eq))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        player.addListener(GaplessListener(player))

        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setMediaButtonPreferences(SessionCommands.mediaButtons(this))
            .build()

        scope.launch { eqProfiles.activeProfile.collect { eq.setEq(it?.eq) } }
        scope.launch {
            settings.settings.map { it.gaplessMode }.distinctUntilChanged().collect {
                gaplessMode = it
                updatePauseAtEnd(player)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /**
     * Gapless off is implemented by pausing at the end of every item and immediately resuming, which
     * drains the audio output between tracks.
     */
    private fun updatePauseAtEnd(player: ExoPlayer) {
        val gapless = GaplessPolicy.useGapless(gaplessMode, GaplessPolicy.modeFor(player.repeatMode))
        player.pauseAtEndOfMediaItems = !gapless
    }

    private inner class GaplessListener(private val player: ExoPlayer) : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) = updatePauseAtEnd(player)

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                player.hasNextMediaItem()
            ) {
                player.play()
            }
        }
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommands.SEEK_BACK)
                    .add(SessionCommands.SEEK_FORWARD)
                    .build(),
            )
            .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                SessionCommands.SEEK_BACK.customAction -> session.player.seekBack()
                SessionCommands.SEEK_FORWARD.customAction -> session.player.seekForward()
                else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /** Items are identified by MediaStore id; the playable URI is always derived from it. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = Futures.immediateFuture(
            mediaItems.mapNotNull { item ->
                item.mediaId.toLongOrNull()?.let { id -> item.buildUpon().setUri(MediaStoreUris.audio(id)).build() }
            },
        )
    }

    private companion object {
        const val SEEK_INCREMENT_MS = 10_000L
    }
}
