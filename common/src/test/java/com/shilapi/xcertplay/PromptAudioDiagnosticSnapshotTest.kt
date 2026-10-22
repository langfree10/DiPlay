package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.AudioCodecKind
import com.shilapi.xcertplay.airplay.AudioFormat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PromptAudioDiagnosticSnapshotTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val pcm = AudioFormat(AudioCodecKind.LPCM, 8000, 2, 101, "default")

    @org.junit.Before fun clearDiagnostics() {
        app.getSharedPreferences("diplay_prompt_audio_diagnostics", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun actualReceivedFormatNeverComesFromTheSavedQualityChoice() {
        val attempt = PromptAudioDiagnosticSnapshot.begin(app)
        AirPlayPersistence.saveSpeechWithout8And16Khz(app, true)
        assertNull(PromptAudioDiagnosticSnapshot.latest(app))
        PromptAudioDiagnosticSnapshot.record(app, attempt, pcm)
        assertEquals(8000, PromptAudioDiagnosticSnapshot.latest(app)?.sampleRate)
        assertTrue(AirPlayPersistence.loadSpeechWithout8And16Khz(app))
    }

    @Test fun reconnectKeepsLastFormatLabelsHistoryAndRejectsStaleOwner() {
        val old = PromptAudioDiagnosticSnapshot.begin(app)
        PromptAudioDiagnosticSnapshot.record(app, old, pcm)
        assertTrue(PromptAudioDiagnosticSnapshot.latest(app)!!.currentConnection)
        val current = PromptAudioDiagnosticSnapshot.begin(app)
        assertEquals(8000, PromptAudioDiagnosticSnapshot.latest(app)!!.sampleRate)
        assertFalse(PromptAudioDiagnosticSnapshot.latest(app)!!.currentConnection)
        assertTrue(PromptAudioDiagnosticSnapshot.report(app).contains("connection=previous"))
        PromptAudioDiagnosticSnapshot.record(app, old, pcm.copy(sampleRate = 48000))
        PromptAudioDiagnosticSnapshot.record(app, null, pcm)
        assertEquals(8000, PromptAudioDiagnosticSnapshot.latest(app)!!.sampleRate)
        PromptAudioDiagnosticSnapshot.record(app, current, pcm.copy(sampleRate = 24000, rtpClockRate = 24000))
        assertEquals(24000, PromptAudioDiagnosticSnapshot.latest(app)!!.sampleRate)
        assertTrue(PromptAudioDiagnosticSnapshot.latest(app)!!.currentConnection)
    }

    @Test fun musicSiriAndTelephoneCannotOverwritePromptFormatAndOpusClockIsRetained() {
        val attempt = PromptAudioDiagnosticSnapshot.begin(app)
        for (type in listOf("media", "speechrecognition", "telephony"))
            PromptAudioDiagnosticSnapshot.record(app, attempt, pcm.copy(audioType = type))
        assertNull(PromptAudioDiagnosticSnapshot.latest(app))
        PromptAudioDiagnosticSnapshot.record(app, attempt, AudioFormat(AudioCodecKind.OPUS, 48000, 1, 100, "alert", 24000))
        val received = PromptAudioDiagnosticSnapshot.latest(app)!!
        assertEquals(48000, received.sampleRate); assertEquals(24000, received.clockRate)
        assertTrue(PromptAudioDiagnosticSnapshot.report(app).contains("does not prove audible playback"))
    }
}
