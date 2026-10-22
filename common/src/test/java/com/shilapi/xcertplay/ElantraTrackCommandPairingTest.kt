package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.CarPlayMediaButton.NEXT
import com.shilapi.xcertplay.airplay.CarPlayMediaButton.PREVIOUS
import com.shilapi.xcertplay.ElantraTrackDelivery.*
import com.shilapi.xcertplay.ElantraTrackRoute.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class ElantraTrackCommandPairingTest {
    private val pairing = ElantraTrackCommandPairing()
    private val owner = Any()
    private val sent = mutableListOf<Int>()
    private fun send(route: ElantraTrackRoute, at: Long, index: Int = NEXT, target: Any = owner) =
        pairing.send(target, index, route, at) { sent.add(index); true }

    @Test fun replaysBothRealTest7ThirtyMillisecondDuplicatesAsOneHidEach() {
        // 21:48:07.883 / .913 and 21:48:54.121 / .151 from the user's test7 log.
        assertEquals(SENT, send(WINDOW, 7_883))
        assertEquals(DUPLICATE, send(MEDIA_SESSION, 7_913))
        assertEquals(SENT, send(WINDOW, 54_121))
        assertEquals(DUPLICATE, send(MEDIA_SESSION, 54_151))
        assertEquals(listOf(NEXT, NEXT), sent)
    }

    @Test fun mediaCallbackCanArriveBeforeWindow() {
        assertEquals(SENT, send(MEDIA_SESSION, 100))
        assertEquals(DUPLICATE, send(WINDOW, 130))
        assertEquals(listOf(NEXT), sent)
    }

    @Test fun singleRouteRapidTapsAlwaysRemainSeparate() {
        for (route in ElantraTrackRoute.entries) {
            send(route, 100, target = route)
            assertEquals(SENT, send(route, 101, target = route))
        }
        assertEquals(4, sent.size)
    }

    @Test fun burstMirrorsPairInOrderWithoutDroppingSecondTap() {
        send(WINDOW, 100)
        send(WINDOW, 110)
        assertEquals(DUPLICATE, send(MEDIA_SESSION, 130))
        assertEquals(DUPLICATE, send(MEDIA_SESSION, 140))
        assertEquals(listOf(NEXT, NEXT), sent)
    }

    @Test fun oppositeDirectionAndLateCommandsStaySeparate() {
        send(WINDOW, 100)
        assertEquals(SENT, send(MEDIA_SESSION, 101, PREVIOUS))
        assertEquals(SENT, send(MEDIA_SESSION, 221))
        assertEquals(listOf(NEXT, PREVIOUS, NEXT), sent)
    }

    @Test fun failedTransportDoesNotSuppressOtherRouteRetry() {
        assertEquals(UNAVAILABLE, pairing.send(owner, NEXT, WINDOW, 100) { false })
        assertEquals(SENT, send(MEDIA_SESSION, 130))
        assertEquals(listOf(NEXT), sent)
    }

    @Test fun newControllerAndBackwardsClockCannotInheritOldSuppression() {
        send(WINDOW, 100)
        assertEquals(SENT, send(MEDIA_SESSION, 101, target = Any()))
        assertEquals(SENT, send(WINDOW, 50))
        assertEquals(3, sent.size)
    }

    @Test fun simultaneousRoutesSubmitOnlyOnce() {
        val pool = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val count = AtomicInteger()
        try {
            val futures = ElantraTrackRoute.entries.map { route -> pool.submit<ElantraTrackDelivery> {
                ready.countDown()
                check(start.await(5, TimeUnit.SECONDS))
                pairing.send(owner, NEXT, route, 100) { count.incrementAndGet(); true }
            } }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            assertEquals(setOf(SENT, DUPLICATE), futures.map { it.get(5, TimeUnit.SECONDS) }.toSet())
            assertEquals(1, count.get())
        } finally { start.countDown(); pool.shutdownNow() }
    }
}
