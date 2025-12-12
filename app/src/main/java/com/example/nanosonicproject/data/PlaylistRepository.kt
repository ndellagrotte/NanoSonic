package com.example.nanosonicproject.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing playlists
 * Handles creating, updating, deleting playlists and managing track membership
 */
@Singleton
class PlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository 
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nanosonic_playlists",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    companion object {
        private const val KEY_PLAYLISTS = "playlists"
    }

    init {
        loadPlaylists()
    }

    /**
     * Load all playlists from SharedPreferences
     */
    private fun loadPlaylists() {
        try {
            val playlistsJson = prefs.getString(KEY_PLAYLISTS, null)
            if (playlistsJson != null) {
                val loadedPlaylists = json.decodeFromString<List<Playlist>>(playlistsJson)
                _playlists.value = loadedPlaylists
            }
        } catch (e: Exception) {
            println("Error loading playlists: ${e.message}")
            _playlists.value = emptyList()
        }
    }

    /**
     * Save playlists to SharedPreferences
     */
    private suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        val playlistsJson = json.encodeToString( playlists)
        prefs.edit { putString(KEY_PLAYLISTS, playlistsJson) }
        _playlists.value = playlists
    }

    /**
     * Create a new playlist
     */
    suspend fun createPlaylist(name: String): Playlist = withContext(Dispatchers.IO) {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name
        )

        val currentPlaylists = _playlists.value.toMutableList()
        currentPlaylists.add(newPlaylist)
        savePlaylists(currentPlaylists)

        newPlaylist
    }

    /**
     * Update playlist name
     */
    suspend fun updatePlaylistName(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }

        if (index >= 0) {
            currentPlaylists[index] = currentPlaylists[index].copy(
                name = newName,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(currentPlaylists)
        }
    }

    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val currentPlaylists = _playlists.value.toMutableList()
        currentPlaylists.removeAll { it.id == playlistId }
        savePlaylists(currentPlaylists)
    }

    /**
     * Add tracks to a playlist (at the end)
     */
    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }

        if (index >= 0) {
            val playlist = currentPlaylists[index]
            val updatedTrackIds = playlist.trackIds.toMutableList()

            // Add tracks that aren't already in the playlist
            trackIds.forEach { trackId ->
                if (!updatedTrackIds.contains(trackId)) {
                    updatedTrackIds.add(trackId)
                }
            }

            currentPlaylists[index] = playlist.copy(
                trackIds = updatedTrackIds,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(currentPlaylists)
        }
    }

    /**
     * Remove tracks from a playlist
     */
    suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }

        if (index >= 0) {
            val playlist = currentPlaylists[index]
            val updatedTrackIds = playlist.trackIds.toMutableList()
            updatedTrackIds.removeAll(trackIds.toSet())

            currentPlaylists[index] = playlist.copy(
                trackIds = updatedTrackIds,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(currentPlaylists)
        }
    }

    /**
     * Reorder tracks in a playlist
     */
    suspend fun reorderPlaylistTracks(playlistId: String, newOrder: List<String>) = withContext(Dispatchers.IO) {
        val currentPlaylists = _playlists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }

        if (index >= 0) {
            currentPlaylists[index] = currentPlaylists[index].copy(
                trackIds = newOrder,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(currentPlaylists)
        }
    }

    /**
     * Get a specific playlist by ID
     */
    fun getPlaylist(playlistId: String): Playlist? {
        return _playlists.value.find { it.id == playlistId }
    }

    /**
     * Get all playlists
     */
    fun getAllPlaylists(): List<Playlist> {
        return _playlists.value
    }

    /**
     * Get all tracks for a given playlist
     */
    suspend fun getPlaylistTracks(playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        val playlist = getPlaylist(playlistId)
        if (playlist != null) {
            return@withContext musicRepository.getTracksByIds(playlist.trackIds)
        }
        return@withContext emptyList()
    }

    /**
     * Clear all playlists (for testing/debugging)
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
        _playlists.value = emptyList()
    }
}