package com.shilapi.xcertplay

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ElantraTrackPressesTest {
    private val presses = ElantraTrackPresses()
    private fun key(action: Int, time: Long = 1, repeat: Int = 0, flags: Int = 0) =
        KeyEvent(time, time + 1, action, 275, repeat, 0, 1, 0, flags)
    private fun route(event: KeyEvent, active: Boolean = true, call: Boolean = false) =
        presses.onKey(event, active, call)

    @Test fun releaseOnlyAndUnreliableDownTimeFollowThirdPartyCarLife() {
        assertEquals(ElantraTrackPresses.Result.SEND, route(key(KeyEvent.ACTION_UP)))
        route(key(KeyEvent.ACTION_DOWN, time = 10))
        assertEquals(ElantraTrackPresses.Result.SEND, route(key(KeyEvent.ACTION_UP, time = 11)))
    }

    @Test fun repeatedReleaseAndHoldNeverSkipTwiceButRapidTapsStillWork() {
        repeat(2) { index ->
            val time = index + 10L
            assertEquals(ElantraTrackPresses.Result.CONSUMED, route(key(KeyEvent.ACTION_DOWN, time)))
            assertEquals(ElantraTrackPresses.Result.CONSUMED, route(key(KeyEvent.ACTION_DOWN, time, repeat = 1)))
            assertEquals(ElantraTrackPresses.Result.SEND, route(key(KeyEvent.ACTION_UP, time)))
            assertEquals(ElantraTrackPresses.Result.CONSUMED, route(key(KeyEvent.ACTION_UP, time)))
        }
    }

    @Test fun canceledCallAndDisconnectedReleasesNeverSend() {
        route(key(KeyEvent.ACTION_DOWN))
        assertEquals(ElantraTrackPresses.Result.CONSUMED,
            route(key(KeyEvent.ACTION_UP, flags = KeyEvent.FLAG_CANCELED)))
        route(key(KeyEvent.ACTION_DOWN))
        assertEquals(ElantraTrackPresses.Result.CONSUMED, route(key(KeyEvent.ACTION_UP), call = true))
        route(key(KeyEvent.ACTION_DOWN))
        assertEquals(ElantraTrackPresses.Result.CONSUMED, route(key(KeyEvent.ACTION_UP), active = false))
        assertEquals(ElantraTrackPresses.Result.PASS, route(key(KeyEvent.ACTION_UP), active = false))
    }

    @Test fun aPressStartedDuringCallOrDisconnectionDoesNotBecomeACarPlayRelease() {
        assertEquals(ElantraTrackPresses.Result.PASS, route(key(KeyEvent.ACTION_DOWN), call = true))
        assertEquals(ElantraTrackPresses.Result.PASS, route(key(KeyEvent.ACTION_UP)))
        assertEquals(ElantraTrackPresses.Result.PASS, route(key(KeyEvent.ACTION_DOWN), active = false))
        assertEquals(ElantraTrackPresses.Result.PASS, route(key(KeyEvent.ACTION_UP)))
    }
}
