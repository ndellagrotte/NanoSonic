package com.denizen.nanosonic.playback.service

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.denizen.nanosonic.playback.eq.EqAudioProcessor

/** Renderers whose audio sink runs [eq]. Float output stays off so the EQ always sees 16-bit PCM. */
class EqRenderersFactory(context: Context, private val eq: EqAudioProcessor) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
        .setAudioProcessors(arrayOf(eq))
        .build()
}
