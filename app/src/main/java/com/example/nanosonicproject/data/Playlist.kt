package com.example.nanosonicproject.data

import kotlinx.serialization.Serializable

/**
 * Playlist model
 * Stores playlist metadata and list of track IDs (not full Track objects)
 */
@Serializable
data class Playlist(
    val id: String,                          // Unique identifier
    val name: String,                        // Playlist name
    val trackIds: List<String> = emptyList(), // List of track IDs in order
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis()
) {
    /**
     * Get track count
     */
    val trackCount: Int
        get() = trackIds.size
}