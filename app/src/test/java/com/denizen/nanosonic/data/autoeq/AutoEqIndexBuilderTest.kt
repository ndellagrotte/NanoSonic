package com.denizen.nanosonic.data.autoeq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqIndexBuilderTest {
    private val assets = FileAssetSource.fixture()
    private val index = AutoEqIndexBuilder(assets).build()

    private fun entry(source: String, label: String) = index.entries.single { it.source == source && it.label == label }

    @Test
    fun `indexes every profile directory without opening profile files`() {
        assertEquals(10, index.entries.size)
        assertTrue(assets.openedPaths.none { it.endsWith("ParametricEQ.txt") })
    }

    @Test
    fun `resolves rigs in priority order`() {
        // Rig named in the form directory.
        assertEquals("711", entry("crinacle", "64 Audio U12t").rig)
        assertEquals("EARS + 711", entry("crinacle", "Sennheiser HD 600").rig)
        assertEquals("over-ear", entry("crinacle", "Sennheiser HD 600").form)
        // name_index.tsv: last row wins, values trimmed.
        assertEquals("HMS II.3", entry("Rtings", "Bose QuietComfort 45").rig)
        // "ignore" rows are skipped, falling through to Unknown.
        assertEquals("Unknown", entry("Rtings", "Bose QuietComfort 35 II").rig)
        // Legacy sources default to HMS II.3.
        assertEquals("HMS II.3", entry("Innerfidelity", "Sennheiser HD 600 (sample 2)").rig)
        // No rig anywhere.
        assertEquals("Unknown", entry("oratory1990", "Sennheiser HD 600").rig)
    }

    @Test
    fun `asset path points at the profile`() {
        val e = entry("crinacle", "Audio-Technica ATH-IM70")
        assertEquals("autoeqDB/results/crinacle/711 in-ear/Audio-Technica ATH-IM70/Audio-Technica ATH-IM70 ParametricEQ.txt", e.assetPath)
        assets.open(e.assetPath).close()
    }

    @Test
    fun `groups brands models and variants`() {
        assertEquals(listOf("64 Audio", "Audio-Technica", "Bose", "Sennheiser", "Sony"), index.brands.map { it.name })

        val sennheiserModels = index.searchModels("sennheiser", "")
        assertEquals(listOf("Sennheiser HD 600"), sennheiserModels.map { it.name })
        assertEquals(3, sennheiserModels.single().variantCount)

        val sony = index.searchModels("sony", "").single()
        assertEquals("Sony WH-1000XM4", sony.name)
        val variants = index.variants("sony", sony.key)
        assertEquals(2, variants.size)
        assertTrue(variants.all { it.isAncVariant })
    }

    @Test
    fun `search ranks prefix matches first`() {
        assertEquals(listOf("Audio-Technica", "64 Audio"), index.searchBrands("audio").map { it.name })
        assertEquals(listOf("Audio-Technica"), index.searchBrands("audio-tech").map { it.name })
        assertEquals(listOf("Bose QuietComfort 35 II", "Bose QuietComfort 45"), index.searchModels("bose", "quiet").map { it.name })
        assertEquals(listOf("Bose QuietComfort 45"), index.searchModels("bose", "45").map { it.name })
    }

    @Test
    fun `loads a profile`() = kotlinx.coroutines.test.runTest {
        val db = AutoEqDatabase(assets, kotlinx.coroutines.Dispatchers.Unconfined, kotlinx.coroutines.Dispatchers.Unconfined)
        val eq = db.loadEq(entry("oratory1990", "Sennheiser HD 600"))
        assertEquals(-1.0, eq.preampDb, 0.0)
        assertEquals(1, eq.bands.size)
    }
}
