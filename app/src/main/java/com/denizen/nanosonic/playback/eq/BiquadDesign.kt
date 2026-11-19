package com.denizen.nanosonic.playback.eq

import com.denizen.nanosonic.core.model.FilterType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biquad coefficients from Robert Bristow-Johnson's Audio EQ Cookbook, the same designs AutoEq uses.
 * Coefficients are normalised by a0 and returned as `[b0, b1, b2, a1, a2]`.
 */
object BiquadDesign {
    const val COEFFICIENT_COUNT = 5

    fun design(type: FilterType, sampleRate: Int, frequency: Double, gainDb: Double, q: Double): DoubleArray {
        val w0 = 2 * PI * frequency / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2 * q)
        val a = 10.0.pow(gainDb / 40)
        val twoSqrtAAlpha = 2 * sqrt(a) * alpha

        val b0: Double; val b1: Double; val b2: Double; val a0: Double; val a1: Double; val a2: Double
        when (type) {
            FilterType.PK -> {
                b0 = 1 + alpha * a; b1 = -2 * cosW; b2 = 1 - alpha * a
                a0 = 1 + alpha / a; a1 = -2 * cosW; a2 = 1 - alpha / a
            }
            FilterType.LSC -> {
                b0 = a * ((a + 1) - (a - 1) * cosW + twoSqrtAAlpha)
                b1 = 2 * a * ((a - 1) - (a + 1) * cosW)
                b2 = a * ((a + 1) - (a - 1) * cosW - twoSqrtAAlpha)
                a0 = (a + 1) + (a - 1) * cosW + twoSqrtAAlpha
                a1 = -2 * ((a - 1) + (a + 1) * cosW)
                a2 = (a + 1) + (a - 1) * cosW - twoSqrtAAlpha
            }
            FilterType.HSC -> {
                b0 = a * ((a + 1) + (a - 1) * cosW + twoSqrtAAlpha)
                b1 = -2 * a * ((a - 1) + (a + 1) * cosW)
                b2 = a * ((a + 1) + (a - 1) * cosW - twoSqrtAAlpha)
                a0 = (a + 1) - (a - 1) * cosW + twoSqrtAAlpha
                a1 = 2 * ((a - 1) - (a + 1) * cosW)
                a2 = (a + 1) - (a - 1) * cosW - twoSqrtAAlpha
            }
            FilterType.LPQ -> {
                b0 = (1 - cosW) / 2; b1 = 1 - cosW; b2 = (1 - cosW) / 2
                a0 = 1 + alpha; a1 = -2 * cosW; a2 = 1 - alpha
            }
            FilterType.HPQ -> {
                b0 = (1 + cosW) / 2; b1 = -(1 + cosW); b2 = (1 + cosW) / 2
                a0 = 1 + alpha; a1 = -2 * cosW; a2 = 1 - alpha
            }
        }
        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    /** Magnitude response in dB of normalised coefficients [c] at [frequency]. */
    fun magnitudeDb(c: DoubleArray, sampleRate: Int, frequency: Double): Double {
        val w = 2 * PI * frequency / sampleRate
        // H(z) evaluated at z = e^{jw}: (b0 + b1 z^-1 + b2 z^-2) / (1 + a1 z^-1 + a2 z^-2)
        val numRe = c[0] + c[1] * cos(w) + c[2] * cos(2 * w)
        val numIm = -(c[1] * sin(w) + c[2] * sin(2 * w))
        val denRe = 1 + c[3] * cos(w) + c[4] * cos(2 * w)
        val denIm = -(c[3] * sin(w) + c[4] * sin(2 * w))
        return 20 * log10(hypot(numRe, numIm) / hypot(denRe, denIm))
    }

    /** True if both poles lie inside the unit circle. */
    fun isStable(c: DoubleArray): Boolean {
        val a1 = c[3]; val a2 = c[4]
        return kotlin.math.abs(a2) < 1 && kotlin.math.abs(a1) < 1 + a2
    }

}
