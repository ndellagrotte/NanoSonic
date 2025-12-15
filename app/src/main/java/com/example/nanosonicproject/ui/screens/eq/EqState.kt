package com.example.nanosonicproject.ui.screens.eq

import com.example.nanosonicproject.data.SavedEQProfile

/**
 * UI State for EQ Screen
 */
data class EQState(
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null
)
