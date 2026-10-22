package com.shilapi.xcertplay

import android.os.SystemClock
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.media.ElantraAudioCompatibility
import com.shilapi.xcertplay.orchestration.CarPlayController
import java.lang.ref.WeakReference

/** Third-party CarLife handles these OEM keys on release, including release-only events. */
internal class ElantraTrackPresses {
    enum class Result { PASS, CONSUMED, SEND }
    private data class Key(val device: Int, val code: Int, val scan: Int)
    private data class Release(val downTime: Long, val eventTime: Long)
    private val presses = mutableMapOf<Key, Boolean>()
    private val releases = linkedMapOf<Key, Release>()

    fun onKey(event: KeyEvent, sessionActive: Boolean, inCall: Boolean): Result {
        if (event.keyCode !in 274..275 ||
            (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP)) return Result.PASS
        val key = Key(event.deviceId, event.keyCode, event.scanCode)
        val eligible = sessionActive && !inCall
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                presses[key] = eligible
                releases.remove(key)
            }
            return if (presses[key] == true) Result.CONSUMED else Result.PASS
        }
        val beganHere = presses.remove(key)
        if (!eligible || beganHere == false) return if (beganHere == true) Result.CONSUMED else Result.PASS
        if (event.isCanceled) return Result.CONSUMED
        val release = Release(event.downTime, event.eventTime)
        if (releases.put(key, release) == release) return Result.CONSUMED
        while (releases.size > 16) releases.remove(releases.keys.first())
        return Result.SEND
    }
}

internal enum class ElantraTrackRoute { WINDOW, MEDIA_SESSION }
internal enum class ElantraTrackDelivery { SENT, DUPLICATE, UNAVAILABLE }

/**
 * Pair mirrored app deliveries, not successive taps. Real test7 presses reached the window and
 * MediaSession 30 ms apart. Same-route commands remain distinct even inside this pairing window.
 */
internal class ElantraTrackCommandPairing {
    private data class Command(val index: Int, val at: Long, val routes: MutableSet<ElantraTrackRoute>)
    private var owner = WeakReference<Any>(null)
    private var lastTime = Long.MIN_VALUE
    private val recent = mutableListOf<Command>()

    @Synchronized
    fun send(owner: Any, index: Int, route: ElantraTrackRoute, now: Long, transmit: () -> Boolean): ElantraTrackDelivery {
        if (this.owner.get() !== owner || now < lastTime) recent.clear()
        this.owner = WeakReference(owner)
        lastTime = now
        recent.removeAll { now - it.at > PAIR_MILLIS }
        val mirror = recent.firstOrNull { it.index == index && route !in it.routes }
        if (mirror != null) {
            mirror.routes.add(route)
            return ElantraTrackDelivery.DUPLICATE
        }
        if (!transmit()) return ElantraTrackDelivery.UNAVAILABLE
        recent.add(Command(index, now, mutableSetOf(route)))
        while (recent.size > 16) recent.removeAt(0)
        return ElantraTrackDelivery.SENT
    }

    private companion object { const val PAIR_MILLIS = 120L }
}

internal object ElantraTrackCommands {
    private val pairing = ElantraTrackCommandPairing()

    @Synchronized
    fun send(controller: CarPlayController?, index: Int, route: ElantraTrackRoute): ElantraTrackDelivery {
        controller ?: return ElantraTrackDelivery.UNAVAILABLE
        if (!ElantraAudioCompatibility.supported() ||
            (index != CarPlayMediaButton.NEXT && index != CarPlayMediaButton.PREVIOUS)) {
            return if (controller.sendMediaButton(index)) ElantraTrackDelivery.SENT else ElantraTrackDelivery.UNAVAILABLE
        }
        return pairing.send(controller, index, route, SystemClock.elapsedRealtime()) { controller.sendMediaButton(index) }
    }
}
