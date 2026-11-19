package com.denizen.nanosonic.data.autoeq

import com.denizen.nanosonic.core.model.EqBand
import com.denizen.nanosonic.core.model.FilterType
import com.denizen.nanosonic.core.model.ParametricEq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParametricEqParserTest {

    private val hd600 = """
        Preamp: -6.3 dB
        Filter 1: ON LSC Fc 105 Hz Gain 6.5 dB Q 0.70
        Filter 2: ON PK Fc 125 Hz Gain -2.7 dB Q 0.55
        Filter 3: ON PK Fc 8445 Hz Gain 3.3 dB Q 1.61
        Filter 4: ON PK Fc 522 Hz Gain 0.7 dB Q 1.02
        Filter 5: ON PK Fc 1298 Hz Gain -1.2 dB Q 2.14
        Filter 6: ON HSC Fc 10000 Hz Gain -3.1 dB Q 0.70
        Filter 7: ON PK Fc 3158 Hz Gain -1.8 dB Q 3.67
        Filter 8: ON PK Fc 2166 Hz Gain 0.9 dB Q 3.32
        Filter 9: ON PK Fc 6639 Hz Gain 2.2 dB Q 5.82
        Filter 10: ON PK Fc 5433 Hz Gain -1.2 dB Q 5.70
    """.trimIndent()

    @Test
    fun `parses an AutoEq profile`() {
        val eq = ParametricEqParser.parse(hd600)
        assertEquals(-6.3, eq.preampDb, 0.0)
        assertEquals(10, eq.bands.size)
        assertEquals(EqBand(105.0, 6.5, 0.70, FilterType.LSC), eq.bands[0])
        assertEquals(EqBand(10000.0, -3.1, 0.70, FilterType.HSC), eq.bands[5])
        assertEquals(EqBand(5433.0, -1.2, 5.70, FilterType.PK), eq.bands[9])
        assertNull(ParametricEqParser.validate(eq))
    }

    @Test
    fun `skips disabled filters and unrelated lines`() {
        val eq = ParametricEqParser.parse(
            """
            # comment
            Device: Something
            Filter 1: OFF PK Fc 100 Hz Gain 3 dB Q 1
            Filter 2: ON PK Fc 200 Hz Gain 3 dB Q 1
            Filter: ON PK Fc 300 Hz Gain .5 dB Q 2
            """.trimIndent(),
        )
        assertEquals(listOf(200.0, 300.0), eq.bands.map { it.frequency })
        assertEquals(0.5, eq.bands[1].gain, 0.0)
        assertEquals(0.0, eq.preampDb, 0.0)
    }

    @Test
    fun `pass filters do not need a gain and shelves default their Q`() {
        val eq = ParametricEqParser.parse(
            """
            Filter 1: ON HPQ Fc 20 Hz Q 0.5
            Filter 2: ON LPQ Fc 18000 Hz
            Filter 3: ON LSC Fc 100 Hz Gain 2 dB
            """.trimIndent(),
        )
        assertEquals(EqBand(20.0, 0.0, 0.5, FilterType.HPQ), eq.bands[0])
        assertEquals(EqBand(18000.0, 0.0, ParametricEqParser.DEFAULT_Q, FilterType.LPQ), eq.bands[1])
        assertEquals(EqBand(100.0, 2.0, ParametricEqParser.DEFAULT_Q, FilterType.LSC), eq.bands[2])
    }

    @Test
    fun `rejects filters missing required values`() {
        val eq = ParametricEqParser.parse(
            """
            Filter 1: ON PK Gain 3 dB Q 1
            Filter 2: ON PK Fc 100 Hz Q 1
            Filter 3: ON PK Fc 100 Hz Gain 3 dB
            Filter 4: ON XYZ Fc 100 Hz Gain 3 dB Q 1
            """.trimIndent(),
        )
        assertEquals(emptyList<EqBand>(), eq.bands)
        assertNotNull(ParametricEqParser.validate(eq))
    }

    @Test
    fun `validate checks ranges`() {
        fun band(f: Double = 1000.0, g: Double = 0.0, q: Double = 1.0) = EqBand(f, g, q, FilterType.PK)
        assertNotNull(ParametricEqParser.validate(ParametricEq(-60.0, listOf(band()))))
        assertNotNull(ParametricEqParser.validate(ParametricEq(0.0, listOf(band(f = 0.0)))))
        assertNotNull(ParametricEqParser.validate(ParametricEq(0.0, listOf(band(g = 31.0)))))
        assertNotNull(ParametricEqParser.validate(ParametricEq(0.0, listOf(band(q = 21.0)))))
        assertNotNull(ParametricEqParser.validate(ParametricEq(0.0, List(21) { band() })))
        assertNull(ParametricEqParser.validate(ParametricEq(0.0, List(20) { band() })))
    }
}
