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
    application: Application,
    private val playlistRepository: com.example.nanosonicproject.data.PlaylistRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // Expose playlists from repository
    val playlists = playlistRepository.playlists

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
            MediaStore.Audio.Media.SIZE
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
                        albumArtUri = artworkUri // Also assign to legacy field
                    )
                )
            }
        }

        return tracks
    }

    fun onErrorDismissed() {
        _state.update { it.copy(error = null) }
    }

    // ═══════════════════════════════════════════════════════════════
    // Selection Mode for Playlist Management
    // ═══════════════════════════════════════════════════════════════

    /**
     * Enter selection mode with the first selected track
     */
    fun onTrackLongPress(trackId: String) {
        _state.update {
            it.copy(
                isSelectionMode = true,
                selectedTrackIds = setOf(trackId)
            )
        }
    }

    /**
     * Toggle track selection (add or remove from selection)
     */
    fun onTrackSelected(trackId: String) {
        _state.update { currentState ->
            val newSelection = if (currentState.selectedTrackIds.contains(trackId)) {
                currentState.selectedTrackIds - trackId
            } else {
                currentState.selectedTrackIds + trackId
            }

            // Exit selection mode if no tracks are selected
            if (newSelection.isEmpty()) {
                currentState.copy(
                    isSelectionMode = false,
                    selectedTrackIds = emptySet()
                )
            } else {
                currentState.copy(selectedTrackIds = newSelection)
            }
        }
    }

    /**
     * Exit selection mode and clear all selections
     */
    fun onExitSelectionMode() {
        _state.update {
            it.copy(
                isSelectionMode = false,
                selectedTrackIds = emptySet()
            )
        }
    }

    /**
     * Get currently selected tracks
     */
    fun getSelectedTracks(): List<Track> {
        val selectedIds = _state.value.selectedTrackIds
        return _state.value.tracks.filter { it.id in selectedIds }
    }

    // ═══════════════════════════════════════════════════════════════
    // Playlist Management
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new playlist with the given name
     */
    suspend fun createPlaylist(name: String): com.example.nanosonicproject.data.Playlist {
        return playlistRepository.createPlaylist(name)
    }

    /**
     * Add currently selected tracks to a playlist
     */
    suspend fun addSelectedTracksToPlaylist(playlistId: String) {
        val selectedIds = _state.value.selectedTrackIds.toList()
        if (selectedIds.isNotEmpty()) {
            playlistRepository.addTracksToPlaylist(playlistId, selectedIds)
            // Exit selection mode after adding tracks
            onExitSelectionMode()
        }
    }

    /**
     * Create a new playlist and add currently selected tracks to it
     */
    suspend fun createPlaylistAndAddSelectedTracks(name: String) {
        val newPlaylist = createPlaylist(name)
        addSelectedTracksToPlaylist(newPlaylist.id)
    }
}
