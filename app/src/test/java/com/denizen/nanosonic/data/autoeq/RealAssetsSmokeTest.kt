package com.denizen.nanosonic.data.autoeq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the real bundled database (src/main/assets/autoeqDB) against the indexer and parser. */
class RealAssetsSmokeTest {
    private val root = File("src/main/assets/autoeqDB")
    private val index by lazy { AutoEqIndexBuilder(FileAssetSource(root)).build() }

    @Test
    fun `indexes the whole database`() {
        assertTrue("run from the app module directory", root.isDirectory)
        assertEquals(8_833, index.entries.size)
        assertFalse(index.brands.any { it.name == "Audio" })
        assertTrue(index.brands.any { it.name == "Audio-Technica" })
        assertTrue(index.brands.any { it.name == "64 Audio" })
        assertTrue(index.brands.size > 500)
    }

    @Test
    fun `every profile exists and parses`() {
        val assets = FileAssetSource(root)
        for (entry in index.entries) {
            val eq = assets.open(entry.assetPath).bufferedReader().use { ParametricEqParser.parse(it.readText()) }
            assertNull("${entry.assetPath}: ${ParametricEqParser.validate(eq)}", ParametricEqParser.validate(eq))
        }
    }
}
