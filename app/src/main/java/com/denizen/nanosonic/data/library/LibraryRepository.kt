package com.denizen.nanosonic.data.library

import com.denizen.nanosonic.core.model.Album
import com.denizen.nanosonic.core.model.Track
import kotlinx.coroutines.flow.StateFlow

sealed interface LibraryState {
    data object NoPermission : LibraryState
    data object Loading : LibraryState
    data class Loaded(
        val tracks: List<Track>,
        val albums: List<Album>,
        val isRefreshing: Boolean = false,
    ) : LibraryState
    data class Error(val message: String) : LibraryState
}

/** The device's music library. Scans automatically once permission is granted and when files change. */
interface LibraryRepository {
    val state: StateFlow<LibraryState>

    /** Call after the audio permission may have changed (grant dialog, return from system settings). */
    fun onPermissionChanged()

    /** Rescans the library. */
    fun refresh()
}
