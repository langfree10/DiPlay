package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.airplay.AudioCodecKind
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.AudioStreamId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PromptAudioFormatObservationTest {
    private fun renderer(sink: AndroidMediaSink, format: AudioFormat) = AndroidMediaSink::class.java
        .getDeclaredMethod("audioRenderer", AudioStreamId::class.java, AudioFormat::class.java)
        .apply { isAccessible = true }.invoke(sink, AudioStreamId(format.payloadType, format.audioType), format)

    @Test fun observesReceivedPromptFormatsOncePerRendererAndRejectsMusicAndLateClosedUpdates() {
        val seen = mutableListOf<AudioFormat>()
        val sink = AndroidMediaSink(onPromptAudioFormat = seen::add)
        val pcm = AudioFormat(AudioCodecKind.LPCM, 8000, 2, 101, "default")
        try {
            renderer(sink, pcm.copy(audioType = "media"))
            renderer(sink, pcm.copy(audioType = "speechrecognition"))
            assertTrue(seen.isEmpty())
            renderer(sink, pcm); renderer(sink, pcm)
            assertEquals(listOf(pcm), seen)
            renderer(sink, pcm.copy(sampleRate = 24000, rtpClockRate = 24000))
            assertEquals(2, seen.size)
            sink.close()
            renderer(sink, pcm.copy(audioType = "alert"))
            assertEquals(2, seen.size)
        } finally { sink.close() }
    }

    @Test fun observerFailureDoesNotBreakAudioRendererCreation() {
        val sink = AndroidMediaSink(onPromptAudioFormat = { throw IllegalStateException("observer unavailable") })
        try { assertNotNull(renderer(sink, AudioFormat(AudioCodecKind.LPCM, 24000, 1, 101, "default"))) }
        finally { sink.close() }
    }
}
