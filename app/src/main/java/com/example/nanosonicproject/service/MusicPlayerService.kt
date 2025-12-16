package com.example.nanosonicproject.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat.Token.fromBundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.nanosonicproject.MainActivity
import com.example.nanosonicproject.data.EQProfileRepository
import com.example.nanosonicproject.data.SettingsRepository
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.service.audio.CustomEqualizerAudioProcessor
import com.example.nanosonicproject.ui.screens.settings.GaplessMode
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MediaSessionService for music playback
 * Integrates with CustomEqualizerAudioProcessor for multi-band EQ
 */
@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService(), MusicPlayerController {

    @Inject
    lateinit var equalizerService: EqualizerService

    @Inject
    lateinit var eqProfileRepository: EQProfileRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val binder = MusicPlayerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val equalizerProcessor = CustomEqualizerAudioProcessor()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentQueue: List<Track> = listOf()
    private var currentTrackIndex = -1
    private var currentPlaybackMode: PlaybackMode = PlaybackMode.CONTINUOUS
    private var useGapless: Boolean = false

    // Audio focus handling
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Could implement ducking here, but for simplicity just continue playing
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Restore volume if ducked, or resume if paused
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "music_playback_channel"
        private const val CHANNEL_NAME = "Music Playback"
        private const val SEEK_INTERVAL_MS = 10000L // 10 seconds

        const val ACTION_PLAY = "com.example.nanosonicproject.action.PLAY"
        const val ACTION_PAUSE = "com.example.nanosonicproject.action.PAUSE"
        const val ACTION_NEXT = "com.example.nanosonicproject.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.nanosonicproject.action.PREVIOUS"
        const val ACTION_SEEK_FORWARD = "com.example.nanosonicproject.action.SEEK_FORWARD"
        const val ACTION_SEEK_BACKWARD = "com.example.nanosonicproject.action.SEEK_BACKWARD"
        const val ACTION_STOP = "com.example.nanosonicproject.action.STOP"
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize audio manager for focus handling
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Create custom renderers factory with our audio processor
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(equalizerProcessor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .build()
            }
        }.setEnableAudioFloatOutput(true)

        // Initialize ExoPlayer with custom renderers factory
        exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(playerListener)
            }

        // Create MediaSession for media controls
        val sessionActivityIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(
            this,
            exoPlayer!!
        )
            .setCallback(MediaSessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // Set the audio processor in EqualizerService
        equalizerService.setAudioProcessor(equalizerProcessor)

        // Observe changes to the active EQ profile
        serviceScope.launch {
            eqProfileRepository.activeProfile.collect { profile ->
                if (profile != null) {
                    equalizerService.applyProfile(profile)
                } else {
                    equalizerService.disable()
                }

                // Force audio pipeline flush by seeking to current position
                // This ensures EQ changes (enable/disable/switch) take effect immediately
                exoPlayer?.let { player ->
                    if (player.playbackState == Player.STATE_READY ||
                        player.playbackState == Player.STATE_BUFFERING) {

                        // Flush the audio processor to clear old buffers
                        equalizerProcessor.flush()

                        // Small delay to ensure state is fully propagated
                        delay(50)

                        // Seek to current position forces ExoPlayer to re-queue audio
                        val currentPos = player.currentPosition
                        player.seekTo(currentPos)
                    }
                }
            }
        }

        createNotificationChannel()
    }

    // MediaSessionService callbacks
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        // For MediaSessionService connections
        if (intent?.action == SERVICE_INTERFACE) {
            return super.onBind(intent)
        }
        // For local service binding
        return binder
    }

    /**
     * MediaSession callback for handling playback commands
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Convert the media items to include full metadata
            val updatedMediaItems = mediaItems.map { mediaItem ->
                // If the mediaItem already has a URI, use it directly
                if (mediaItem.localConfiguration != null) {
                    return@map mediaItem
                }

                // Otherwise, load the track from MediaStore by ID
                val track = loadTrackById(mediaItem.mediaId)

                if (track != null) {
                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(track.filePath.toUri())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setArtworkUri(track.albumArtUri?.toUri())
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .build()
                        )
                        .build()
                } else {
                    mediaItem
                }
            }

            return Futures.immediateFuture(updatedMediaItems)
        }

        // Playback resumption callback
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_SEEK_FORWARD -> seekForward()
            ACTION_SEEK_BACKWARD -> seekBackward()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // Release media session
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        // Cancel coroutine scope
        serviceScope.cancel()

        // Release equalizer
        equalizerService.release()

        // Release ExoPlayer if not already released by mediaSession
        exoPlayer?.release()
        exoPlayer = null

        // Release audio focus, probably
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }

        super.onDestroy()
    }

    /**
     * Play a specific track with a queue
     */
    override fun playTrack(track: Track, queue: List<Track>, mode: PlaybackMode) {
        currentQueue = queue
        currentTrackIndex = queue.indexOfFirst { it.id == track.id }
        currentPlaybackMode = mode

        if (currentTrackIndex == -1) {
            // Track not in queue, play as single track
            currentQueue = listOf(track)
            currentTrackIndex = 0
        }

        // Determine if gapless should be used based on settings and playback mode
        val gaplessMode = settingsRepository.getGaplessMode()
        useGapless = when (gaplessMode) {
            GaplessMode.ENABLED -> true
            GaplessMode.DISABLED -> false
            GaplessMode.ALBUMS_ONLY -> mode == PlaybackMode.ALBUM
        }

        if (useGapless) {
            playQueueWithGapless(currentTrackIndex)
        } else {
            playCurrentTrack()
        }
    }

    /**
     * Play the current track in the queue
     */
    private fun playCurrentTrack() {
        if (currentTrackIndex < 0 || currentTrackIndex >= currentQueue.size) return

        val track = currentQueue[currentTrackIndex]

        exoPlayer?.let { player ->
            // Create MediaItem with metadata
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.filePath.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.albumArtUri?.toUri())
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = true,
                currentPosition = 0L
            )

            startForeground(NOTIFICATION_ID, createNotification(track))
        }
    }

    /**
     * Play queue with gapless playback enabled
     * Builds the entire queue as MediaItems for seamless transitions
     */
    private fun playQueueWithGapless(startIndex: Int) {
        if (startIndex < 0 || startIndex >= currentQueue.size) return

        exoPlayer?.let { player ->
            // Clear existing queue
            player.clearMediaItems()

            // Build MediaItems for entire queue
            val mediaItems = currentQueue.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.filePath.toUri())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(track.albumArtUri?.toUri())
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }

            // Add all items to player
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.play()

            _playbackState.value = _playbackState.value.copy(
                currentTrack = currentQueue[startIndex],
                isPlaying = true,
                currentPosition = 0L
            )

            startForeground(NOTIFICATION_ID, createNotification(currentQueue[startIndex]))
        }
    }

    /**
     * Load album art bitmap from content URI using ContentResolver
     * Same approach as the library screen uses
     */
    private fun loadAlbumArtBitmap(artworkUri: String?): Bitmap? {
        if (artworkUri == null) return null

        return try {
            contentResolver.openInputStream(artworkUri.toUri())?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Play/Resume playback
     */
    override fun play() {
        // Request audio focus
        val audioFocusRequestBuilder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        audioFocusRequest = audioFocusRequestBuilder

        val result = audioManager.requestAudioFocus(audioFocusRequestBuilder)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
            // Some older devices might still use the deprecated method internally
            // and return AUDIOFOCUS_REQUEST_GRANTED with the new API even if
            // they don't support it fully. We also handle the case where
            // ExoPlayer might already have focus.
            exoPlayer?.isPlaying == false
        ) {
            exoPlayer?.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            _playbackState.value.currentTrack?.let { track ->
                startForeground(NOTIFICATION_ID, createNotification(track))
            }
        }
    }

    /**
     * Pause playback
     */
    override fun pause() {
        exoPlayer?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        _playbackState.value.currentTrack?.let { track ->
            startForeground(NOTIFICATION_ID, createNotification(track))
        }
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
    }

    /**
     * Skip to next track
     */
    override fun next() {
        if (currentQueue.isEmpty()) return

        if (useGapless) {
            // Use ExoPlayer's built-in navigation for gapless playback
            exoPlayer?.let { player ->
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    currentTrackIndex = player.currentMediaItemIndex
                    _playbackState.value = _playbackState.value.copy(
                        currentTrack = currentQueue.getOrNull(currentTrackIndex),
                        currentPosition = 0L
                    )
                    currentQueue.getOrNull(currentTrackIndex)?.let { track ->
                        startForeground(NOTIFICATION_ID, createNotification(track))
                    }
                } else {
                    // At end of queue
                    if (currentPlaybackMode == PlaybackMode.ALBUM) {
                        pause()
                    } else {
                        // Loop back to beginning for continuous mode
                        player.seekTo(0, 0L)
                        currentTrackIndex = 0
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = currentQueue.first(),
                            currentPosition = 0L
                        )
                        startForeground(NOTIFICATION_ID, createNotification(currentQueue.first()))
                    }
                }
            }
        } else {
            // Non-gapless mode - use original logic
            val nextIndex = currentTrackIndex + 1

            // Check if we're at the end of the queue
            if (nextIndex >= currentQueue.size) {
                // For album playback, stop at the end
                if (currentPlaybackMode == PlaybackMode.ALBUM) {
                    pause()
                    return
                }
                // For continuous playback, loop back to the beginning
                currentTrackIndex = 0
            } else {
                currentTrackIndex = nextIndex
            }

            playCurrentTrack()
        }
    }

    /**
     * Skip to previous track
     */
    override fun previous() {
        if (currentQueue.isEmpty()) return

        // If we're more than 3 seconds into the song, restart it
        if ((exoPlayer?.currentPosition ?: 0) > 3000) {
            exoPlayer?.seekTo(0)
        } else {
            if (useGapless) {
                // Use ExoPlayer's built-in navigation for gapless playback
                exoPlayer?.let { player ->
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                        currentTrackIndex = player.currentMediaItemIndex
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = currentQueue.getOrNull(currentTrackIndex),
                            currentPosition = 0L
                        )
                        currentQueue.getOrNull(currentTrackIndex)?.let { track ->
                            startForeground(NOTIFICATION_ID, createNotification(track))
                        }
                    } else {
                        // At beginning of queue - wrap to end
                        val lastIndex = currentQueue.size - 1
                        player.seekTo(lastIndex, 0L)
                        currentTrackIndex = lastIndex
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = currentQueue.last(),
                            currentPosition = 0L
                        )
                        startForeground(NOTIFICATION_ID, createNotification(currentQueue.last()))
                    }
                }
            } else {
                // Non-gapless mode - use original logic
                currentTrackIndex = if (currentTrackIndex - 1 < 0) {
                    currentQueue.size - 1
                } else {
                    currentTrackIndex - 1
                }
                playCurrentTrack()
            }
        }
    }

    /**
     * Seek forward by 10 seconds
     */
    fun seekForward() {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition + SEEK_INTERVAL_MS).coerceAtMost(player.duration)
            player.seekTo(newPosition)
        }
    }

    /**
     * Seek backward by 10 seconds
     */
    fun seekBackward() {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition - SEEK_INTERVAL_MS).coerceAtLeast(0L)
            player.seekTo(newPosition)
        }
    }

    /**
     * Seek to a specific position
     */
    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /**
     * Get current playback position
     */
    override fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    /**
     * Get total duration
     */
    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    /**
     * Check if we have permission to read audio files
     */
//    private fun hasAudioPermission(): Boolean {
//        val readMediaAudio = ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.READ_MEDIA_AUDIO
//        ) == PackageManager.PERMISSION_GRANTED
//
//        val readExternalStorage = ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_GRANTED
//
//        return readMediaAudio || readExternalStorage
//    }

    private fun createTrackFromCursor(cursor: Cursor): Track {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

        val id = cursor.getLong(idColumn)
        val title = cursor.getString(titleColumn) ?: "Unknown Title"
        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
        val album = cursor.getString(albumColumn)
        val albumId = cursor.getLong(albumIdColumn)
        val duration = cursor.getLong(durationColumn)
        val filePath = cursor.getString(dataColumn) ?: ""
        val dateAdded = cursor.getLong(dateAddedColumn)
        val size = cursor.getLong(sizeColumn)

        // Generate album artwork URI
        val artworkUri = try {
            ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                albumId
            ).toString()
        } catch (_: Exception) {
            null
        }

        return Track(
            id = id.toString(),
            title = title,
            artist = artist,
            album = album,
            albumId = albumId.toString(),
            duration = duration,
            filePath = filePath,
            artworkUri = artworkUri,
            dateAdded = dateAdded,
            size = size,
            albumArtUri = artworkUri // Also assign to legacy field
        )
    }

    private fun loadTrackById(id: String): Track? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(id)

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToNext()) {
                    return createTrackFromCursor(cursor)
                }
            }
        } catch (_: Exception) {
            // Handle error
        }
        return null
    }

    /**
     * Stop service and release resources
     */
    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Player event listener
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // In gapless mode, ExoPlayer handles transitions automatically
                    // Only call next() for non-gapless mode
                    if (!useGapless) {
                        next()
                    } else {
                        // Check if we're at the end of the queue in album mode
                        exoPlayer?.let { player ->
                            if (!player.hasNextMediaItem() && currentPlaybackMode == PlaybackMode.ALBUM) {
                                pause()
                            }
                        }
                    }
                }
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        duration = getDuration(),
                        isBuffering = false
                    )
                }
                Player.STATE_BUFFERING -> {
                    _playbackState.value = _playbackState.value.copy(isBuffering = true)
                }

                Player.STATE_IDLE -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Track media item changes in gapless mode
            if (useGapless && mediaItem != null) {
                exoPlayer?.let { player ->
                    currentTrackIndex = player.currentMediaItemIndex
                    currentQueue.getOrNull(currentTrackIndex)?.let { track ->
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = track,
                            currentPosition = 0L
                        )
                        startForeground(NOTIFICATION_ID, createNotification(track))
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)

            // Update position periodically while playing
            if (isPlaying) {
                updatePosition()
            }
        }
    }

    /**
     * Update current playback position
     */
    private fun updatePosition() {
        _playbackState.value = _playbackState.value.copy(
            currentPosition = getCurrentPosition()
        )
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Music playback controls"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create notification for foreground service with MediaStyle controls
     */
    @SuppressLint("RestrictedApi")
    private fun createNotification(track: Track): Notification {
        // Load album art bitmap for notification (same as library screen approach)
        val albumArt = loadAlbumArtBitmap(track.albumArtUri)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create pending intents for all controls
        val previousIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MusicPlayerService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_IMMUTABLE
        )

        val seekBackwardIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlayerService::class.java).setAction(ACTION_SEEK_BACKWARD),
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = if (_playbackState.value.isPlaying) {
            PendingIntent.getService(
                this, 2,
                Intent(this, MusicPlayerService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this, 2,
                Intent(this, MusicPlayerService::class.java).setAction(ACTION_PLAY),
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val seekForwardIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlayerService::class.java).setAction(ACTION_SEEK_FORWARD),
            PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicPlayerService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(albumArt)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            // Action 0: Previous track
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousIntent
            )
            // Action 1: Seek backward (-10s)
            .addAction(
                android.R.drawable.ic_media_rew,
                "Rewind 10s",
                seekBackwardIntent
            )
            // Action 2: Play/Pause
            .addAction(
                if (_playbackState.value.isPlaying)
                    android.R.drawable.ic_media_pause
                else
                    android.R.drawable.ic_media_play,
                if (_playbackState.value.isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            // Action 3: Seek forward (+10s)
            .addAction(
                android.R.drawable.ic_media_ff,
                "Fast Forward 10s",
                seekForwardIntent
            )
            // Action 4: Next track
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    // Show Previous, Play/Pause, Next in compact view (lock screen)
                    .setShowActionsInCompactView(0, 2, 4)
                    .setMediaSession(
                        mediaSession?.token?.let { fromBundle(it.toBundle()) }
                    )
            )
            .build()
    }
}

/**
 * Playback state data class
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)
