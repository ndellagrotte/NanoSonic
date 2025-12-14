package com.example.nanosonicproject.service

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
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
import android.os.Bundle
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
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.example.nanosonicproject.MainActivity
import com.example.nanosonicproject.data.EQProfileRepository
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.service.audio.CustomEqualizerAudioProcessor
import com.google.common.collect.ImmutableList
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
 * MediaLibraryService for music playback with Android Auto support
 * Integrates with CustomEqualizerAudioProcessor for multi-band EQ
 * automatically disables EQ during Android Auto sessions
 */
@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {

    @Inject
    lateinit var equalizerService: EqualizerService

    @Inject
    lateinit var eqProfileRepository: EQProfileRepository

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null
    private val binder = MusicPlayerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val equalizerProcessor = CustomEqualizerAudioProcessor()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentQueue: List<Track> = listOf()
    private var currentTrackIndex = -1

    // Android Auto EQ state management
    private var isAndroidAutoConnected = false
    private var savedProfileIdBeforeAuto: String? = null

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
        private const val ROOT_ID = "root"
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
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(equalizerProcessor))
                    .build()
            }
        }

        // Initialize ExoPlayer with custom renderers factory
        exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(playerListener)
            }

        // Create MediaLibrarySession for Android Auto and media controls
        val sessionActivityIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaLibrarySession.Builder(
            this,
            exoPlayer!!,
            MediaLibrarySessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .build()

        // Set the audio processor in EqualizerService
        equalizerService.setAudioProcessor(equalizerProcessor)

        // Observe changes to the active EQ profile
        // Skip EQ changes when Android Auto is connected
        serviceScope.launch {
            eqProfileRepository.activeProfile.collect { profile ->
                if (!isAndroidAutoConnected) {
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
        }

        createNotificationChannel()
    }

    // MediaLibraryService callbacks for Android Auto
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        // For MediaBrowserService connections (Android Auto)
        if (intent?.action == SERVICE_INTERFACE) {
            return super.onBind(intent)
        }
        // For local service binding
        return binder
    }

    /**
     * Check if controller is Android Auto
     */
    private fun isAndroidAutoController(controller: MediaSession.ControllerInfo): Boolean {
        val packageName = controller.packageName
        return packageName.contains("android.car") ||
                packageName.contains("androidauto") ||
                packageName.contains("projection.gearhead")
    }

    /**
     * Handle Android Auto connection
     */
    private fun handleAndroidAutoConnected() {
        if (isAndroidAutoConnected) return // Already connected

        isAndroidAutoConnected = true
        serviceScope.launch {
            val currentProfile = eqProfileRepository.activeProfile.value
            savedProfileIdBeforeAuto = currentProfile?.id
            // Disable EQ for Android Auto
            equalizerService.disable()
        }
    }

    /**
     * Handle Android Auto disconnection
     */
    private fun handleAndroidAutoDisconnected() {
        // Check if any other Android Auto controllers are still connected
        val hasRemainingAaControllers = mediaSession?.connectedControllers?.any { controller ->
            isAndroidAutoController(controller)
        } ?: false

        if (isAndroidAutoConnected && !hasRemainingAaControllers) {
            isAndroidAutoConnected = false
            serviceScope.launch {
                savedProfileIdBeforeAuto?.let { profileId ->
                    eqProfileRepository.setActiveProfile(profileId)
                }
                savedProfileIdBeforeAuto = null
            }
        }
    }


    /**
     * MediaLibrarySession callback for handling playback commands and browsing
     */
    private inner class MediaLibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            if (isAndroidAutoController(controller)) {
                handleAndroidAutoConnected()
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            if (isAndroidAutoController(controller)) {
                handleAndroidAutoDisconnected()
            }
            super.onDisconnected(session, controller)
        }


        // Browsing methods required for Android Auto
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder()
                        .setMediaId(ROOT_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setTitle("NanoSonic")
                                .build()
                        )
                        .build(),
                    params
                )
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // Check if we have permission to read audio files
            if (!hasAudioPermission()) {
                // Return error item prompting user to grant permissions
                val errorItem = MediaItem.Builder()
                    .setMediaId("error_no_permission")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Permission Required")
                            .setArtist("Please grant audio access in the app")
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()

                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.of(errorItem), params)
                )
            }

            // Load tracks with pagination
            val tracks = loadTracksFromMediaStore(page, pageSize)

            if (tracks.isEmpty() && page == 0) {
                // Return error item if no music found on the first page
                val errorItem = MediaItem.Builder()
                    .setMediaId("error_no_music")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("No Music Found")
                            .setArtist("Add music to your device to get started")
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()

                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.of(errorItem), params)
                )
            }

            // Convert tracks to MediaItems for Android Auto browsing
            val mediaItems = tracks.map { track ->
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

            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Skip for error items
            if (mediaId.startsWith("error_")) {
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }

            // Load the specific track by its ID
            val track = loadTrackById(mediaId)

            return if (track != null) {
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
                Futures.immediateFuture(LibraryResult.ofItem(mediaItem, null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
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

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            // Android Auto subscribes to get updates when library changes
            // We accept all subscriptions
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onUnsubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String
        ): ListenableFuture<LibraryResult<Void>> {
            // Handle unsubscribe requests
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            // Android Auto might try to search the library
            // We accept the search request (results would be returned via onGetSearchResult)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // Return search results (for now, just return empty list)
            // Could implement actual search functionality here in the future
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.of(), params)
            )
        }

        // Playback resumption callback for Android Auto
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return empty playlist - Android Auto will request tracks via onAddMediaItems
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
    fun playTrack(track: Track, queue: List<Track>) {
        currentQueue = queue
        currentTrackIndex = queue.indexOfFirst { it.id == track.id }

        if (currentTrackIndex == -1) {
            // Track not in queue, play as single track
            currentQueue = listOf(track)
            currentTrackIndex = 0
        }

        playCurrentTrack()
    }

    /**
     * Play the current track in the queue
     */
    private fun playCurrentTrack() {
        if (currentTrackIndex < 0 || currentTrackIndex >= currentQueue.size) return

        val track = currentQueue[currentTrackIndex]

        exoPlayer?.let { player ->
            // Create MediaItem with metadata for Android Auto
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
    fun play() {
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
    fun pause() {
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
    fun next() {
        if (currentQueue.isEmpty()) return

        currentTrackIndex = (currentTrackIndex + 1) % currentQueue.size
        playCurrentTrack()
    }

    /**
     * Skip to previous track
     */
    fun previous() {
        if (currentQueue.isEmpty()) return

        // If we're more than 3 seconds into the song, restart it
        if ((exoPlayer?.currentPosition ?: 0) > 3000) {
            exoPlayer?.seekTo(0)
        } else {
            // Otherwise, go to previous track
            currentTrackIndex = if (currentTrackIndex - 1 < 0) {
                currentQueue.size - 1
            } else {
                currentTrackIndex - 1
            }
            playCurrentTrack()
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
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    /**
     * Get total duration
     */
    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    /**
     * Check if we have permission to read audio files
     */
    private fun hasAudioPermission(): Boolean {
        val readMediaAudio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val readExternalStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        return readMediaAudio || readExternalStorage
    }

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
     * Load tracks from MediaStore with pagination.
     */
    private fun loadTracksFromMediaStore(page: Int = 0, pageSize: Int = Int.MAX_VALUE): List<Track> {
        val tracks = mutableListOf<Track>()

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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, null)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Audio.Media.DATE_ADDED))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, page * pageSize)
                },
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    tracks.add(createTrackFromCursor(cursor))
                }
            }
        } catch (_: Exception) {
            // Handle errors silently - will return empty list
        }

        return tracks
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
                    // Auto-play next track
                    next()
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
            .setSmallIcon(R.drawable.ic_media_play)
            .setLargeIcon(albumArt)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            // Action 0: Previous track
            .addAction(
                R.drawable.ic_media_previous,
                "Previous",
                previousIntent
            )
            // Action 1: Seek backward (-10s)
            .addAction(
                R.drawable.ic_media_rew,
                "Rewind 10s",
                seekBackwardIntent
            )
            // Action 2: Play/Pause
            .addAction(
                if (_playbackState.value.isPlaying)
                    R.drawable.ic_media_pause
                else
                    R.drawable.ic_media_play,
                if (_playbackState.value.isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            // Action 3: Seek forward (+10s)
            .addAction(
                R.drawable.ic_media_ff,
                "Fast Forward 10s",
                seekForwardIntent
            )
            // Action 4: Next track
            .addAction(
                R.drawable.ic_media_next,
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
