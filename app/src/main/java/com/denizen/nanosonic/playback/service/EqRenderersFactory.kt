package com.denizen.nanosonic.playback.service

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.denizen.nanosonic.playback.eq.EqAudioProcessor

/**
 * Renderers whose audio sink runs [eq] (see [EqAudioSink]). Float output is enabled, so decoders deliver
 * float (keeping 24-bit precision) and the EQ'd float goes to the AudioTrack without reduction to 16-bit.
 */
class EqRenderersFactory(context: Context, private val eq: EqAudioProcessor) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink = EqAudioSink(
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(true)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
            .build(),
        eq,
    )
}
