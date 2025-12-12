package com.example.nanosonicproject.ui.screens.library

/**
 * State for Music Sources dialog
 */
data class MusicSourcesState(
    val sourceType: MusicSourceType = MusicSourceType.SYSTEM,
    val selectedFolders: List<MusicFolder> = emptyList(),
    val isLoading: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val showFolderPicker: Boolean = false
)

/**
 * Music source types
 */
enum class MusicSourceType {
    SYSTEM,      // Use MediaStore to scan all audio
    FILE_PICKER  // User selects specific folders
}

/**
 * Represents a music folder
 */
data class MusicFolder(
    val path: String,
    val name: String,
    val trackCount: Int = 0
)