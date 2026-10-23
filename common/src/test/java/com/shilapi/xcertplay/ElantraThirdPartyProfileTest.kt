package com.shilapi.xcertplay

import android.content.Context
import android.os.Build
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ElantraThirdPartyProfileTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL

    @Before fun setDevice() {
        prefs.edit().clear().commit()
        ShadowBuild.setManufacturer("humaxdigital")
        ShadowBuild.setModel("BHCN-T3103C")
    }

    @After fun cleanup() {
        prefs.edit().clear().commit()
        ShadowBuild.setManufacturer(manufacturer)
        ShadowBuild.setModel(model)
    }

    @Test fun profilePreservesAudioTransportChoiceWhileRemovingFocusAndSettingFullscreen() {
        prefs.edit().putBoolean("car_bluetooth_audio", true)
            .putBoolean("disable_media_key_audio_focus", false)
            .putBoolean("audio_focus_enabled", true)
            .putInt("media_audio_channel", 3).putInt("navigation_audio_channel", 14)
            .putBoolean("hide_top_bar", false).putBoolean("harman_fullscreen", false).commit()
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        assertFalse(AirPlayPersistence.loadAudioFocusEnabled(app))
        assertTrue(AirPlayPersistence.loadDisableMediaKeyAudioFocus(app))
        assertEquals(0, AirPlayPersistence.loadMediaAudioChannel(app))
        assertEquals(0, AirPlayPersistence.loadNavigationAudioChannel(app))
        assertTrue(AirPlayPersistence.loadHideTopBar(app))
        assertTrue(AirPlayPersistence.loadHideBottomBar(app))
        assertTrue(AirPlayPersistence.loadHarmanFullscreen(app))
        assertEquals(2062, HarmanFullscreenCompat.SYSTEM_UI_FLAGS)
        AirPlayPersistence.saveCarBluetoothAudio(app, true)
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, false)
        AirPlayPersistence.saveHarmanFullscreen(app, false)
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        assertFalse(AirPlayPersistence.loadDisableMediaKeyAudioFocus(app))
        assertFalse(AirPlayPersistence.loadHarmanFullscreen(app))
    }

    @Test fun test4MigrationPreservesWorkingAudioAndDefaultsToWiredSquareElantra() {
        prefs.edit().putBoolean("elantra_third_party_defaults_v2", true)
            .putBoolean("car_bluetooth_audio", true).putBoolean("audio_focus_enabled", false)
            .putString("oem_label", "BYD").putBoolean("main_display_disable_safe_area", false).commit()
        assertFalse(AirPlayPersistence.loadWirelessEnabled(app))
        assertEquals(DefaultConnectionMode.USB, DiPlayPreferences.defaultConnectionMode(app))
        assertEquals("伊兰特", AirPlayPersistence.loadOemLabel(app))
        assertEquals("Hyundai", AirPlayPersistence.loadManufacturer(app))
        assertEquals("Elantra", AirPlayPersistence.loadModel(app))
        assertTrue(AirPlayPersistence.loadMainDisplayDisableSafeArea(app))
        assertEquals(0, AirPlayPersistence.loadBlackCornerRadiusDp(app))
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        assertFalse(AirPlayPersistence.loadAudioFocusEnabled(app))
        AirPlayPersistence.saveMainDisplayDisableSafeArea(app, false)
        assertFalse(AirPlayPersistence.loadMainDisplayDisableSafeArea(app))
        AirPlayPersistence.saveWirelessEnabled(app, true)
        assertFalse(AirPlayPersistence.loadWirelessEnabled(app))
    }

    @Test fun voiceExperimentCanBeDisabledWithoutChangingWorkingBluetoothMusicOrZeroCharging() {
        AirPlayPersistence.saveCarBluetoothAudio(app, true)
        AirPlayPersistence.saveUsbChargingCurrent(app, UsbChargingCurrent.DATA_ONLY)
        assertTrue(AirPlayPersistence.loadElantraBluetoothVoice(app))
        AirPlayPersistence.saveElantraBluetoothVoice(app, false)
        assertFalse(AirPlayPersistence.loadElantraBluetoothVoice(app))
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        assertFalse(AirPlayPersistence.loadAudioFocusEnabled(app))
        assertEquals(UsbChargingCurrent.DATA_ONLY, AirPlayPersistence.loadUsbChargingCurrent(app))
        ShadowBuild.setModel("BHCN-T3105C")
        AirPlayPersistence.saveElantraBluetoothVoice(app, true)
        assertFalse(AirPlayPersistence.loadElantraBluetoothVoice(app))
    }

    @Test fun customCarLabelSurvivesMigration() {
        prefs.edit().putString("oem_label", "My Elantra").commit()
        assertEquals("My Elantra", AirPlayPersistence.loadOemLabel(app))
    }

    @Test fun otherModelsKeepTheirSavedAudioChoices() {
        ShadowBuild.setModel("BHCN-T3105C")
        prefs.edit().putBoolean("car_bluetooth_audio", true)
            .putBoolean("audio_focus_enabled", true).putInt("media_audio_channel", 3).commit()
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(app))
        assertTrue(AirPlayPersistence.loadAudioFocusEnabled(app))
        assertFalse(AirPlayPersistence.loadDisableMediaKeyAudioFocus(app))
        assertEquals(3, AirPlayPersistence.loadMediaAudioChannel(app))
        assertFalse(prefs.contains("elantra_third_party_defaults_v2"))
    }
}
