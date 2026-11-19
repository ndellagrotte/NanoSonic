package com.denizen.nanosonic.data.library

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.denizen.nanosonic.core.model.Track
import com.denizen.nanosonic.di.AppScope
import com.denizen.nanosonic.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreLibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LibraryRepository {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    override val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var observerRegistered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = scan(debounceMs = CHANGE_DEBOUNCE_MS)
    }

    init {
        onPermissionChanged()
    }

    override fun onPermissionChanged() {
        if (hasPermission()) {
            if (!observerRegistered) {
                context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
                observerRegistered = true
            }
            val scanning = scanJob?.isActive == true
            if (_state.value !is LibraryState.Loaded && !scanning) scan()
        } else {
            scanJob?.cancel()
            _state.value = LibraryState.NoPermission
        }
    }

    override fun refresh() {
        if (hasPermission()) scan() else onPermissionChanged()
    }

    private fun scan(debounceMs: Long = 0) {
        scanJob?.cancel()
        scanJob = scope.launch {
            if (debounceMs > 0) delay(debounceMs)
            _state.update { if (it is LibraryState.Loaded) it.copy(isRefreshing = true) else LibraryState.Loading }
            _state.value = try {
                val tracks = withContext(io) { queryTracks(context.contentResolver) }
                val albums = withContext(io) { AlbumGrouping.group(tracks) }
                LibraryState.Loaded(tracks, albums)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                LibraryState.NoPermission
            } catch (e: Exception) {
                Log.e(TAG, "Library scan failed", e)
                LibraryState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val AUDIO_PERMISSION = Manifest.permission.READ_MEDIA_AUDIO
        private const val TAG = "LibraryRepository"
        private const val CHANGE_DEBOUNCE_MS = 1_000L

        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
        )

        /** All music on the device, sorted by title. */
        fun queryTracks(resolver: ContentResolver): List<Track> {
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val tracks = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION, selection, null, null)
                ?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toTrack()) } }
                .orEmpty()
            val collator = AlbumGrouping.defaultCollator()
            return tracks.sortedWith(compareBy(collator) { it.title })
        }

        private fun Cursor.toTrack(): Track {
            fun str(column: String) = getString(getColumnIndexOrThrow(column))
            fun long(column: String) = getLong(getColumnIndexOrThrow(column))
            val position = TrackNumbers.parse(
                getInt(getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)),
                str(MediaStore.Audio.Media.DISPLAY_NAME).orEmpty(),
            )
            return Track(
                id = long(MediaStore.Audio.Media._ID),
                title = str(MediaStore.Audio.Media.TITLE) ?: str(MediaStore.Audio.Media.DISPLAY_NAME) ?: "Unknown Title",
                artist = str(MediaStore.Audio.Media.ARTIST)?.takeUnless { it == MediaStore.UNKNOWN_STRING } ?: "Unknown Artist",
                album = str(MediaStore.Audio.Media.ALBUM) ?: "Unknown Album",
                albumId = long(MediaStore.Audio.Media.ALBUM_ID),
                albumArtist = str(MediaStore.Audio.Media.ALBUM_ARTIST),
                durationMs = long(MediaStore.Audio.Media.DURATION),
                trackNumber = position.track,
                discNumber = position.disc,
                dateAddedSec = long(MediaStore.Audio.Media.DATE_ADDED),
                sizeBytes = long(MediaStore.Audio.Media.SIZE),
            )
        }
    }
}
