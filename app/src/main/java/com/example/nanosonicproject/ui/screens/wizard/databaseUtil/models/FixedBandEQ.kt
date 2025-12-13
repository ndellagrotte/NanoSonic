package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models

import kotlinx.serialization.Serializable

/**
 * Represents a single band in a FixedBandEQ profile
 */
@Serializable
data class FixedBandEQBand(
    val frequency: Double,  // Center frequency in Hz
    val gain: Double,       // Gain in dB
    val enabled: Boolean = true  // Whether this band is active
)

/**
 * FixedBandEQ profile from AutoEQ
 * Uses 10 fixed frequency bands with Q = 1.41
 */
@Serializable
data class FixedBandEQ(
    val preamp: Double,  // Preamp/global gain in dB
    val bands: List<FixedBandEQBand>,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        // Standard 10-band frequencies used by AutoEQ
        val STANDARD_FREQUENCIES = listOf(
            31.0, 62.0, 125.0, 250.0, 500.0,
            1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        )
    }
}
