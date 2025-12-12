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

    private val _state = MutableStateFlow(MusicSourcesState())
    val state: StateFlow<MusicSourcesState> = _state.asStateFlow()

    init {
        checkPermissions()
        loadCurrentSettings()
    }

    private fun checkPermissions() {
        val hasPermission = PermissionUtil.hasAudioPermission(getApplication())
        _state.update { it.copy(hasStoragePermission = hasPermission) }
    }

    private fun loadCurrentSettings() {
        // TODO: Load saved settings from preferences
        // For now, default to SYSTEM
        _state.update { it.copy(sourceType = MusicSourceType.SYSTEM) }
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
            // TODO: Save settings to preferences
            // TODO: Trigger library rescan based on selected source type

            // For now, just simulate saving
            _state.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(500)
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