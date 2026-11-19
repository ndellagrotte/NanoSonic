package com.denizen.nanosonic.playback.service

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioSink
import com.denizen.nanosonic.core.model.ParametricEq
import com.denizen.nanosonic.playback.eq.EqAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EqAudioSinkTest {
    /** Records what reaches the real sink; accepts at most [maxBytesPerCall] bytes per `handleBuffer`. */
    private class FakeSink(private val maxBytesPerCall: Int = Int.MAX_VALUE) : AudioSink by noOpSink() {
        var configured: Format? = null
        val received = mutableListOf<ByteBuffer>()
        val written: ByteBuffer = ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder())

        override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
            configured = inputFormat
        }

        override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
            received += buffer
            repeat(minOf(maxBytesPerCall, buffer.remaining())) { written.put(buffer.get()) }
            return !buffer.hasRemaining()
        }

        companion object {
            private fun noOpSink(): AudioSink = Proxy.newProxyInstance(AudioSink::class.java.classLoader, arrayOf(AudioSink::class.java)) { _, method, _ ->
                when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Long.TYPE -> 0L
                    Integer.TYPE -> 0
                    else -> null
                }
            } as AudioSink
        }
    }

    private fun rawFormat(encoding: Int, channels: Int = 2) = Format.Builder()
        .setSampleMimeType(MimeTypes.AUDIO_RAW)
        .setPcmEncoding(encoding)
        .setSampleRate(48_000)
        .setChannelCount(channels)
        .build()

    private fun pcm16(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder()).apply { samples.forEach(::putShort); flip() }

    @Test
    fun `equalizes each buffer once even when the sink takes it in pieces`() {
        val fake = FakeSink(maxBytesPerCall = 6)
        val eq = EqAudioProcessor().apply { setEq(ParametricEq(-6.0206, emptyList())) }
        val sink = EqAudioSink(fake, eq)
        sink.configure(rawFormat(C.ENCODING_PCM_16BIT), 0, null)
        assertEquals(C.ENCODING_PCM_FLOAT, fake.configured!!.pcmEncoding)

        val input = pcm16(20000, -20000, 10000, -10000)
        var calls = 0
        while (!sink.handleBuffer(input, 0, 1)) calls++
        assertEquals(2, calls) // 16 float bytes at 6 per call: false, false, true
        assertFalse(input.hasRemaining())
        assertTrue(fake.received.all { it === fake.received[0] })

        fake.written.flip()
        val out = FloatArray(4) { fake.written.getFloat() }
        listOf(10000, -10000, 5000, -5000).forEachIndexed { i, expected ->
            assertEquals(expected / 32768.0, out[i].toDouble(), 1e-4)
        }
    }

    @Test
    fun `unsupported formats pass through untouched`() {
        val fake = FakeSink()
        val sink = EqAudioSink(fake, EqAudioProcessor())
        val sixChannels = rawFormat(C.ENCODING_PCM_16BIT, channels = 6)
        sink.configure(sixChannels, 0, null)
        assertSame(sixChannels, fake.configured)

        val input = pcm16(1, 2, 3, 4, 5, 6)
        assertTrue(sink.handleBuffer(input, 0, 1))
        assertSame(input, fake.received.single())
    }
}
