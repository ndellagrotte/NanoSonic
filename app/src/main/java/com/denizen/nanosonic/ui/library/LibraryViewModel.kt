package com.denizen.nanosonic.ui.library

import androidx.lifecycle.ViewModel
import com.denizen.nanosonic.core.model.Album
import com.denizen.nanosonic.core.model.PlaybackMode
import com.denizen.nanosonic.core.model.Track
import com.denizen.nanosonic.data.library.LibraryRepository
import com.denizen.nanosonic.data.library.LibraryState
import com.denizen.nanosonic.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Backs the Library and Albums tabs. */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    val state: StateFlow<LibraryState> = library.state

    private val _permissionDenied = MutableStateFlow(false)
    /** True after the user declined the permission dialog; the UI then offers system settings too. */
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    /** Plays [track] with the whole library (in title order) as a looping queue. */
    fun play(track: Track) {
        val tracks = (state.value as? LibraryState.Loaded)?.tracks ?: return
        playback.playQueue(tracks, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0), PlaybackMode.CONTINUOUS)
    }

    /** Plays [album] from its first track; album playback stops at the end and may be gapless. */
    fun play(album: Album) = playback.playQueue(album.tracks, 0, PlaybackMode.ALBUM)

    fun refresh() = library.refresh()

    /** @param granted result of the permission dialog, or null when the permission may have changed elsewhere. */
    fun onPermissionResult(granted: Boolean?) {
        if (granted == false) _permissionDenied.value = true
        library.onPermissionChanged()
    }
}
