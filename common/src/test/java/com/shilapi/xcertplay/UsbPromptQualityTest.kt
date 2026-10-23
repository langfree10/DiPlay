package com.shilapi.xcertplay

import android.content.Context
import org.junit.Assert.*
import org.junit.After
import android.os.Build
import org.robolectric.shadows.ShadowBuild
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class UsbPromptQualityTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL
    @After fun restoreBuild() { ShadowBuild.setManufacturer(manufacturer); ShadowBuild.setModel(model) }
    @Before fun reset() { prefs.edit().clear().commit() }

    @Test fun oldCompatibleAndClearChoicesArePreservedWithoutOptingInto48Khz() {
        assertEquals(UsbPromptQuality.COMPATIBLE, AirPlayPersistence.loadUsbPromptQuality(app))
        prefs.edit().putBoolean("speech_without_8_16_khz", true).commit()
        assertEquals(UsbPromptQuality.CLEAR, AirPlayPersistence.loadUsbPromptQuality(app))
        prefs.edit().putBoolean("speech_without_8_16_khz", false).commit()
        assertEquals(UsbPromptQuality.COMPATIBLE, AirPlayPersistence.loadUsbPromptQuality(app))
    }

    @Test fun selecting48AndReturningToEitherOlderChoiceNeverChangesAudioTransport() {
        AirPlayPersistence.saveCarBluetoothAudio(app, true)
        AirPlayPersistence.saveElantraBluetoothVoice(app, true)
        for (quality in listOf(UsbPromptQuality.PCM_48_KHZ, UsbPromptQuality.CLEAR, UsbPromptQuality.COMPATIBLE)) {
            AirPlayPersistence.saveUsbPromptQuality(app, quality)
            assertEquals(quality, AirPlayPersistence.loadUsbPromptQuality(app))
            assertEquals(quality != UsbPromptQuality.COMPATIBLE, AirPlayPersistence.loadSpeechWithout8And16Khz(app))
            assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        }
        AirPlayPersistence.saveUsbPromptQuality(app, UsbPromptQuality.PCM_48_KHZ)
        AirPlayPersistence.saveSpeechWithout8And16Khz(app, true)
        assertEquals(UsbPromptQuality.CLEAR, AirPlayPersistence.loadUsbPromptQuality(app))
    }

    @Test fun elantraFreshDefaultOffers48KhzBeforeReadingOtherAudioSettings() {
        ShadowBuild.setManufacturer("humaxdigital"); ShadowBuild.setModel("BHCN-T3103C")
        assertTrue(AirPlayPersistence.loadSpeechWithout8And16Khz(app))
        assertEquals(UsbPromptQuality.PCM_48_KHZ, AirPlayPersistence.loadUsbPromptQuality(app))
        for (choice in UsbPromptQuality.entries) {
            AirPlayPersistence.saveUsbPromptQuality(app, choice)
            AirPlayPersistence.loadCarBluetoothAudio(app)
            assertEquals(choice, AirPlayPersistence.loadUsbPromptQuality(app))
        }
    }

    @Test fun elantraUpgradePreservesExplicitLegacyChoicesAnd48Khz() {
        ShadowBuild.setManufacturer("humaxdigital"); ShadowBuild.setModel("BHCN-T3103C")
        for (clear in listOf(false, true)) {
            prefs.edit().clear().putBoolean("speech_without_8_16_khz", clear).commit()
            assertEquals(if (clear) UsbPromptQuality.CLEAR else UsbPromptQuality.COMPATIBLE,
                AirPlayPersistence.loadUsbPromptQuality(app))
        }
        AirPlayPersistence.saveUsbPromptQuality(app, UsbPromptQuality.PCM_48_KHZ)
        assertEquals(UsbPromptQuality.PCM_48_KHZ, AirPlayPersistence.loadUsbPromptQuality(app))
    }

}
