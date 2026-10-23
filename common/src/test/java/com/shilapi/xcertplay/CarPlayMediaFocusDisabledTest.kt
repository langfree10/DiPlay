package com.shilapi.xcertplay

import android.media.session.MediaSession
import android.os.Looper
import com.shilapi.xcertplay.media.CarPlayNowPlaying
import com.shilapi.xcertplay.orchestration.CarPlayController
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

/** An opt-in compatibility mode keeps media keys active without owning audio focus. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25, 28, 33], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class CarPlayMediaFocusDisabledTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val controllers = mutableListOf<CarPlayController>()

    @Before fun disableMediaKeyFocus() {
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, true)
    }

    @After fun cleanup() {
        AirPlayPersistence.saveCarBluetoothAudio(app, false)
        controllers.forEach(CarPlayMediaKeys::detach)
        CarPlayBackgroundSession.clear()
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, false)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun elantraDefaultsKeepMediaSessionActiveWithoutTakingTheNativeMusicFocus() {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        org.robolectric.shadows.ShadowBuild.setManufacturer("humaxdigital")
        org.robolectric.shadows.ShadowBuild.setModel("BHCN-T3103C")
        try {
            assertFalse(AirPlayPersistence.loadCarBluetoothAudio(app))
            assertFalse(AirPlayPersistence.loadAudioFocusEnabled(app))
            val session = start(mock(CarPlayController::class.java).also(controllers::add))
            assertTrue(session.isActive)
            assertFocusDisabled()
        } finally {
            org.robolectric.shadows.ShadowBuild.setManufacturer(manufacturer)
            org.robolectric.shadows.ShadowBuild.setModel(model)
        }
    }

    @Test fun nativeBluetoothFocusCannotSuppressMediaSessionOnlyTrackCommands() {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        org.robolectric.shadows.ShadowBuild.setManufacturer("humaxdigital")
        org.robolectric.shadows.ShadowBuild.setModel("BHCN-T3103C")
        try {
            AirPlayPersistence.saveCarBluetoothAudio(app, false)
            val controller = mock(CarPlayController::class.java).also(controllers::add)
            start(controller)
            val callback = ReflectionHelpers.getField<CarPlayMediaCallback>(CarPlayMediaKeys, "callback")
            android.provider.Settings.Global.putInt(app.contentResolver, ElantraVoiceSourceReturn.LAST_MODE, 8)
            android.provider.Settings.Global.putInt(app.contentResolver, "android.extension.car.BT_AUDIO_FOCUS_STATE", 2)
            callback.onSkipToNext(); callback.onSkipToPrevious()
            val event = android.view.KeyEvent(0, 1, android.view.KeyEvent.ACTION_UP, 275, 0)
            callback.onMediaButtonEvent(android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON)
                .putExtra(android.content.Intent.EXTRA_KEY_EVENT, event))
            org.mockito.Mockito.verify(controller, org.mockito.Mockito.times(2)).sendMediaButton(com.shilapi.xcertplay.airplay.CarPlayMediaButton.NEXT)
            org.mockito.Mockito.verify(controller).sendMediaButton(com.shilapi.xcertplay.airplay.CarPlayMediaButton.PREVIOUS)
            android.provider.Settings.Global.putInt(app.contentResolver, ElantraVoiceSourceReturn.LAST_MODE, 6)
            callback.onSkipToNext()
            org.mockito.Mockito.verify(controller, org.mockito.Mockito.times(3)).sendMediaButton(com.shilapi.xcertplay.airplay.CarPlayMediaButton.NEXT)
        } finally {
            org.robolectric.shadows.ShadowBuild.setManufacturer(manufacturer)
            org.robolectric.shadows.ShadowBuild.setModel(model)
        }
    }

    @Test fun mediaAudioCreatesActiveSessionWithoutFocusRequest() {
        val session = start(mock(CarPlayController::class.java).also(controllers::add))

        assertTrue(session.isActive)
        assertFocusDisabled()
    }

    @Test fun bluetoothAudioNeverCreatesALocalMediaSessionOrFocusRequest() {
        AirPlayPersistence.saveCarBluetoothAudio(app, true)
        val controller = mock(CarPlayController::class.java).also(controllers::add)
        `when`(controller.nowPlayingSnapshot()).thenReturn(CarPlayNowPlaying())
        CarPlayMediaKeys.attach(app, controller)
        CarPlayMediaKeys.onMediaAudioChanged(true)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(currentSession())
        assertFocusDisabled()
    }

    @Test fun repeatedMediaActivityDoesNotRequestFocusOrReplaceSession() {
        val controller = mock(CarPlayController::class.java).also(controllers::add)
        val session = start(controller)

        CarPlayMediaKeys.onMediaAudioChanged(true)
        shadowOf(Looper.getMainLooper()).idle()

        assertSame(session, currentSession())
        assertFocusDisabled()
    }

    @Test fun replacementControllerRetiresSessionWithoutClaimingFocus() {
        val first = mock(CarPlayController::class.java).also(controllers::add)
        val firstSession = start(first)
        val second = mock(CarPlayController::class.java).also(controllers::add)

        val secondSession = start(second)

        assertFalse(firstSession.isActive)
        assertNotSame(firstSession, secondSession)
        assertTrue(secondSession.isActive)
        assertFocusDisabled()
    }

    @Test fun detachReleasesSessionAndLeavesFocusUnowned() {
        val controller = mock(CarPlayController::class.java).also(controllers::add)
        val session = start(controller)

        CarPlayMediaKeys.detach(controller)
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(session.isActive)
        assertNull(currentSession())
        assertFocusDisabled()
    }

    private fun start(controller: CarPlayController): MediaSession {
        `when`(controller.nowPlayingSnapshot()).thenReturn(CarPlayNowPlaying())
        CarPlayMediaKeys.attach(app, controller)
        CarPlayMediaKeys.onMediaAudioChanged(true)
        shadowOf(Looper.getMainLooper()).idle()
        assertFocusDisabled()
        return currentSession().also(::assertNotNull)!!
    }

    private fun currentSession(): MediaSession? =
        ReflectionHelpers.getField(CarPlayMediaKeys, "session")

    private fun assertFocusDisabled() {
        assertNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusRequest"))
        assertFalse(ReflectionHelpers.getField(CarPlayMediaKeys, "focusHeld"))
        assertNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusOwner"))
    }
}
