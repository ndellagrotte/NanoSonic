package com.denizen.nanosonic.data.library

import com.denizen.nanosonic.core.model.Album
import com.denizen.nanosonic.core.model.Track
import java.text.Collator

object AlbumGrouping {
    /**
     * Groups tracks by album. Tracks are ordered by disc, then track number (unnumbered last), then
     * title; albums are ordered by title.
     */
    fun group(tracks: List<Track>, collator: Comparator<in String> = defaultCollator()): List<Album> {
        val trackOrder = compareBy<Track>({ it.trackNumber == null }, { it.discNumber ?: 0 }, { it.trackNumber })
            .thenBy(collator) { it.title }
        return tracks.groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                val sorted = albumTracks.sortedWith(trackOrder)
                val first = sorted.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = sorted.firstNotNullOfOrNull { it.albumArtist?.takeIf(String::isNotBlank) } ?: first.artist,
                    tracks = sorted,
                )
            }
            .sortedWith(compareBy(collator) { it.title })
    }

    /** Locale-aware, case-insensitive ordering. */
    fun defaultCollator(): Collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
}
