package com.example.nanosonicproject.ui.screens.library

import java.util.Locale
import kotlin.text.format

/**
 * UI State for Library Screen
 */
data class LibraryState(
    // Music data
    val tracks: List<Track> = emptyList(),
    val totalTracks: Int = 0,
    val totalDuration: Long = 0,

    // Permission state
    val hasPermission: Boolean = false,
    val showPermissionRationale: Boolean = false,

    // Loading states
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,  // For pull-to-refresh
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress? = null,

    // Error state
    val error: String? = null,

    // First launch state
    val isFirstLaunch: Boolean = true,
    val hasScannedBefore: Boolean = false
) {
    /**
     * True if we should show the empty state
     */
    val showEmptyState: Boolean
        get() = !isLoading && !isScanning && !isRefreshing && tracks.isEmpty() && hasPermission

    /**
     * True if we should show the permission request
     */
    val showPermissionRequest: Boolean
        get() = !hasPermission && !isLoading

    /**
     * True if we should show loading
     */
    val showLoading: Boolean
        get() = isLoading && tracks.isEmpty() && !isRefreshing
}

/**
 * Track model for display
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val duration: Long, // in milliseconds
    val filePath: String,
    val artworkUri: String?,
    val dateAdded: Long,
    val size: Long // file size in bytes
) {
    /**
     * Format duration as MM:SS
     */
    val formattedDuration: String
        get() {
            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000
            // Corrected line: Use String.format with a specific Locale
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    /**
     * Format file size as MB
     */
    val formattedSize: String
        get() {
            val mb = size / (1024 * 1024)
            return "${mb}MB"
        }
}

/**
 * Scan progress information
 */
data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
    val percentage: Float = if (total > 0) (current.toFloat() / total.toFloat()) * 100f else 0f
) {
    val formattedProgress: String
        get() = "$current / $total"
}