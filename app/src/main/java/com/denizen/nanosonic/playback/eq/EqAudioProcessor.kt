package com.denizen.nanosonic.playback.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.Util
import com.denizen.nanosonic.core.model.ParametricEq
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parametric EQ for decoded PCM.
 *
 * Accepts 16-bit, packed 24-bit, 32-bit integer or float PCM with one or two channels, and always outputs
 * 32-bit float (unclamped), including while EQ is off, so turning EQ on or off never changes the output
 * format. Other formats are not handled (`configure` returns [AudioFormat.NOT_SET]) and should bypass it.
 *
 * [setEq] may be called from any thread: it publishes an immutable snapshot, and the audio thread picks
 * it up at the start of the next buffer, recomputing coefficients for the current sample rate. A change
 * becomes audible once the audio already queued in the AudioTrack has played (a few hundred ms).
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
        if (inputAudioFormat.encoding in EqChain.SUPPORTED_ENCODINGS && inputAudioFormat.channelCount in 1..2) {
            AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
        } else {
            AudioFormat.NOT_SET
        }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val encoding = inputAudioFormat.encoding
        val output = replaceOutputBuffer(size / Util.getByteDepth(encoding) * FLOAT_BYTES)
        currentChain().process(inputBuffer.order(ByteOrder.nativeOrder()), encoding, output)
        // Drop a trailing partial sample, if any, so the input always counts as consumed.
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    private fun currentChain(): EqChain {
        val eq = requested
        val format = inputAudioFormat
        val existing = chain
        if (existing != null && eq === applied && existing.sampleRate == format.sampleRate && existing.channelCount == format.channelCount) {
            return existing
        }
        applied = eq
        return EqChain(eq ?: FLAT, format.sampleRate, format.channelCount)
            .also { new -> existing?.let(new::copyStateFrom) }
            .also { chain = it }
    }

    override fun onFlush() {
        chain?.resetState()
    }

    override fun onReset() {
        chain = null
        applied = null
    }

    private companion object {
        const val FLOAT_BYTES = 4
        val FLAT = ParametricEq(0.0, emptyList())
    }
}
