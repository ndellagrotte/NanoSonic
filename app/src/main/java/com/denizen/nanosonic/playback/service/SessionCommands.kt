package com.denizen.nanosonic.playback.service

import android.content.Context
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.denizen.nanosonic.R

/**
 * Custom session commands, shown as extra buttons in the media notification and system controls.
 * They use the overflow slot: Media3 only exports back/forward/overflow buttons as platform custom
 * actions, which is what Android 13+ system media controls display; prev/next keep their own slots.
 */
object SessionCommands {
    val SEEK_BACK = SessionCommand("com.denizen.nanosonic.SEEK_BACK", Bundle.EMPTY)
    val SEEK_FORWARD = SessionCommand("com.denizen.nanosonic.SEEK_FORWARD", Bundle.EMPTY)

    fun mediaButtons(context: Context): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName(context.getString(R.string.seek_back_10))
            .setSessionCommand(SEEK_BACK)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build(),
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
            .setDisplayName(context.getString(R.string.seek_forward_10))
            .setSessionCommand(SEEK_FORWARD)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build(),
    )
}
