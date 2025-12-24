package com.denizen.nanosonic.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denizen.nanosonic.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        checkInitializationStatus()
    }

    private fun checkInitializationStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(500)

            // Check if app has been initialized (user completed setup)
            val isInitialized = settingsRepository.isAppInitialized()

            if (isInitialized) {
                // Skip splash and go directly to main screen
                _state.update {
                    it.copy(
                        isLoading = false,
                        navigationEvent = NavigationEvent.NavigateToMain
                    )
                }
            } else {
                // Show splash screen with Get Started button
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onGetStartedClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(300)
            _state.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = NavigationEvent.NavigateToWizard
                )
            }
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigationEvent = NavigationEvent.None) }
    }
}

data class SplashState(
    val isLoading: Boolean = false,
    val navigationEvent: NavigationEvent = NavigationEvent.None,
    val error: String? = null
)

sealed class NavigationEvent {
    object None : NavigationEvent()
    object NavigateToWizard : NavigationEvent()
    object NavigateToMain : NavigationEvent()
}
