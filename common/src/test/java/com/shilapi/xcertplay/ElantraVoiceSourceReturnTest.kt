package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test

class ElantraVoiceSourceReturnTest {
    private var mode = 8
    private var ready = true
    private var owner = true
    private val queued = ArrayDeque<() -> Unit>()
    private val restored = mutableListOf<Int>()
    private val policy = ElantraVoiceSourceReturn({ mode }, { ready }, { restored.add(it) },
        { _, action -> queued.add(action) }, { owner })
    private fun states(speech: Int, call: Boolean = false, entity: Int = if (speech > 0) 1 else 0) =
        mapOf("appStates" to listOf(mapOf("appStateID" to 1, "entity" to entity, "speechMode" to speech),
            mapOf("appStateID" to 2, "entity" to if (call) 1 else 0)))
    private fun drain() { var count = 0; while (queued.isNotEmpty()) { check(count++ < 50); queued.removeFirst()() } }

    @Test fun recognizingToSpeakingDoesNotRestoreUntilSpeechIsIdle() {
        policy.modes(states(1)); policy.modes(states(2))
        assertTrue(queued.isEmpty())
        policy.modes(states(0)); drain()
        assertEquals(listOf(8), restored)
        policy.modes(states(0)); drain()
        assertEquals(listOf(8), restored)
    }
    @Test fun remembersOtherSelectedAudioSourcesAndDoesNotAssumeBluetooth() {
        mode = 6; policy.modes(states(1)); policy.modes(states(0)); drain()
        assertEquals(listOf(6), restored)
    }
    @Test fun waitsForScoOrNativeVoiceRouteToRelease() {
        policy.modes(states(1)); ready = false; policy.modes(states(0))
        repeat(3) { queued.removeFirst()() }
        assertTrue(restored.isEmpty())
        ready = true; drain(); assertEquals(listOf(8), restored)
    }
    @Test fun neverForcesMediaWhileVehicleRemainsBusy() {
        policy.modes(states(1)); ready = false; policy.modes(states(0)); drain()
        assertTrue(restored.isEmpty())
    }
    @Test fun manualSourceSelectionWins() {
        policy.modes(states(1)); mode = 1; policy.modes(states(0)); drain()
        assertTrue(restored.isEmpty())
    }
    @Test fun telephoneSpeechIsNotSiriAndSiriPlacingCallDoesNotStealTheCall() {
        policy.modes(states(1, true)); policy.modes(states(0)); drain()
        assertTrue(restored.isEmpty())
        policy.modes(states(1)); policy.modes(states(1, true)); policy.modes(states(0)); drain()
        assertTrue(restored.isEmpty())
    }
    @Test fun ignoresUnrelatedModesAndAcceptsExplicitEntityRelease() {
        policy.modes(states(1)); policy.modes(mapOf("resources" to emptyList<Any>()))
        assertTrue(queued.isEmpty())
        policy.modes(states(-1, entity = 0)); drain(); assertEquals(listOf(8), restored)
    }
    @Test fun nextSiriSessionCancelsPreviousDeferredRestoration() {
        policy.modes(states(1)); policy.modes(states(0)); policy.modes(states(1)); drain()
        assertTrue(restored.isEmpty())
        policy.modes(states(0)); drain(); assertEquals(listOf(8), restored)
    }
    @Test fun closedOrReplacedOwnerCannotRestore() {
        policy.modes(states(1)); policy.modes(states(0)); owner = false; drain()
        assertTrue(restored.isEmpty())
        owner = true; policy.modes(states(1)); policy.modes(states(0)); policy.close(); drain()
        assertTrue(restored.isEmpty())
    }
    @Test fun unknownSourceDoesNotFallBackToRadio() {
        mode = 12; policy.modes(states(1)); policy.modes(states(0)); drain()
        assertTrue(restored.isEmpty())
    }
    @Test fun waitsForAsynchronousAcknowledgementBeforeUnmutingOrResuming() {
        var acknowledged = false
        val completed = mutableListOf<Int>()
        val messages = mutableListOf<String>()
        val policy = ElantraVoiceSourceReturn({ mode }, { ready }, { restored.add(it) },
            { _, action -> queued.add(action) }, { owner }, messages::add,
            isRestored = { acknowledged }, didRestore = completed::add)
        policy.modes(states(1)); policy.modes(states(0))
        queued.removeFirst()()
        assertEquals(listOf(8), restored); assertTrue(completed.isEmpty())
        assertFalse(messages.any { it.contains("confirmed source") })
        acknowledged = true; drain()
        assertEquals(listOf(8), restored); assertEquals(listOf(8), completed)
    }
    @Test fun retriesAreBoundedAndNeverReportSuccessWithoutAcknowledgement() {
        val messages = mutableListOf<String>()
        var completed = false
        val policy = ElantraVoiceSourceReturn({ mode }, { ready }, { restored.add(it) },
            { _, action -> queued.add(action) }, { owner }, messages::add,
            isRestored = { false }, didRestore = { completed = true })
        policy.modes(states(1)); policy.modes(states(0)); drain()
        assertEquals(listOf(8, 8, 8), restored)
        assertFalse(completed)
        assertTrue(messages.last().contains("not confirmed after bounded retries"))
    }
    @Test fun manualSourceChangeAndNewCallCancelAnOutstandingAcknowledgement() {
        val policy = ElantraVoiceSourceReturn({ mode }, { ready }, { restored.add(it) },
            { _, action -> queued.add(action) }, { owner }, isRestored = { false },
            didRestore = { fail("Late acknowledgement stole the new source") })
        policy.modes(states(1)); policy.modes(states(0)); queued.removeFirst()()
        mode = 6; drain(); assertEquals(listOf(8), restored)
        mode = 8; restored.clear()
        policy.modes(states(1)); policy.modes(states(0)); queued.removeFirst()()
        policy.modes(states(1, call = true)); drain()
        assertEquals(listOf(8), restored)
    }
    @Test fun notApplicableSpeechWithoutEntityReleasesVoiceAndCallReleasesTemporaryFocus() {
        val focus = mutableListOf<Boolean>()
        val policy = ElantraVoiceSourceReturn({ mode }, { ready }, { restored.add(it) },
            { _, action -> queued.add(action) }, { owner }, voiceActiveChanged = focus::add)
        policy.modes(states(1))
        policy.modes(mapOf("appStates" to listOf(mapOf("appStateID" to 1, "speechMode" to -1))))
        drain(); assertEquals(listOf(true, false), focus); assertEquals(listOf(8), restored)
        policy.modes(states(1)); policy.modes(states(1, call = true))
        assertEquals(listOf(true, false, true, false), focus)
    }

}
