package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.airplay.VideoCodec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MainVideoCodecObservationTest {
    @Test fun onlyMainVideoUpdatesTheObservedCodecAndLateClosedCallbacksAreIgnored() {
        val received = mutableListOf<VideoCodec>()
        val sink = AndroidMediaSink(onMainVideoCodec = received::add)
        try {
            sink.onVideoCodec(111, VideoCodec.H264)
            assertTrue(received.isEmpty())
            sink.onVideoCodec(110, VideoCodec.H265)
            assertEquals(listOf(VideoCodec.H265), received)
            sink.close()
            sink.onVideoCodec(110, VideoCodec.H264)
            assertEquals(listOf(VideoCodec.H265), received)
        } finally { sink.close() }
    }
    @Test fun diagnosticObserverFailureCannotBreakVideoNegotiation() {
        val sink = AndroidMediaSink(onMainVideoCodec = { throw IllegalStateException("test observer unavailable") })
        try { sink.onVideoCodec(110, VideoCodec.H264); sink.onVideoCodec(110, VideoCodec.H265) }
        finally { sink.close() }
    }
}
