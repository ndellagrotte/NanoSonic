package com.denizen.nanosonic.data.library

/** Disc and track numbers from MediaStore's TRACK column, with a file-name fallback. */
object TrackNumbers {
    data class Position(val track: Int?, val disc: Int?)

    private val LEADING_NUMBER = Regex("""^(\d{1,3})\s*[-.\s]""")
    private val TRACK_WORD = Regex("""track\s*(\d{1,3})""", RegexOption.IGNORE_CASE)

    /**
     * MediaStore encodes TRACK as `DTTT` (disc × 1000 + track). When it is missing, file names like
     * `01 - Song.flac`, `01. Song.flac` or `Track 01.mp3` are used for the track number.
     */
    fun parse(trackColumn: Int, displayName: String): Position {
        if (trackColumn > 0) {
            val track = trackColumn % 1000
            val disc = trackColumn / 1000
            return Position(track.takeIf { it > 0 }, disc.takeIf { it > 0 })
        }
        val fromName = LEADING_NUMBER.find(displayName) ?: TRACK_WORD.find(displayName)
        return Position(fromName?.groupValues?.get(1)?.toIntOrNull(), null)
    }
}
