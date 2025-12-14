package com.example.nanosonicproject.ui.screens.library

import com.example.nanosonicproject.data.Track
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
    
    // Selection mode properties are removed as Playlist functionality is removed
    val isSelectionMode: Boolean = false
    val selectedTrackIds: Set<String> = emptySet()
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
