package com.example.nanosonicproject.ui.screens.wizard.databaseUtil.parsers

import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.GraphicEQ
import com.example.nanosonicproject.ui.screens.wizard.databaseUtil.models.GraphicEQBand
import java.io.File

/**
 * Parser for AutoEq GraphicEQ.txt files.
 * These files contain graphic EQ settings compatible with android.media.audiofx.Equalizer
 *
 * File format:
 *   GraphicEQ: 25 -10.0; 40 -8.5; 63 -7.0; 100 -5.2; ... 16000 -2.5
 *
 * Format is: "GraphicEQ: freq1 gain1; freq2 gain2; ..."
 * Where frequencies are in Hz and gains are in dB
 */
object GraphicEQParser {

    /**
     * Parse a GraphicEQ file
     */
    fun parseFile(file: File): GraphicEQ {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }

        return parseText(file.readText())
    }

    /**
     * Parse a GraphicEQ file from a path string
     */
    fun parseFile(filePath: String): GraphicEQ {
        return parseFile(File(filePath))
    }

    /**
     * Parse GraphicEQ text content
     */
    fun parseText(content: String): GraphicEQ {
        val bands = mutableListOf<GraphicEQBand>()
        val metadata = mutableMapOf<String, String>()

        val lines = content.lines()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            when {
                // Parse GraphicEQ line: "GraphicEQ: 25 -10.0; 40 -8.5; ..."
                trimmedLine.startsWith("GraphicEQ:", ignoreCase = true) -> {
                    val eqData = trimmedLine.substringAfter("GraphicEQ:", "").trim()
                    bands.addAll(parseGraphicEQLine(eqData))
                }

                // Store other lines as metadata
                else -> {
                    val parts = trimmedLine.split(":", limit = 2)
                    if (parts.size == 2) {
                        metadata[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        }

        return GraphicEQ(
            bands = bands,
            metadata = metadata
        )
    }

    /**
     * Parse the GraphicEQ data line
     * Format: "25 -10.0; 40 -8.5; 63 -7.0; ..."
     */
    private fun parseGraphicEQLine(eqData: String): List<GraphicEQBand> {
        val bands = mutableListOf<GraphicEQBand>()

        // Split by semicolon to get individual frequency-gain pairs
        val pairs = eqData.split(";")

        for (pair in pairs) {
            val trimmedPair = pair.trim()
            if (trimmedPair.isEmpty()) continue

            try {
                // Split by whitespace to get frequency and gain
                val parts = trimmedPair.split(Regex("\\s+"))

                if (parts.size >= 2) {
                    val frequency = parts[0].toDoubleOrNull()
                    val gain = parts[1].toDoubleOrNull()

                    if (frequency != null && gain != null) {
                        bands.add(
                            GraphicEQBand(
                                frequency = frequency,
                                gain = gain
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("Warning: Failed to parse GraphicEQ pair: $trimmedPair")
                println("Error: ${e.message}")
            }
        }

        return bands
    }

    /**
     * Convert GraphicEQ to a human-readable string
     */
    fun toString(eq: GraphicEQ): String {
        val sb = StringBuilder()
        sb.append("GraphicEQ: ")

        eq.bands.forEachIndexed { index, band ->
            if (index > 0) sb.append("; ")
            sb.append("${band.frequency.toInt()} ${String.format("%.1f", band.gain)}")
        }

        return sb.toString()
    }

    /**
     * Format GraphicEQ for export to file
     */
    fun toFileFormat(eq: GraphicEQ): String {
        val sb = StringBuilder()

        // Add GraphicEQ line
        sb.appendLine(toString(eq))

        // Add metadata
        eq.metadata.forEach { (key, value) ->
            sb.appendLine("$key: $value")
        }

        return sb.toString()
    }

    /**
     * Validate that a GraphicEQ has sensible values
     * Returns list of validation errors (empty if valid)
     */
    fun validate(eq: GraphicEQ): List<String> {
        val errors = mutableListOf<String>()

        if (eq.bands.isEmpty()) {
            errors.add("No EQ bands found")
        }

        // Check for duplicate frequencies
        val frequencies = eq.bands.map { it.frequency }
        val duplicates = frequencies.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate frequencies found: ${duplicates.keys.joinToString(", ")}")
        }

        // Check for unreasonable gain values (typically should be between -20 and +20 dB)
        eq.bands.forEach { band ->
            if (band.gain < -30 || band.gain > 30) {
                errors.add("Unusual gain value at ${band.frequency} Hz: ${band.gain} dB")
            }
        }

        // Check that frequencies are in ascending order
        for (i in 1 until eq.bands.size) {
            if (eq.bands[i].frequency <= eq.bands[i - 1].frequency) {
                errors.add("Frequencies are not in ascending order")
                break
            }
        }

        return errors
    }
}
