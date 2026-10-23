package com.shilapi.xcertplay

import android.media.AudioManager
import android.os.Looper
import android.provider.Settings
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class ElantraVoiceSourceBroadcastTest {
    @Test fun bluetoothRecoveryUsesUnprotectedNativeRequestsAndPreservesTheSelectedDeviceSlot() {
        val app = RuntimeEnvironment.getApplication()
        Settings.Global.putInt(app.contentResolver, ElantraVoiceSourceReturn.LAST_MODE, 9)
        Settings.Global.putInt(app.contentResolver, "android.extension.car.BT_AUDIO_FOCUS_STATE", 1)
        Settings.Global.putInt(app.contentResolver, "last_BTMedia_state", 1)
        app.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_NORMAL
        val restore = ElantraVoiceSourceReturn.create(app, { true }, {})
        fun state(mode: Int) = mapOf("appStates" to listOf(mapOf("appStateID" to 1, "entity" to if (mode > 0) 1 else 0,
            "speechMode" to mode), mapOf("appStateID" to 2, "entity" to 0)))
        restore.modes(state(1)); restore.modes(state(0))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        val intents = shadowOf(app).broadcastIntents
        assertTrue(intents.any { it.action == "action_request_a2dpsink_audio_focus" && it.`package` == "com.android.bluetooth" })
        assertTrue(intents.any { it.action == "com.humaxdigital.automotive.btaudio.AUDIO_STREAM_PLAY" })
        assertTrue(intents.any { it.action == "com.humaxdigital.automotive.btaudio.play" })
        assertFalse(intents.any { it.action?.contains("ACTION_LAST_MODE_BT_AUDIO") == true })
        assertEquals(9, Settings.Global.getInt(app.contentResolver, ElantraVoiceSourceReturn.LAST_MODE))
        restore.close()
    }
}
