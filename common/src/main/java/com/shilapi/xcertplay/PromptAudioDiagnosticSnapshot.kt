package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.airplay.AudioFormat
import java.util.Locale
import java.util.UUID

/** Latest received USB prompt format, separate from the user's quality preference. */
internal object PromptAudioDiagnosticSnapshot {
    private const val PREFS = "diplay_prompt_audio_diagnostics"
    data class Received(val codec: String, val sampleRate: Int, val channels: Int, val clockRate: Int,
        val receivedAtMillis: Long, val currentConnection: Boolean)

    @Synchronized fun begin(context: Context): String = UUID.randomUUID().toString().also { attempt ->
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("attempt", attempt).apply()
    }

    @Synchronized fun currentAttempt(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("attempt", null)

    @Synchronized fun record(context: Context, attempt: String?, format: AudioFormat) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (attempt == null || prefs.getString("attempt", null) != attempt ||
            format.audioType.lowercase(Locale.ROOT) !in setOf("default", "alert") ||
            format.sampleRate <= 0 || format.rtpClockRate <= 0 || format.channels <= 0) return
        prefs.edit().putString("formatAttempt", attempt).putString("codec", format.codec.name).putInt("rate", format.sampleRate)
            .putInt("channels", format.channels).putInt("clock", format.rtpClockRate)
            .putLong("receivedAt", System.currentTimeMillis()).apply()
    }

    @Synchronized fun latest(context: Context): Received? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val codec = prefs.getString("codec", null) ?: return null
        return Received(codec, prefs.getInt("rate", 0), prefs.getInt("channels", 0), prefs.getInt("clock", 0),
            prefs.getLong("receivedAt", 0), prefs.getString("formatAttempt", null) != null &&
                prefs.getString("formatAttempt", null) == prefs.getString("attempt", null))
    }

    @Synchronized fun report(context: Context): String {
        val received = latest(context) ?: return "No USB prompt format has been received."
        return "Received USB prompt codec=${received.codec} rate=${received.sampleRate} channels=${received.channels} " +
            "rtpClockRate=${received.clockRate} receivedAtEpochMs=${received.receivedAtMillis} " +
            "connection=${if (received.currentConnection) "current" else "previous"}; received format does not prove audible playback."
    }
}
