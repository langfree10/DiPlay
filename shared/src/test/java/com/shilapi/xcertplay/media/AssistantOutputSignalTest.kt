package com.shilapi.xcertplay.media

import org.junit.Assert.*
import org.junit.Test

class AssistantOutputSignalTest {
    @Test fun distinguishesSilentPcmFromNonzeroSpeechAndResetsEachWindow() {
        val signal = AssistantOutputSignal()
        signal.observe(ByteArray(8), 0, 8)
        assertEquals("peakDbfs=silent nonZeroSamples=0 samples=4", signal.reportAndReset())
        // PCM16: +16384, -32768, 0; exercise both signs and the full negative range.
        signal.observe(byteArrayOf(99, 0, 64, 0, -128, 0, 0, 99), 1, 6)
        assertEquals("peakDbfs=0 nonZeroSamples=2 samples=3", signal.reportAndReset())
        assertEquals("peakDbfs=silent nonZeroSamples=0 samples=0", signal.reportAndReset())
    }

    @Test fun halfScaleSignalIsMinusSixDbfs() {
        val signal = AssistantOutputSignal()
        signal.observe(byteArrayOf(0, 64, 0, -64), 0, 4)
        assertTrue(signal.reportAndReset().startsWith("peakDbfs=-6 nonZeroSamples=2"))
    }
}
