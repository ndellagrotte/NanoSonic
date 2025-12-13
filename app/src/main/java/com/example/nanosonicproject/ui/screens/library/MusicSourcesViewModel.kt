// ui/screens/library/MusicSourcesViewModel.kt
package com.example.nanosonicproject.ui.screens.library

import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanosonicproject.util.PermissionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Music Sources dialog
 */
@HiltViewModel
class MusicSourcesViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(
        "music_sources_prefs",
        android.content.Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(MusicSourcesState())
    val state: StateFlow<MusicSourcesState> = _state.asStateFlow()

    companion object {
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_SELECTED_FOLDERS = "selected_folders"
    }

    init {
        checkPermissions()
        loadCurrentSettings()
    }

    private fun checkPermissions() {
        val hasPermission = PermissionUtil.hasAudioPermission(getApplication())
        _state.update { it.copy(hasStoragePermission = hasPermission) }
    }

    private fun loadCurrentSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load source type from preferences (default to SYSTEM)
            val sourceTypeStr = prefs.getString(KEY_SOURCE_TYPE, MusicSourceType.SYSTEM.name)
            val sourceType = try {
                MusicSourceType.valueOf(sourceTypeStr ?: MusicSourceType.SYSTEM.name)
            } catch (e: IllegalArgumentException) {
                MusicSourceType.SYSTEM
            }

            // Load selected folders from preferences
            val foldersJson = prefs.getString(KEY_SELECTED_FOLDERS, null)
            val folders = if (foldersJson != null) {
                try {
                    parseFoldersJson(foldersJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            _state.update {
                it.copy(
                    sourceType = sourceType,
                    selectedFolders = folders
                )
            }
        }
    }

    /**
     * Parse folders from JSON string
     */
    private fun parseFoldersJson(json: String): List<MusicFolder> {
        // Simple parsing: format is "path1:name1:count1;path2:name2:count2"
        return json.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 3) {
                    MusicFolder(
                        path = parts[0],
                        name = parts[1],
                        trackCount = parts[2].toIntOrNull() ?: 0
                    )
                } else null
            }
    }

    /**
     * Convert folders to JSON string
     */
    private fun foldersToJson(folders: List<MusicFolder>): String {
        return folders.joinToString(";") { folder ->
            "${folder.path}:${folder.name}:${folder.trackCount}"
        }
    }

    /**
     * Handle source type selection
     */
    fun onSourceTypeSelected(sourceType: MusicSourceType) {
        _state.update { it.copy(sourceType = sourceType) }

        if (sourceType == MusicSourceType.FILE_PICKER) {
            loadAvailableFolders()
        }
    }

    /**
     * Load available music folders from MediaStore
     */
    private fun loadAvailableFolders() {
        if (!_state.value.hasStoragePermission) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val folders = withContext(Dispatchers.IO) {
                scanMusicFolders()
            }

            _state.update {
                it.copy(
                    selectedFolders = folders,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Scan for music folders using MediaStore
     */
    private fun scanMusicFolders(): List<MusicFolder> {
        val contentResolver = getApplication<Application>().contentResolver
        val folders = mutableMapOf<String, Int>()

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataColumn)
                val folder = File(filePath).parent

                if (folder != null) {
                    folders[folder] = folders.getOrDefault(folder, 0) + 1
                }
            }
        }

        return folders.map { (path, count) ->
            MusicFolder(
                path = path,
                name = File(path).name,
                trackCount = count
            )
        }.sortedByDescending { it.trackCount }
    }

    /**
     * Toggle folder selection
     */
    fun onFolderToggled(folder: MusicFolder) {
        // For this simplified version, we'll just update the state
        // In a full implementation, you'd track selected vs available folders
        _state.update { currentState ->
            val isSelected = currentState.selectedFolders.contains(folder)
            val updatedFolders = if (isSelected) {
                currentState.selectedFolders - folder
            } else {
                currentState.selectedFolders + folder
            }
            currentState.copy(selectedFolders = updatedFolders)
        }
    }

    /**
     * Save music sources settings
     */
    fun onSaveSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            withContext(Dispatchers.IO) {
                // Save settings to preferences
                prefs.edit().apply {
                    putString(KEY_SOURCE_TYPE, _state.value.sourceType.name)
                    putString(KEY_SELECTED_FOLDERS, foldersToJson(_state.value.selectedFolders))
                    apply()
                }
            }

            // Note: Library rescan would be triggered by the LibraryViewModel
            // when it observes changes to these settings. For now, settings
            // are saved and will be applied on next app launch or when
            // library screen refreshes.

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Handle permission granted
     */
    fun onPermissionGranted() {
        _state.update { it.copy(hasStoragePermission = true) }
        if (_state.value.sourceType == MusicSourceType.FILE_PICKER) {
            loadAvailableFolders()
        }
    }
}