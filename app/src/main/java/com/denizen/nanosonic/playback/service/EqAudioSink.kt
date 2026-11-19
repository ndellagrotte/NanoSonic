package com.denizen.nanosonic.playback.service

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import com.denizen.nanosonic.playback.eq.EqAudioProcessor
import java.nio.ByteBuffer

/**
 * Runs [eq] on decoded PCM before it reaches [sink], then hands [sink] 32-bit float.
 *
 * The EQ sits here instead of in `DefaultAudioSink`'s processor chain because that chain runs only after
 * high-resolution PCM has been converted to 16-bit. With float output enabled, `DefaultAudioSink` skips
 * the chain entirely. Formats [eq] can't handle (encoded passthrough, more than two channels) are
 * forwarded untouched.
 */
class EqAudioSink(sink: AudioSink, private val eq: EqAudioProcessor) : ForwardingAudioSink(sink) {
    private var processing = false

    /**
     * EQ output for the buffer being handled. `handleBuffer` gets the same input buffer back until that
     * buffer is fully handled, so each input is processed only once and this is what gets re-offered.
     */
    private var pending: ByteBuffer? = null

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        pending = null
        processing = MimeTypes.AUDIO_RAW == inputFormat.sampleMimeType &&
            eq.configure(AudioFormat(inputFormat)) != AudioFormat.NOT_SET
        if (processing) {
            eq.flush()
            super.configure(inputFormat.buildUpon().setPcmEncoding(C.ENCODING_PCM_FLOAT).build(), specifiedBufferSize, outputChannels)
        } else {
            super.configure(inputFormat, specifiedBufferSize, outputChannels)
        }
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        if (!processing) return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        val output = pending ?: run {
            eq.queueInput(buffer)
            eq.output
        }
        val handled = super.handleBuffer(output, presentationTimeUs, encodedAccessUnitCount)
        pending = if (handled) null else output
        return handled
    }

    override fun flush() {
        pending = null
        if (processing) eq.flush()
        super.flush()
    }

    override fun reset() {
        pending = null
        processing = false
        eq.reset()
        super.reset()
    }
}
