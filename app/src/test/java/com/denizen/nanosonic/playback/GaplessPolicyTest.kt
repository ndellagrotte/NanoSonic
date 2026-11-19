package com.denizen.nanosonic.playback

import androidx.media3.common.Player
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.core.model.PlaybackMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GaplessPolicyTest {
    @Test
    fun `gapless decision matrix`() {
        val expected = mapOf(
            (GaplessMode.ENABLED to PlaybackMode.ALBUM) to true,
            (GaplessMode.ENABLED to PlaybackMode.CONTINUOUS) to true,
            (GaplessMode.DISABLED to PlaybackMode.ALBUM) to false,
            (GaplessMode.DISABLED to PlaybackMode.CONTINUOUS) to false,
            (GaplessMode.ALBUMS_ONLY to PlaybackMode.ALBUM) to true,
            (GaplessMode.ALBUMS_ONLY to PlaybackMode.CONTINUOUS) to false,
        )
        expected.forEach { (key, value) -> assertEquals(key.toString(), value, GaplessPolicy.useGapless(key.first, key.second)) }
    }

    @Test
    fun `playback mode round trips through the repeat mode`() {
        PlaybackMode.entries.forEach { assertEquals(it, GaplessPolicy.modeFor(GaplessPolicy.repeatModeFor(it))) }
        assertEquals(Player.REPEAT_MODE_ALL, GaplessPolicy.repeatModeFor(PlaybackMode.CONTINUOUS))
        assertEquals(Player.REPEAT_MODE_OFF, GaplessPolicy.repeatModeFor(PlaybackMode.ALBUM))
    }
}
