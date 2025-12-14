package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models

import kotlinx.serialization.Serializable

/**
 * Filter type enumeration matching AutoEQ conventions
 */
@Serializable
enum class FilterType {
    /** Peaking filter - boosts or cuts around a center frequency */
    PK,
    /** Low-shelf filter - affects frequencies below the cutoff */
    LSC,
    /** High-shelf filter - affects frequencies above the cutoff */
    HSC
}

/**
 * Represents a single band in a FixedBandEQ profile
 */
@Serializable
data class FixedBandEQBand(
    val frequency: Double,  // Center frequency in Hz
    val gain: Double,       // Gain in dB
    val q: Double = 1.41,   // Q factor (bandwidth) - default to sqrt(2) for graphic EQ
    val filterType: FilterType = FilterType.PK,  // Filter type
    val enabled: Boolean = true  // Whether this band is active
)

/**
 * FixedBandEQ profile from AutoEQ
 * Supports PK (peaking), LSC (low-shelf), and HSC (high-shelf) filters
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