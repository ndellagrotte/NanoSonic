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
    private fun buffer(bytes: Int): ByteBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())

    private fun pcm16(vararg samples: Short): ByteBuffer = buffer(samples.size * 2).apply { samples.forEach(::putShort); flip() }

    /** Packed little-endian 24-bit samples. */
    private fun pcm24(vararg samples: Int): ByteBuffer = buffer(samples.size * 3).apply {
        samples.forEach { put(it.toByte()); put((it shr 8).toByte()); put((it shr 16).toByte()) }
        flip()
    }

    private fun pcm32(vararg samples: Int): ByteBuffer = buffer(samples.size * 4).apply { samples.forEach(::putInt); flip() }

    private fun pcmFloat(vararg samples: Float): ByteBuffer = buffer(samples.size * 4).apply { samples.forEach(::putFloat); flip() }

    private fun ByteBuffer.floats(): FloatArray = FloatArray(remaining() / 4) { getFloat() }

    private fun run(chain: EqChain, input: ByteBuffer, encoding: Int): FloatArray {
        val samples = input.remaining() / when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            else -> 4
        }
        val out = buffer(samples * 4)
        chain.process(input, encoding, out)
        out.flip()
        return out.floats()
    }

    private fun run16(chain: EqChain, vararg samples: Short) = run(chain, pcm16(*samples), C.ENCODING_PCM_16BIT)

    private val flat = ParametricEq(0.0, emptyList())

    @Test
    fun `flat chain converts 16-bit to float exactly`() {
        val input = shortArrayOf(0, 1, -1, 12345, -32768, 32767)
        val expected = FloatArray(input.size) { input[it] / 32768f }
        assertArrayEquals(expected, run16(EqChain(flat, 48_000, 2), *input), 0f)
    }

    @Test
    fun `flat chain keeps every bit of 24-bit input`() {
        val input = intArrayOf(0, 1, -1, 0x123456, -0x123456, 8_388_607, -8_388_608)
        val expected = FloatArray(input.size) { input[it] / 8_388_608f }
        assertArrayEquals(expected, run(EqChain(flat, 96_000, 1), pcm24(*input), C.ENCODING_PCM_24BIT), 0f)
    }

    @Test
    fun `32-bit integer and float input are converted`() {
        val ints = intArrayOf(0, Int.MIN_VALUE, 1 shl 30)
        assertArrayEquals(floatArrayOf(0f, -1f, 0.5f), run(EqChain(flat, 48_000, 1), pcm32(*ints), C.ENCODING_PCM_32BIT), 0f)
        val floats = floatArrayOf(0f, 0.25f, -0.999f, 1.5f)
        assertArrayEquals(floats, run(EqChain(flat, 48_000, 1), pcmFloat(*floats), C.ENCODING_PCM_FLOAT), 0f)
    }

    @Test
    fun `preamp scales and output is not clamped`() {
        val minus6 = run16(EqChain(ParametricEq(-6.0206, emptyList()), 48_000, 1), 20000, -20000)
        assertEquals(10000 / 32768.0, minus6[0].toDouble(), 1e-4)
        assertEquals(-10000 / 32768.0, minus6[1].toDouble(), 1e-4)
        val plus12 = run16(EqChain(ParametricEq(12.0, emptyList()), 48_000, 1), 30000, -30000)
        assertTrue(plus12[0] > 3.5f && plus12[1] < -3.5f)
    }

    @Test
    fun `channels are filtered independently`() {
        val eq = ParametricEq(0.0, listOf(EqBand(1000.0, 6.0, 1.0, FilterType.PK)))
        val chain = EqChain(eq, 48_000, 2)
        // Left gets a sine, right stays silent.
        val frames = 4800
        val input = ShortArray(frames * 2) { i -> if (i % 2 == 0) (10000 * sin(2 * PI * 1000 * (i / 2) / 48_000)).toInt().toShort() else 0 }
        val out = run16(chain, *input)
        assertTrue((1 until frames * 2 step 2).all { out[it] == 0f })
        val peak = (frames / 2 until frames).maxOf { abs(out[it * 2]) } * 32768.0
        assertEquals(19953.0, peak, 150.0) // +6 dB
    }

    @Test
    fun `filters 24-bit input like 16-bit input`() {
        val eq = ParametricEq(-3.0, listOf(EqBand(200.0, 4.0, 0.7, FilterType.LSC), EqBand(3000.0, -5.0, 2.0, FilterType.PK)))
        val n = 2000
        val sine = DoubleArray(n) { 0.5 * sin(2 * PI * 440 * it / 48_000) }
        val from16 = run16(EqChain(eq, 48_000, 1), *ShortArray(n) { (sine[it] * 32768).toInt().toShort() })
        val from24 = run(EqChain(eq, 48_000, 1), pcm24(*IntArray(n) { (sine[it] * 8_388_608).toInt() }), C.ENCODING_PCM_24BIT)
        assertArrayEquals(from16, from24, 1e-4f)
    }

    @Test
    fun `bands above Nyquist and disabled bands are skipped`() {
        val eq = ParametricEq(
            0.0,
            listOf(EqBand(30_000.0, 6.0, 1.0, FilterType.PK), EqBand(1000.0, 6.0, 1.0, FilterType.PK, enabled = false)),
        )
        val input = shortArrayOf(100, 200, -300, 400)
        assertArrayEquals(FloatArray(input.size) { input[it] / 32768f }, run16(EqChain(eq, 44_100, 1), *input), 0f)
    }

    @Test
    fun `processor outputs float for supported formats and bypasses others`() {
        val processor = EqAudioProcessor()
        assertEquals(AudioFormat.NOT_SET, processor.configure(AudioFormat(48_000, 6, C.ENCODING_PCM_16BIT)))
        assertEquals(AudioFormat.NOT_SET, processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_24BIT_BIG_ENDIAN)))
        for (encoding in listOf(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT)) {
            assertEquals(AudioFormat(96_000, 2, C.ENCODING_PCM_FLOAT), processor.configure(AudioFormat(96_000, 2, encoding)))
        }
    }

    @Test
    fun `processor applies and removes EQ and rebuilds for a new sample rate`() {
        val processor = EqAudioProcessor()
        processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_24BIT))
        processor.flush()
        assertTrue(processor.isActive)

        // EQ off still converts to float.
        processor.queueInput(pcm24(4_000_000, 4_000_000))
        assertEquals(4_000_000 / 8_388_608f, processor.output.floats()[0], 0f)

        processor.setEq(ParametricEq(-6.0206, emptyList()))
        processor.queueInput(pcm24(4_000_000, 4_000_000))
        assertEquals(2_000_000 / 8_388_608.0, processor.output.floats()[0].toDouble(), 1e-5)

        processor.configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        processor.flush()
        processor.queueInput(pcmFloat(0.5f))
        assertEquals(0.25, processor.output.floats()[0].toDouble(), 1e-5)

        processor.setEq(null)
        processor.queueInput(pcmFloat(0.5f))
        assertEquals(0.5f, processor.output.floats()[0], 0f)
        assertFalse(processor.output.hasRemaining())
    }
}
