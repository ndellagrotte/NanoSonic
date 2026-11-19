package com.denizen.nanosonic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.core.model.ThemeMode
import com.denizen.nanosonic.data.library.LibraryRepository
import com.denizen.nanosonic.data.library.LibraryState
import com.denizen.nanosonic.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gaplessMode: GaplessMode = GaplessMode.ALBUMS_ONLY,
    val hasAudioPermission: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(settings.settings, library.state) { s, lib ->
        SettingsUiState(s.themeMode, s.gaplessMode, hasAudioPermission = lib !is LibraryState.NoPermission)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setGaplessMode(mode: GaplessMode) = settings.setGaplessMode(mode)
    fun onPermissionChanged() = library.onPermissionChanged()
}
