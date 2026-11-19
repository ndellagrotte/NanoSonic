package com.denizen.nanosonic.core.model

/** A playable audio file from the device's MediaStore. [id] is the MediaStore `_ID`. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val albumArtist: String?,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val dateAddedSec: Long,
    val sizeBytes: Long,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val tracks: List<Track>,
)
