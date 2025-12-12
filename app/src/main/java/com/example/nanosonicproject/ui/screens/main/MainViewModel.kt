package com.example.nanosonicproject.ui.screens.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

//---------------------------
// ViewModel for Main Screen
// Manages bottom navigation tab selection
//---------------------------

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    /**
     * Handle tab selection
     */

    fun onTabSelected(tab: MainTab) {
        _state.update { it.copy(selectedTab = tab) }
    }
}