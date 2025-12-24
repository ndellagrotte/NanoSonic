package com.denizen.nanosonic.ui.screens.settings

/**
 * Gapless playback mode options
 */
enum class GaplessMode(val displayName: String) {
    /** Gapless playback only for album playback (default) */
    ALBUMS_ONLY("Enabled for Albums (default)"),

    /** Always use gapless playback */
    ENABLED("Enabled"),

    /** Never use gapless playback */
    DISABLED("Disabled")
}
