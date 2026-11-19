package com.denizen.nanosonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denizen.nanosonic.core.model.ThemeMode
import com.denizen.nanosonic.data.settings.SettingsRepository
import com.denizen.nanosonic.ui.navigation.MainRoute
import com.denizen.nanosonic.ui.navigation.WelcomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.settings.value.themeMode)

    /** Returning users go straight to the main screen; first launch starts with the welcome screen. */
    val startDestination: Any = if (settings.settings.value.appInitialized) MainRoute else WelcomeRoute
}
