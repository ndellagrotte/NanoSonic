package com.denizen.nanosonic.data.library

import com.denizen.nanosonic.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLogicTest {
    private fun track(id: Long, title: String, albumId: Long = 1, album: String = "A", track: Int? = null, disc: Int? = null, albumArtist: String? = null) =
        Track(id, title, "Artist $id", album, albumId, albumArtist, 1000, track, disc, 0, 0)

    @Test
    fun `track numbers from MediaStore`() {
        assertEquals(TrackNumbers.Position(3, 1), TrackNumbers.parse(1003, "x.flac"))
        assertEquals(TrackNumbers.Position(7, null), TrackNumbers.parse(7, "x.flac"))
    }

    @Test
    fun `track numbers from file names`() {
        assertEquals(7, TrackNumbers.parse(0, "07 - Song.flac").track)
        assertEquals(12, TrackNumbers.parse(0, "12. Song.mp3").track)
        assertEquals(4, TrackNumbers.parse(0, "Track 04.mp3").track)
        assertEquals(null, TrackNumbers.parse(0, "Song.mp3").track)
    }

    @Test
    fun `albums order tracks by disc, number, then title`() {
        val album = AlbumGrouping.group(
            listOf(
                track(1, "b", track = null),
                track(2, "x", track = 1, disc = 2),
                track(3, "y", track = 2, disc = 1),
                track(4, "z", track = 1, disc = 1),
                track(5, "a", track = null),
            ),
        ).single()
        assertEquals(listOf(4L, 3L, 2L, 5L, 1L), album.tracks.map { it.id })
    }

    @Test
    fun `albums are sorted case-insensitively and use the album artist`() {
        val albums = AlbumGrouping.group(
            listOf(
                track(1, "t", albumId = 1, album = "beta"),
                track(2, "t", albumId = 2, album = "Alpha", albumArtist = "Various"),
                track(3, "t", albumId = 3, album = "Gamma"),
            ),
        )
        assertEquals(listOf("Alpha", "beta", "Gamma"), albums.map { it.title })
        assertEquals("Various", albums[0].artist)
        assertEquals("Artist 1", albums[1].artist)
    }
}
