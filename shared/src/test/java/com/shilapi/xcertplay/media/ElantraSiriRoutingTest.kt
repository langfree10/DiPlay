package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.airplay.AudioCodecKind
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.AudioStreamId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ElantraSiriRoutingTest {
    @Test fun elantraSiriUsesNavigationPlaybackAttributesButRetainsAssistantFocusOwnership() {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        org.robolectric.shadows.ShadowBuild.setManufacturer("humaxdigital")
        org.robolectric.shadows.ShadowBuild.setModel("BHCN-T3103C")
        val sink = AndroidMediaSink(context = RuntimeEnvironment.getApplication(),
            audioFocusEnabled = false, temporaryAssistantFocus = true)
        try {
            val renderer = renderer(sink, "speechrecognition")
            val selection = renderer.javaClass.getDeclaredMethod("mappedSelection").apply { isAccessible = true }
                .invoke(renderer) as AudioChannelSelection
            assertEquals(AudioChannel.ASSISTANT, selection.channel)
            val attributes = renderer.javaClass.getDeclaredMethod("audioAttributesFor", AudioChannelSelection::class.java)
                .apply { isAccessible = true }.invoke(renderer, selection) as android.media.AudioAttributes
            assertEquals(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, attributes.usage)
            assertEquals(android.media.AudioAttributes.CONTENT_TYPE_MUSIC, attributes.contentType)
            val coordinator = AndroidMediaSink::class.java.getDeclaredField("audioFocusCoordinator")
                .apply { isAccessible = true }.get(sink) as AudioFocusCoordinator
            assertTrue(coordinator.assistantOnly)
            assertFalse(coordinator.voiceStreamsOnly)
            assertEquals(AudioChannel.MEDIA, selection(sink, "media").channel)
            assertEquals(AudioChannel.NAVIGATION, selection(sink, "default").channel)
            assertEquals(AudioChannel.PHONE, selection(sink, "telephony").channel)
        } finally {
            sink.close()
            org.robolectric.shadows.ShadowBuild.setManufacturer(manufacturer)
            org.robolectric.shadows.ShadowBuild.setModel(model)
        }
    }

    @Test fun enablingTemporarySiriFocusDoesNotReclassifyUsbMusicNavigationOrCalls() {
        val sink = AndroidMediaSink(context = RuntimeEnvironment.getApplication(),
            audioFocusEnabled = false, bluetoothVoiceAudio = false, temporaryAssistantFocus = true)
        try {
            val coordinator = AndroidMediaSink::class.java.getDeclaredField("audioFocusCoordinator")
                .apply { isAccessible = true }.get(sink) as AudioFocusCoordinator
            assertTrue(coordinator.assistantOnly)
            assertFalse(coordinator.voiceStreamsOnly)
            for ((type, channel) in listOf("media" to AudioChannel.MEDIA,
                "speechrecognition" to AudioChannel.ASSISTANT, "default" to AudioChannel.NAVIGATION,
                "alert" to AudioChannel.NAVIGATION, "telephony" to AudioChannel.PHONE)) {
                assertEquals(type, channel, selection(sink, type).channel)
            }
        } finally { sink.close() }
    }

    @Test fun bluetoothVoiceStreamsStillMapToAssistantButGenericDevicesKeepTheirNormalFocusPolicy() {
        val voice = AndroidMediaSink(bluetoothVoiceAudio = true)
        val generic = AndroidMediaSink()
        try {
            for (type in listOf("speechrecognition", "default", "alert"))
                assertEquals(AudioChannel.ASSISTANT, selection(voice, type).channel)
            val coordinator = AndroidMediaSink::class.java.getDeclaredField("audioFocusCoordinator")
                .apply { isAccessible = true }.get(generic) as AudioFocusCoordinator
            assertFalse(coordinator.assistantOnly)
            assertEquals(AudioChannel.NAVIGATION, selection(generic, "default").channel)
        } finally { voice.close(); generic.close() }
    }

    private fun selection(sink: AndroidMediaSink, type: String): AudioChannelSelection {
        val renderer = renderer(sink, type)
        return renderer.javaClass.getDeclaredMethod("mappedSelection").apply { isAccessible = true }
            .invoke(renderer) as AudioChannelSelection
    }

    private fun renderer(sink: AndroidMediaSink, type: String): Any {
        val format = AudioFormat(AudioCodecKind.LPCM, 24_000, 1, 100, type)
        return AndroidMediaSink::class.java.getDeclaredMethod("audioRenderer",
            AudioStreamId::class.java, AudioFormat::class.java).apply { isAccessible = true }
            .invoke(sink, AudioStreamId(100, type), format)
    }
}
