package com.denizen.nanosonic.data.autoeq

import com.denizen.nanosonic.core.model.EqBand
import com.denizen.nanosonic.core.model.FilterType
import com.denizen.nanosonic.core.model.ParametricEq

/**
 * Parses Equalizer APO / AutoEq `ParametricEQ.txt` content:
 * ```
 * Preamp: -6.3 dB
 * Filter 1: ON LSC Fc 105 Hz Gain 6.6 dB Q 0.70
 * Filter 2: ON PK Fc 2240 Hz Gain -2.2 dB Q 1.83
 * ```
 * Lines that aren't a preamp or an enabled filter are ignored.
 */
object ParametricEqParser {
    private const val NUMBER = """[-+]?(?:\d+(?:\.\d*)?|\.\d+)"""
    private val PREAMP = Regex("""^Preamp:\s*($NUMBER)\s*dB""", RegexOption.IGNORE_CASE)
    private val FILTER = Regex("""^Filter\s*\d*\s*:\s*(ON|OFF)\s+([A-Z]+)\b(.*)$""", RegexOption.IGNORE_CASE)
    private val FC = Regex("""\bFc\s+($NUMBER)\s*Hz""", RegexOption.IGNORE_CASE)
    private val GAIN = Regex("""\bGain\s+($NUMBER)\s*dB""", RegexOption.IGNORE_CASE)
    private val Q = Regex("""\bQ\s+($NUMBER)""", RegexOption.IGNORE_CASE)

    /** Q used for shelves and pass filters when the file doesn't specify one (Butterworth). */
    const val DEFAULT_Q = 0.707

    fun parse(content: String): ParametricEq {
        var preamp = 0.0
        val bands = mutableListOf<EqBand>()
        for (raw in content.lineSequence()) {
            val line = raw.trim()
            PREAMP.find(line)?.let { preamp = it.groupValues[1].toDouble() }
            parseFilter(line)?.let(bands::add)
        }
        return ParametricEq(preamp, bands)
    }

    private fun parseFilter(line: String): EqBand? {
        val match = FILTER.find(line) ?: return null
        val (state, typeName, params) = match.destructured
        if (!state.equals("ON", ignoreCase = true)) return null
        val type = FilterType.entries.firstOrNull { it.name.equals(typeName, ignoreCase = true) } ?: return null

        val frequency = FC.find(params)?.groupValues?.get(1)?.toDouble() ?: return null
        val gain = GAIN.find(params)?.groupValues?.get(1)?.toDouble()
        val q = Q.find(params)?.groupValues?.get(1)?.toDouble()
        return when (type) {
            FilterType.PK -> EqBand(frequency, gain ?: return null, q ?: return null, type)
            FilterType.LSC, FilterType.HSC -> EqBand(frequency, gain ?: return null, q ?: DEFAULT_Q, type)
            FilterType.LPQ, FilterType.HPQ -> EqBand(frequency, 0.0, q ?: DEFAULT_Q, type)
        }
    }

    /** Returns a user-facing problem with [eq], or null if it is usable. */
    fun validate(eq: ParametricEq): String? {
        if (eq.preampDb !in -50.0..50.0) return "Preamp ${eq.preampDb} dB is outside -50 to 50 dB"
        if (eq.bands.isEmpty()) return "No enabled filters found"
        if (eq.bands.size > MAX_BANDS) return "Too many filters (${eq.bands.size}); the maximum is $MAX_BANDS"
        eq.bands.forEachIndexed { i, band ->
            val n = i + 1
            if (band.frequency <= 0 || band.frequency > 100_000) return "Filter $n: frequency ${band.frequency} Hz is out of range"
            if (band.gain !in -30.0..30.0) return "Filter $n: gain ${band.gain} dB is outside -30 to 30 dB"
            if (band.q <= 0 || band.q > 20) return "Filter $n: Q ${band.q} is outside 0 to 20"
        }
        return null
    }

    const val MAX_BANDS = 20
}
