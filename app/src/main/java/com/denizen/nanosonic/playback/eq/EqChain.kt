package com.denizen.nanosonic.playback.eq

import com.denizen.nanosonic.core.model.ParametricEq
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A cascade of biquads applied to interleaved 16-bit PCM. Not thread-safe: owned by the audio thread.
 * Processing allocates nothing.
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

    /** Reads all of [input] and writes the same number of bytes to [output]. */
    fun process(input: ByteBuffer, output: ByteBuffer) {
        var channel = 0
        while (input.remaining() >= 2) {
            var x = input.getShort() / FULL_SCALE * preampGain
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
            output.putShort((x * FULL_SCALE).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            channel = if (channel + 1 == channelCount) 0 else channel + 1
        }
    }

    fun resetState() = state.fill(0.0)

    /** Carries filter memory over from [other] when the layout matches, so a profile switch doesn't click. */
    fun copyStateFrom(other: EqChain) {
        if (other.filterCount == filterCount && other.channelCount == channelCount) other.state.copyInto(state)
    }

    private companion object {
        const val FULL_SCALE = 32768.0
    }
}
