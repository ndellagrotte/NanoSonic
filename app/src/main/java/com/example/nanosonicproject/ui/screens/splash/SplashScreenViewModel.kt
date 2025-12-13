package com.example.nanosonicproject.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nanosonicproject.data.EQProfileRepository
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
    private val eqProfileRepository: EQProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(500)

            // Check if wizard has been completed (guest or registered user)
            val wizardCompleted = eqProfileRepository.isWizardCompleted()

            if (wizardCompleted) {
                // Skip splash/wizard and go directly to main
                _state.update {
                    it.copy(
                        isLoading = false,
                        navigationEvent = NavigationEvent.NavigateToMain
                    )
                }
            } else {
                // Show splash screen options
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onLoginClicked() {
        _state.update {
            it.copy(navigationEvent = NavigationEvent.NavigateToLogin)
        }
    }

    fun onRegisterClicked() {
        _state.update {
            it.copy(navigationEvent = NavigationEvent.NavigateToRegister)
        }
    }
// this doesn't really work sometimes
    fun onGuestClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(500)
            _state.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = NavigationEvent.NavigateAsGuest
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
    object NavigateToLogin : NavigationEvent()
    object NavigateToRegister : NavigationEvent()
    object NavigateAsGuest : NavigationEvent()
    object NavigateToMain : NavigationEvent()
}