package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models

import kotlinx.serialization.Serializable

/**
 * Represents a single frequency band in a graphic equalizer
 */
@Serializable
data class GraphicEQBand(
    val frequency: Double,  // Frequency in Hz
    val gain: Double        // Gain in dB
)

/**
 * Represents a complete graphic EQ configuration for a headphone
 * Parsed from device_GraphicEQ.txt files
 *
 * This format is compatible with android.media.audiofx.Equalizer
 */
@Serializable
data class GraphicEQ(
    val bands: List<GraphicEQBand>,             // List of frequency-gain pairs
    val metadata: Map<String, String> = emptyMap()  // Additional metadata from file
) {
    /**
     * Get the gain value for a specific frequency band
     * Returns null if the frequency doesn't exist in the profile
     */
    fun getGainForFrequency(frequency: Double): Double? {
        return bands.find { it.frequency == frequency }?.gain
    }

    /**
     * Get all frequencies in this EQ profile
     */
    fun getAllFrequencies(): List<Double> {
        return bands.map { it.frequency }
    }

    /**
     * Get the number of bands in this EQ profile
     */
    fun getBandCount(): Int = bands.size

    /**
     * Interpolate gain value for a frequency that's not exactly in the profile
     * Uses linear interpolation between the two nearest bands
     */
    fun interpolateGain(targetFrequency: Double): Double {
        if (bands.isEmpty()) return 0.0

        // If exact match exists, return it
        getGainForFrequency(targetFrequency)?.let { return it }

        // Find surrounding bands
        val lowerBand = bands.lastOrNull { it.frequency < targetFrequency }
        val upperBand = bands.firstOrNull { it.frequency > targetFrequency }

        return when {
            lowerBand == null -> upperBand?.gain ?: 0.0
            upperBand == null -> lowerBand.gain
            else -> {
                // Linear interpolation
                val freqRatio = (targetFrequency - lowerBand.frequency) /
                               (upperBand.frequency - lowerBand.frequency)
                lowerBand.gain + freqRatio * (upperBand.gain - lowerBand.gain)
            }
        }
    }

    /**
     * Adapt this GraphicEQ to a specific set of equalizer bands
     * This is useful when the device's equalizer has different frequency bands
     * than what's in the profile
     */
    fun adaptToBands(targetFrequencies: List<Double>): GraphicEQ {
        val adaptedBands = targetFrequencies.map { freq ->
            GraphicEQBand(
                frequency = freq,
                gain = interpolateGain(freq)
            )
        }
        return GraphicEQ(
            bands = adaptedBands,
            metadata = metadata
        )
    }
}
