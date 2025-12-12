package com.example.nanosonicproject.service.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Biquad filter implementation for graphic EQ
 * Applies gain at a specific frequency using peaking EQ filter
 */
class BiquadFilter(
    private val sampleRate: Int,
    private val frequency: Double,
    private val gain: Double
) {
    // Filter coefficients
    private var a0 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0

    // State variables for filtering (per channel)
    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    companion object {
        // Fixed Q value matching AutoEQ FixedBandEQ format
        // Q = 1.41 (sqrt(2)) provides optimal response for 10-band EQ
        private const val GRAPHIC_EQ_Q = 1.41
    }

    init {
        calculateCoefficients()
    }

    /**
     * Calculate biquad filter coefficients for peaking EQ
     * Based on Robert Bristow-Johnson's Audio EQ Cookbook
     */
    private fun calculateCoefficients() {
        val A = 10.0.pow(gain / 40.0) // Gain in linear scale
        val omega = 2.0 * PI * frequency / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / (2.0 * GRAPHIC_EQ_Q)

        // Peaking EQ coefficients
        b0 = 1.0 + alpha * A
        b1 = -2.0 * cosOmega
        b2 = 1.0 - alpha * A
        a0 = 1.0 + alpha / A
        a1 = -2.0 * cosOmega
        a2 = 1.0 - alpha / A

        // Normalize coefficients
        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
        a0 = 1.0
    }

    /**
     * Process a single sample (mono)
     */
    fun processSample(input: Double): Double {
        val output = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L

        // Update state
        x2L = x1L
        x1L = input
        y2L = y1L
        y1L = output

        return output
    }

    /**
     * Process stereo samples (left and right channels)
     */
    fun processStereo(inputLeft: Double, inputRight: Double): Pair<Double, Double> {
        // Left channel
        val outputLeft = b0 * inputLeft + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = inputLeft
        y2L = y1L
        y1L = outputLeft

        // Right channel
        val outputRight = b0 * inputRight + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = inputRight
        y2R = y1R
        y1R = outputRight

        return Pair(outputLeft, outputRight)
    }

    /**
     * Reset filter state (clears history)
     */
    fun reset() {
        x1L = 0.0
        x2L = 0.0
        y1L = 0.0
        y2L = 0.0
        x1R = 0.0
        x2R = 0.0
        y1R = 0.0
        y2R = 0.0
    }
}