package com.denizen.nanosonic.core.model

/** Persisted by [name]; never rename entries. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Persisted by [name]; never rename entries. */
enum class GaplessMode {
    ALBUMS_ONLY, ENABLED, DISABLED;

    companion object {
        fun fromStorage(value: String?): GaplessMode = entries.firstOrNull { it.name == value } ?: ALBUMS_ONLY
    }
}

/**
 * How a queue was started. Library playback loops the whole queue; album playback stops at the end
 * and is the trigger for gapless playback in [GaplessMode.ALBUMS_ONLY].
 */
enum class PlaybackMode { CONTINUOUS, ALBUM }
