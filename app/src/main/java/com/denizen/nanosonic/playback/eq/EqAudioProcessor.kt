package com.denizen.nanosonic.playback.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import com.denizen.nanosonic.core.model.ParametricEq
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parametric EQ in ExoPlayer's audio sink.
 *
 * [setEq] may be called from any thread: it publishes an immutable snapshot, and the audio thread picks
 * it up at the start of the next buffer, recomputing coefficients for the current sample rate. A change
 * becomes audible once the audio already queued in the AudioTrack has played (a few hundred ms).
 *
 * Only 16-bit PCM with one or two channels is processed. Other formats bypass the EQ. With float output
 * disabled (the default), higher bit depths are converted to 16-bit before reaching this processor.
 */
class EqAudioProcessor : BaseAudioProcessor() {
    @Volatile private var requested: ParametricEq? = null

    // Audio thread only.
    private var applied: ParametricEq? = null
    private var chain: EqChain? = null

    /** Applies [eq], or disables EQ when null. */
    fun setEq(eq: ParametricEq?) {
        requested = eq
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount in 1..2) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val activeChain = currentChain()
        val output = replaceOutputBuffer(size)
        if (activeChain == null) {
            output.put(inputBuffer)
        } else {
            activeChain.process(inputBuffer.order(ByteOrder.nativeOrder()), output)
        }
        output.flip()
    }

    private fun currentChain(): EqChain? {
        val eq = requested
        val format = inputAudioFormat
        val existing = chain
        if (eq === applied && (existing == null || (existing.sampleRate == format.sampleRate && existing.channelCount == format.channelCount))) {
            return existing
        }
        applied = eq
        chain = eq?.let { EqChain(it, format.sampleRate, format.channelCount) }?.also { new -> existing?.let(new::copyStateFrom) }
        return chain
    }

    override fun onFlush() {
        chain?.resetState()
    }

    override fun onReset() {
        chain = null
        applied = null
    }
}
