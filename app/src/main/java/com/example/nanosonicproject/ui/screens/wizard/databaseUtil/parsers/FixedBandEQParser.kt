package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.parsers

import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.FixedBandEQ
import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.FixedBandEQBand

/**
 * Parser for FixedBandEQ.txt files from AutoEQ project
 * Format:
 * Preamp: -12.1 dB
 * Filter 1: ON PK Fc 31 Hz Gain 3.8 dB Q 1.41
 * Filter 2: ON PK Fc 62 Hz Gain 2.0 dB Q 1.41
 * ...
 */
object FixedBandEQParser {

    /**
     * Parse a FixedBandEQ.txt file content
     */
    fun parseText(content: String): FixedBandEQ {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var preamp = 0.0
        val bands = mutableListOf<FixedBandEQBand>()

        for (line in lines) {
            when {
                line.startsWith("Preamp:", ignoreCase = true) -> {
                    preamp = parsePreamp(line)
                }
                line.startsWith("Filter", ignoreCase = true) -> {
                    parseFilter(line)?.let { bands.add(it) }
                }
            }
        }

        return FixedBandEQ(
            preamp = preamp,
            bands = bands
        )
    }

    /**
     * Parse preamp line: "Preamp: -12.1 dB"
     */
    private fun parsePreamp(line: String): Double {
        val regex = Regex("""Preamp:\s*(-?[\d.]+)\s*dB""", RegexOption.IGNORE_CASE)
        val match = regex.find(line) ?: return 0.0
        return match.groupValues[1].toDoubleOrNull() ?: 0.0
    }

    /**
     * Parse filter line: "Filter 1: ON PK Fc 31 Hz Gain 3.8 dB Q 1.41"
     */
    private fun parseFilter(line: String): FixedBandEQBand? {
        // Regex to extract: ON/OFF, Fc value, Gain value
        val regex = Regex(
            """Filter\s+\d+:\s+(ON|OFF)\s+PK\s+Fc\s+([\d.]+)\s+Hz\s+Gain\s+(-?[\d.]+)\s+dB""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(line) ?: return null

        val enabled = match.groupValues[1].equals("ON", ignoreCase = true)
        val frequency = match.groupValues[2].toDoubleOrNull() ?: return null
        val gain = match.groupValues[3].toDoubleOrNull() ?: return null

        return FixedBandEQBand(
            frequency = frequency,
            gain = gain,
            enabled = enabled
        )
    }

    /**
     * Validate a FixedBandEQ profile
     */
    fun validate(eq: FixedBandEQ): List<String> {
        val errors = mutableListOf<String>()

        // Check preamp range (-20 to +20 dB is reasonable)
        if (eq.preamp < -20.0 || eq.preamp > 20.0) {
            errors.add("Preamp ${eq.preamp} dB is outside reasonable range (-20 to +20 dB)")
        }

        // Check number of bands
        if (eq.bands.isEmpty()) {
            errors.add("No filter bands found")
        }

        if (eq.bands.size > 10) {
            errors.add("Too many bands: ${eq.bands.size} (expected 10)")
        }

        // Validate each band
        eq.bands.forEach { band ->
            // Frequency validation
            if (band.frequency <= 0 || band.frequency > 24000) {
                errors.add("Invalid frequency: ${band.frequency} Hz")
            }

            // Gain validation
            if (band.gain < -20.0 || band.gain > 20.0) {
                errors.add("Gain ${band.gain} dB at ${band.frequency} Hz is outside reasonable range")
            }
        }

        return errors
    }
}
