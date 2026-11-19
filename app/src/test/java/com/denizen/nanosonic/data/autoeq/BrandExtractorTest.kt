package com.denizen.nanosonic.data.autoeq

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandExtractorTest {
    private fun brand(label: String) = BrandExtractor.brandOf(label)

    @Test
    fun `single word brands`() {
        assertEquals("Sennheiser", brand("Sennheiser HD 600"))
        assertEquals("Sony", brand("Sony WH-1000XM4 (ANC on)"))
        assertEquals("Moondrop", brand("Moondrop"))
    }

    @Test
    fun `hyphenated brands stay whole`() {
        assertEquals("Audio-Technica", brand("Audio-Technica ATH-M50x"))
        assertEquals("Audio Zenith", brand("Audio Zenith PMx2 (v1)"))
    }

    @Test
    fun `generic second words belong to the brand`() {
        assertEquals("64 Audio", brand("64 Audio U12t"))
        assertEquals("Kiwi Ears", brand("Kiwi Ears Cadenza"))
        assertEquals("IMR Acoustics", brand("IMR Acoustics Rhapsody"))
        assertEquals("Tin HiFi", brand("Tin HiFi T2"))
        assertEquals("LZ Hi-Fi", brand("LZ Hi-Fi A7"))
    }

    @Test
    fun `known multi word brands`() {
        assertEquals("Dan Clark Audio", brand("Dan Clark Audio Ether 2"))
        assertEquals("Bowers & Wilkins", brand("Bowers & Wilkins PX7"))
        assertEquals("Bang & Olufsen", brand("Bang & Olufsen Beoplay H95"))
        assertEquals("Tansio Mirai", brand("Tansio Mirai Land"))
    }

    @Test
    fun `brand keys merge different capitalisation`() {
        assertEquals(BrandExtractor.keyOf(brand("1More Triple Driver")), BrandExtractor.keyOf(brand("1MORE Triple Driver")))
    }
}
