package com.denizen.nanosonic.playback.service

import android.content.Context
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.denizen.nanosonic.R

/** Custom session commands, shown as extra buttons in the media notification and system controls. */
object SessionCommands {
    val SEEK_BACK = SessionCommand("com.denizen.nanosonic.SEEK_BACK", Bundle.EMPTY)
    val SEEK_FORWARD = SessionCommand("com.denizen.nanosonic.SEEK_FORWARD", Bundle.EMPTY)

    fun mediaButtons(context: Context): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName(context.getString(R.string.seek_back_10))
            .setSessionCommand(SEEK_BACK)
            .setSlots(CommandButton.SLOT_BACK_SECONDARY)
            .build(),
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
            .setDisplayName(context.getString(R.string.seek_forward_10))
            .setSessionCommand(SEEK_FORWARD)
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
            .build(),
    )
}
