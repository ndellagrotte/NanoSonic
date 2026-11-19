package com.denizen.nanosonic.playback

import androidx.media3.common.Player
import com.denizen.nanosonic.core.model.GaplessMode
import com.denizen.nanosonic.core.model.PlaybackMode

/**
 * The queue's [PlaybackMode] is stored in the player as its repeat mode, so the service and every
 * controller agree on it: library queues loop ([Player.REPEAT_MODE_ALL]), albums stop at the end.
 */
object GaplessPolicy {
    fun useGapless(gapless: GaplessMode, mode: PlaybackMode): Boolean = when (gapless) {
        GaplessMode.ENABLED -> true
        GaplessMode.DISABLED -> false
        GaplessMode.ALBUMS_ONLY -> mode == PlaybackMode.ALBUM
    }

    fun repeatModeFor(mode: PlaybackMode): Int =
        if (mode == PlaybackMode.ALBUM) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL

    fun modeFor(repeatMode: Int): PlaybackMode =
        if (repeatMode == Player.REPEAT_MODE_OFF) PlaybackMode.ALBUM else PlaybackMode.CONTINUOUS
}
