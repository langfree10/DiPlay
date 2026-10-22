package com.shilapi.xcertplay.media

/** Worker-owned PCM16 output levels; retains counters only, never speech samples. */
internal class AssistantOutputSignal {
    private var peak = 0
    private var samples = 0L
    private var nonZero = 0L

    fun observe(data: ByteArray, offset: Int, length: Int) {
        var position = offset
        val end = offset + length
        while (position + 1 < end) {
            val sample = ((data[position].toInt() and 255) or (data[position + 1].toInt() shl 8)).toShort().toInt()
            peak = maxOf(peak, kotlin.math.abs(sample))
            samples++
            if (sample != 0) nonZero++
            position += 2
        }
    }

    fun reportAndReset(): String {
        val result = "peakDbfs=${MicrophoneCaptureStats.peakDbfs(peak)} nonZeroSamples=$nonZero samples=$samples"
        peak = 0; samples = 0; nonZero = 0
        return result
    }
}
