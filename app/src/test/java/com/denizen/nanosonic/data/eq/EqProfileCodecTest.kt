package com.denizen.nanosonic.data.eq

import com.denizen.nanosonic.core.model.EqBand
import com.denizen.nanosonic.core.model.EqProfile
import com.denizen.nanosonic.core.model.FilterType
import com.denizen.nanosonic.core.model.ParametricEq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqProfileCodecTest {

    /** Exactly what releases up to 1.2.4 wrote (pretty-printed, default values omitted). */
    private val legacyJson = """
        [
            {
                "id": "sennheiser_hd_600_0",
                "name": "Sennheiser HD 600",
                "deviceModel": "Sennheiser HD 600",
                "source": "oratory1990",
                "rig": "Unknown",
                "bands": [
                    {
                        "frequency": 105.0,
                        "gain": 6.5,
                        "q": 0.7,
                        "filterType": "LSC"
                    },
                    {
                        "frequency": 125.0,
                        "gain": -2.7,
                        "q": 0.55
                    },
                    {
                        "frequency": 1000.0,
                        "gain": 1.0,
                        "enabled": false
                    }
                ],
                "preamp": -6.3,
                "addedTimestamp": 1730000000000
            },
            {
                "id": "custom_1730000000001_42",
                "name": "My EQ",
                "deviceModel": "My EQ",
                "source": "Custom Import",
                "rig": "N/A",
                "bands": [
                    {
                        "frequency": 20.0,
                        "gain": 0.0,
                        "q": 0.5,
                        "filterType": "HPQ"
                    }
                ],
                "isCustom": true,
                "isActive": true,
                "addedTimestamp": 1730000000001,
                "someFutureField": "ignored"
            }
        ]
    """.trimIndent()

    @Test
    fun `decodes data written by earlier releases`() {
        val profiles = EqProfileCodec.decode(legacyJson)
        assertEquals(2, profiles.size)

        val hd600 = profiles[0]
        assertEquals("sennheiser_hd_600_0", hd600.id)
        assertEquals(-6.3, hd600.eq.preampDb, 0.0)
        assertFalse(hd600.isCustom)
        assertEquals(1730000000000, hd600.addedTimestamp)
        assertEquals(
            listOf(
                EqBand(105.0, 6.5, 0.7, FilterType.LSC),
                EqBand(125.0, -2.7, 0.55, FilterType.PK),
                EqBand(1000.0, 1.0, 1.41, FilterType.PK, enabled = false),
            ),
            hd600.eq.bands,
        )

        val custom = profiles[1]
        assertTrue(custom.isCustom)
        assertEquals(0.0, custom.eq.preampDb, 0.0)
        assertEquals(FilterType.HPQ, custom.eq.bands.single().filterType)
    }

    @Test
    fun `round trips and keeps the legacy format`() {
        val profiles = EqProfileCodec.decode(legacyJson)
        val encoded = EqProfileCodec.encode(profiles)
        assertEquals(profiles, EqProfileCodec.decode(encoded))
        // Defaults are omitted and enums are stored by name, as before.
        assertFalse(encoded.contains("\"enabled\": true"))
        assertFalse(encoded.contains("\"isActive\""))
        assertTrue(encoded.contains("\"filterType\": \"LSC\""))
    }

    @Test
    fun `legacy model ids`() {
        assertEquals("sennheiser_hd_600", LegacyProfileIds.modelId("Sennheiser HD 600"))
        assertEquals("sony_wh1000xm4", LegacyProfileIds.modelId("Sony WH-1000XM4 (ANC on)"))
        assertEquals("bowers__wilkins_px7", LegacyProfileIds.modelId("Bowers & Wilkins PX7"))
        assertEquals("sony_wh1000xm4_3", LegacyProfileIds.autoEqProfileId("sony_wh1000xm4", 3))
    }
}
