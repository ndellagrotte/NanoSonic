package com.denizen.nanosonic.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.denizen.nanosonic.core.model.Track
import com.denizen.nanosonic.data.library.MediaStoreUris

object MediaItemMapper {
    /** The media id is the MediaStore id; the service resolves the playable URI from it. */
    fun toMediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(MediaStoreUris.audio(track.id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setAlbumArtist(track.albumArtist)
                .setArtworkUri(MediaStoreUris.albumArt(track.albumId))
                .setTrackNumber(track.trackNumber)
                .setDiscNumber(track.discNumber)
                .setDurationMs(track.durationMs)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}
