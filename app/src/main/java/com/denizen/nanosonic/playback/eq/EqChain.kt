package com.denizen.nanosonic.playback.eq

import androidx.media3.common.C
import com.denizen.nanosonic.core.model.ParametricEq
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * A cascade of biquads applied to interleaved PCM. Reads 16-bit, packed 24-bit, 32-bit integer or float
 * input ([SUPPORTED_ENCODINGS]) and always writes 32-bit float, unclamped, so neither a positive preamp
 * nor a boost clips here. Filtering runs in double precision.
 *
 * Not thread-safe: owned by the audio thread. Processing allocates nothing.
 */
class EqChain(eq: ParametricEq, val sampleRate: Int, val channelCount: Int) {
    private val preampGain = 10.0.pow(eq.preampDb / 20)
    private val coefficients: DoubleArray
    private val filterCount: Int
    /** Transposed direct form II state, `[channel][filter][z1, z2]` flattened. */
    private val state: DoubleArray

    init {
        val nyquist = sampleRate / 2.0
        val bands = eq.bands.filter { it.enabled && it.frequency > 0 && it.frequency < nyquist }
        filterCount = bands.size
        coefficients = DoubleArray(filterCount * BiquadDesign.COEFFICIENT_COUNT)
        bands.forEachIndexed { i, band ->
            BiquadDesign.design(band.filterType, sampleRate, band.frequency, band.gain, band.q)
                .copyInto(coefficients, i * BiquadDesign.COEFFICIENT_COUNT)
        }
        state = DoubleArray(channelCount * filterCount * 2)
    }

    /**
     * Reads all whole samples of [input], which holds [inputEncoding] PCM in native byte order, and writes
     * one float per sample to [output].
     */
    fun process(input: ByteBuffer, inputEncoding: Int, output: ByteBuffer) {
        var channel = 0
        when (inputEncoding) {
            C.ENCODING_PCM_16BIT -> while (input.remaining() >= 2) {
                output.putFloat(filter(input.getShort() / SCALE_16, channel))
                channel = nextChannel(channel)
            }
            C.ENCODING_PCM_24BIT -> while (input.remaining() >= 3) {
                val b0 = input.get().toInt() and 0xFF
                val b1 = input.get().toInt() and 0xFF
                val b2 = input.get().toInt() // sign carries from the top byte
                output.putFloat(filter((b0 or (b1 shl 8) or (b2 shl 16)) / SCALE_24, channel))
                channel = nextChannel(channel)
            }
            C.ENCODING_PCM_32BIT -> while (input.remaining() >= 4) {
                output.putFloat(filter(input.getInt() / SCALE_32, channel))
                channel = nextChannel(channel)
            }
            C.ENCODING_PCM_FLOAT -> while (input.remaining() >= 4) {
                output.putFloat(filter(input.getFloat().toDouble(), channel))
                channel = nextChannel(channel)
            }
            else -> throw IllegalArgumentException("Unsupported PCM encoding $inputEncoding")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nextChannel(channel: Int) = if (channel + 1 == channelCount) 0 else channel + 1

    @Suppress("NOTHING_TO_INLINE")
    private inline fun filter(sample: Double, channel: Int): Float {
        var x = sample * preampGain
        var s = channel * filterCount * 2
        var c = 0
        repeat(filterCount) {
            val y = coefficients[c] * x + state[s]
            state[s] = coefficients[c + 1] * x - coefficients[c + 3] * y + state[s + 1]
            state[s + 1] = coefficients[c + 2] * x - coefficients[c + 4] * y
            x = y
            s += 2
            c += BiquadDesign.COEFFICIENT_COUNT
        }
        return x.toFloat()
    }

    fun resetState() = state.fill(0.0)

    /** Carries filter memory over from [other] when the layout matches, so a profile switch doesn't click. */
    fun copyStateFrom(other: EqChain) {
        if (other.filterCount == filterCount && other.channelCount == channelCount) other.state.copyInto(state)
    }

    companion object {
        /** Input encodings [process] accepts. */
        val SUPPORTED_ENCODINGS = setOf(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT)

        private const val SCALE_16 = 32768.0
        private const val SCALE_24 = 8388608.0
        private const val SCALE_32 = 2147483648.0
    }
}
