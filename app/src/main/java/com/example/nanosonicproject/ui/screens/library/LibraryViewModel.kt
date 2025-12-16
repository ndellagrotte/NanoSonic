package com.example.nanosonicproject.ui.screens.library

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanosonicproject.data.Track
import com.example.nanosonicproject.util.PermissionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for Library Screen - Simplified Version
 * Only scans default Music folder via MediaStore
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        checkPermissionAndScan()
    }

    private fun checkPermissionAndScan() {
        val hasPermission = PermissionUtil.hasAudioPermission(getApplication())
        _state.update { it.copy(hasPermission = hasPermission) }

        if (hasPermission) {
            scanMusicLibrary(isRefresh = false)
        }
    }

    fun checkPermission() {
        val hasPermission = PermissionUtil.hasAudioPermission(getApplication())
        _state.update {
            it.copy(
                hasPermission = hasPermission,
                showPermissionRationale = false
            )
        }

        if (hasPermission) {
            scanMusicLibrary(isRefresh = false)
        }
    }

    fun onPermissionGranted() {
        _state.update { it.copy(hasPermission = true, showPermissionRationale = false) }
        scanMusicLibrary(isRefresh = false)
    }

    fun onPermissionDenied() {
        _state.update {
            it.copy(
                hasPermission = false,
                showPermissionRationale = true
            )
        }
    }

    fun onRefresh() {
        if (_state.value.hasPermission) {
            scanMusicLibrary(isRefresh = true)
        } else {
            checkPermission()
        }
    }

    /**
     * Scan music from default Android Music folder via MediaStore
     */
    private fun scanMusicLibrary(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _state.update {
                    it.copy(
                        isLoading = true,
                        isScanning = true,
                        error = null,
                        isFirstLaunch = false
                    )
                }
            }

            try {
                val tracks = withContext(Dispatchers.IO) {
                    scanTracksFromMediaStore()
                }

                val totalDuration = tracks.sumOf { it.duration }

                _state.update {
                    it.copy(
                        tracks = tracks,
                        totalTracks = tracks.size,
                        totalDuration = totalDuration,
                        isLoading = false,
                        isRefreshing = false,
                        isScanning = false,
                        scanProgress = null,
                        hasScannedBefore = true,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isScanning = false,
                        scanProgress = null,
                        error = "Failed to scan music: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Scan all audio files from MediaStore
     * This automatically includes the default Music folder
     */
    private suspend fun scanTracksFromMediaStore(): List<Track> {
        val contentResolver = getApplication<Application>().contentResolver
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
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TRACK
        )

        // Only get music files
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            val totalTracks = cursor.count
            var currentTrack = 0

            while (cursor.moveToNext()) {
                currentTrack++

                // Update scan progress
                val fileName = cursor.getString(titleColumn) ?: "Unknown"
                _state.update {
                    it.copy(
                        scanProgress = ScanProgress(
                            current = currentTrack,
                            total = totalTracks,
                            currentFile = fileName
                        )
                    )
                }

                // Small delay to show progress
                if (currentTrack % 20 == 0) {
                    delay(50)
                }

                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val size = cursor.getLong(sizeColumn)

                // Extract track number from metadata or filename
                val trackMetadata = cursor.getInt(trackColumn)
                val trackNumber = extractTrackNumber(trackMetadata, filePath)

                // Generate album artwork URI
                val artworkUri = try {
                    ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    ).toString()
                } catch (e: Exception) {
                    null
                }

                tracks.add(
                    Track(
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
                        albumArtUri = artworkUri, // Also assign to legacy field
                        trackNumber = trackNumber
                    )
                )
            }
        }

        return tracks
    }

    fun onErrorDismissed() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Extract track number from metadata or filename
     * @param trackMetadata Track metadata from MediaStore (format: CDTTT where CD is disc, TTT is track)
     * @param filePath Full file path for filename parsing
     * @return Track number or null if not found
     */
    private fun extractTrackNumber(trackMetadata: Int, filePath: String): Int? {
        // First try to extract from metadata
        if (trackMetadata > 0) {
            // MediaStore TRACK field format: CDTTT (e.g., 1003 = disc 1, track 3)
            // We only want the track number (last 3 digits)
            return trackMetadata % 1000
        }

        // Fallback: try to extract from filename
        return extractTrackNumberFromFilename(filePath)
    }

    /**
     * Extract track number from filename patterns like:
     * - "01 - Song Name.mp3"
     * - "01. Song Name.mp3"
     * - "01 Song Name.mp3"
     * - "Track 01 - Song Name.mp3"
     * @param filePath Full file path
     * @return Track number or null if not found
     */
    private fun extractTrackNumberFromFilename(filePath: String): Int? {
        val filename = filePath.substringAfterLast('/')

        // Pattern 1: "01 - Song.mp3" or "01. Song.mp3" or "01 Song.mp3"
        val pattern1 = Regex("""^(\d{1,3})\s*[-.\s]""")
        pattern1.find(filename)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        // Pattern 2: "Track 01 - Song.mp3"
        val pattern2 = Regex("""[Tt]rack\s*(\d{1,3})""")
        pattern2.find(filename)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        return null
    }
}
