package com.denizen.nanosonic.playback.eq

import com.denizen.nanosonic.core.model.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiquadDesignTest {
    private val rates = listOf(44_100, 48_000, 96_000)

    private fun response(type: FilterType, fs: Int, f0: Double, gain: Double, q: Double, at: Double) =
        BiquadDesign.magnitudeDb(BiquadDesign.design(type, fs, f0, gain, q), fs, at)

    @Test
    fun `peaking filter has its gain at the centre and none far away`() = rates.forEach { fs ->
        assertEquals(6.0, response(FilterType.PK, fs, 1000.0, 6.0, 1.41, 1000.0), 0.01)
        assertEquals(-4.5, response(FilterType.PK, fs, 3000.0, -4.5, 2.0, 3000.0), 0.01)
        assertEquals(0.0, response(FilterType.PK, fs, 1000.0, 6.0, 1.41, 1.0), 0.01)
        assertEquals(0.0, response(FilterType.PK, fs, 1000.0, 6.0, 1.41, fs / 2.0 - 1), 0.05)
    }

    @Test
    fun `shelves`() = rates.forEach { fs ->
        assertEquals(6.5, response(FilterType.LSC, fs, 105.0, 6.5, 0.7, 1.0), 0.01)
        assertEquals(0.0, response(FilterType.LSC, fs, 105.0, 6.5, 0.7, fs / 2.0 - 1), 0.01)
        assertEquals(3.25, response(FilterType.LSC, fs, 105.0, 6.5, 0.7, 105.0), 0.01) // half gain at f0
        assertEquals(-3.1, response(FilterType.HSC, fs, 10_000.0, -3.1, 0.7, fs / 2.0 - 1), 0.05)
        assertEquals(0.0, response(FilterType.HSC, fs, 10_000.0, -3.1, 0.7, 10.0), 0.01)
    }

    @Test
    fun `pass filters are -3 dB at the cutoff with Butterworth Q`() = rates.forEach { fs ->
        assertEquals(-3.01, response(FilterType.LPQ, fs, 5000.0, 0.0, 0.7071, 5000.0), 0.01)
        assertEquals(0.0, response(FilterType.LPQ, fs, 5000.0, 0.0, 0.7071, 1.0), 0.01)
        assertTrue(response(FilterType.LPQ, fs, 5000.0, 0.0, 0.7071, 20_000.0) < -20)
        assertEquals(-3.01, response(FilterType.HPQ, fs, 50.0, 0.0, 0.7071, 50.0), 0.01)
        assertTrue(response(FilterType.HPQ, fs, 50.0, 0.0, 0.7071, 5.0) < -30)
    }

    @Test
    fun `filters are stable`() = rates.forEach { fs ->
        for (type in FilterType.entries) {
            for (f in listOf(20.0, 1000.0, fs / 2.0 * 0.95)) {
                for (q in listOf(0.3, 0.707, 5.0)) {
                    assertTrue("$type $f $q", BiquadDesign.isStable(BiquadDesign.design(type, fs, f, 12.0, q)))
                }
            }
        }
    }
}
