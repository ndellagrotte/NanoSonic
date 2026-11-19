package com.denizen.nanosonic.core.model

import com.denizen.nanosonic.core.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEnumsTest {
    @Test
    fun `stored values map back, unknown values fall back to defaults`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("bogus"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(GaplessMode.DISABLED, GaplessMode.fromStorage("DISABLED"))
        assertEquals(GaplessMode.ALBUMS_ONLY, GaplessMode.fromStorage(null))
    }

    @Test
    fun `durations`() {
        assertEquals("0:00", formatDuration(-5))
        assertEquals("3:07", formatDuration(187_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
    }
}
