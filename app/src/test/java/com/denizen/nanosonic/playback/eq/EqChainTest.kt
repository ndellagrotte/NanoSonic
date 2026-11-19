package com.denizen.nanosonic.playback.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.denizen.nanosonic.core.model.EqBand
import com.denizen.nanosonic.core.model.FilterType
import com.denizen.nanosonic.core.model.ParametricEq
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class EqChainTest {
    private fun pcm(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach(::putShort); flip()
        }

    private fun ByteBuffer.shorts(): ShortArray = ShortArray(remaining() / 2) { getShort() }

    private fun run(chain: EqChain, input: ShortArray): ShortArray {
        val out = ByteBuffer.allocateDirect(input.size * 2).order(ByteOrder.nativeOrder())
        chain.process(pcm(*input), out)
        out.flip()
        return out.shorts()
    }

    private val flat = ParametricEq(0.0, emptyList())

    @Test
    fun `flat chain is bit exact`() {
        val input = shortArrayOf(0, 1, -1, 12345, -32768, 32767)
        assertArrayEquals(input, run(EqChain(flat, 48_000, 2), input))
    }

    @Test
    fun `preamp scales and output clamps instead of wrapping`() {
        val minus6 = run(EqChain(ParametricEq(-6.0206, emptyList()), 48_000, 1), shortArrayOf(20000, -20000))
        assertEquals(10000.0, minus6[0].toDouble(), 1.0)
        assertEquals(-10000.0, minus6[1].toDouble(), 1.0)
        val plus12 = run(EqChain(ParametricEq(12.0, emptyList()), 48_000, 1), shortArrayOf(30000, -30000))
        assertArrayEquals(shortArrayOf(32767, -32768), plus12)
    }

    @Test
    fun `channels are filtered independently`() {
        val eq = ParametricEq(0.0, listOf(EqBand(1000.0, 6.0, 1.0, FilterType.PK)))
        val chain = EqChain(eq, 48_000, 2)
        // Left gets a sine, right stays silent.
        val frames = 4800
        val input = ShortArray(frames * 2) { i -> if (i % 2 == 0) (10000 * sin(2 * PI * 1000 * (i / 2) / 48_000)).toInt().toShort() else 0 }
        val out = run(chain, input)
        assertTrue((1 until frames * 2 step 2).all { out[it].toInt() == 0 })
        val peak = (frames / 2 until frames).maxOf { abs(out[it * 2].toInt()) }
        assertEquals(19953.0, peak.toDouble(), 150.0) // +6 dB
    }

    @Test
    fun `bands above Nyquist and disabled bands are skipped`() {
        val eq = ParametricEq(
            0.0,
            listOf(EqBand(30_000.0, 6.0, 1.0, FilterType.PK), EqBand(1000.0, 6.0, 1.0, FilterType.PK, enabled = false)),
        )
        val input = shortArrayOf(100, 200, -300, 400)
        assertArrayEquals(input, run(EqChain(eq, 44_100, 1), input))
    }

    @Test
    fun `processor bypasses unsupported formats and rebuilds for a new sample rate`() {
        val processor = EqAudioProcessor()
        assertFalse(processor.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_16BIT)) != AudioFormat.NOT_SET)
        processor.reset()

        processor.setEq(ParametricEq(-6.0206, emptyList()))
        processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        assertTrue(processor.isActive)
        processor.queueInput(pcm(20000, 20000))
        assertEquals(10000.0, processor.output.shorts()[0].toDouble(), 1.0)

        processor.setEq(null)
        processor.queueInput(pcm(20000, 20000))
        assertEquals(20000.0, processor.output.shorts()[0].toDouble(), 0.0)
    }
}
