package com.example.nanosonicproject.data

import java.util.Locale

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: String,
    val duration: Long, // in milliseconds
    val filePath: String,
    val artworkUri: String?,
    val dateAdded: Long?,
    val size: Long?, // file size in bytes
    val albumArtUri: String?, // Kept for backward compatibility
    val trackNumber: Int? = null // Track number from metadata or filename
)

/**
 * Returns the track duration in a user-friendly "MM:SS" format.
 */
val Track.formattedDuration: String
    get() {
        // Ensure duration is not negative
        if (duration < 0) return "0:00"
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
