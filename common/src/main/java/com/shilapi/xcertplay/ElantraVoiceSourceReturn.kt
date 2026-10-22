package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Restores the selected native media source after Siri releases speech, without opening a media UI.
 * Bluetooth uses the same unprotected focus request as BTACoreManager, then the native unmute/play
 * controls. Protected LAST_MODE_BT_AUDIO broadcasts cannot be sent by an ordinary installed app.
 * No Global settings are written and DiPlay's music/media-key focus stays unchanged.
 */
internal class ElantraVoiceSourceReturn(
    private val currentMode: () -> Int,
    private val ready: () -> Boolean,
    private val restore: (Int) -> Unit,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val ownerActive: () -> Boolean,
    private val report: (String) -> Unit = {},
    private val voiceActiveChanged: (Boolean) -> Unit = {},
    private val isRestored: (Int) -> Boolean = { true },
    private val didRestore: (Int) -> Unit = {},
) {
    private var speech = false
    private var call = false
    private var suppressUntilIdle = false
    private var savedMode: Int? = null
    private var generation = 0
    private var closed = false

    fun modes(params: Map<String, Any?>) {
        if (closed || !ownerActive()) return
        val states = params["appStates"] as? List<*> ?: return
        val entries = states.filterIsInstance<Map<*, *>>()
        entries.firstOrNull { (it["appStateID"] as? Number)?.toInt() == 2 }?.let {
            call = (it["entity"] as? Number)?.toInt()?.let { entity -> entity != 0 }
                ?: (it["state"] as? Boolean) ?: call
        }
        if (call) {
            suppressUntilIdle = true
            speech = false
            voiceActiveChanged(false)
            savedMode = null
            generation++
            report("Elantra Siri return deferred: telephone owns audio")
            return
        }
        val entry = entries.firstOrNull { (it["appStateID"] as? Number)?.toInt() == 1 } ?: return
        val mode = (entry["speechMode"] as? Number)?.toInt() ?: return
        // Some iOS versions clear entity with a not-applicable speechMode instead of mode 0.
        val entity = (entry["entity"] as? Number)?.toInt()
        if (mode !in -1..2 && entity != 0) return
        val active = entity != 0 && (mode == 1 || mode == 2)
        if (!active) suppressUntilIdle = false
        if (suppressUntilIdle) return
        if (active != speech) voiceActiveChanged(active)
        if (active && !speech) {
            generation++
            savedMode = currentMode().takeIf { it in SOURCES }
            report("Elantra Siri started previousSource=${savedMode ?: "unknown"}")
        } else if (!active && speech) {
            val target = savedMode
            val token = ++generation
            if (target != null) restoreWhenReleased(target, token, 0)
        }
        speech = active
    }

    private fun restoreWhenReleased(target: Int, token: Int, attempt: Int) {
        schedule(RESTORE_DELAY_MILLIS) {
            if (closed || token != generation || !ownerActive() || speech || call) return@schedule
            val selected = currentMode()
            if (selected in MEDIA_SOURCES && selected != target) {
                savedMode = null
                report("Elantra Siri return cancelled: source was changed manually")
                return@schedule
            }
            if (!ready()) {
                if (attempt < MAX_WAIT_ATTEMPTS) restoreWhenReleased(target, token, attempt + 1)
                else report("Elantra Siri return skipped: voice/call or vehicle still busy")
                return@schedule
            }
            requestAndConfirm(target, token, 0)
        }
    }

    private fun stillPending(target: Int, token: Int): Boolean {
        if (closed || token != generation || !ownerActive() || speech || call) return false
        val selected = currentMode()
        if (selected in MEDIA_SOURCES && selected != target) {
            savedMode = null
            report("Elantra Siri return cancelled: source was changed manually")
            return false
        }
        return true
    }

    private fun requestAndConfirm(target: Int, token: Int, attempt: Int) {
        if (!stillPending(target, token) || !ready()) return
        restore(target)
        report("Elantra Siri return requested source=$target attempt=${attempt + 1}")
        confirm(target, token, attempt, 0)
    }

    private fun confirm(target: Int, token: Int, attempt: Int, poll: Int) {
        if (!stillPending(target, token)) return
        if (!ready()) {
            // A new native voice/call window must never be interrupted by an old retry.
            report("Elantra Siri return suspended: native voice/call became busy")
            return
        }
        if (isRestored(target)) {
            savedMode = null
            didRestore(target)
            report("Elantra Siri native audio focus confirmed source=$target")
        } else if (poll < CONFIRM_POLLS) {
            schedule(CONFIRM_DELAY_MILLIS) { confirm(target, token, attempt, poll + 1) }
        } else if (attempt < MAX_RESTORE_RETRIES) {
            schedule(CONFIRM_DELAY_MILLIS) { requestAndConfirm(target, token, attempt + 1) }
        } else {
            savedMode = null
            report("Elantra Siri return not confirmed after bounded retries source=$target")
        }
    }

    fun close() {
        closed = true; savedMode = null; generation++
        if (speech) voiceActiveChanged(false)
        speech = false
    }

    companion object {
        const val RESTORE_DELAY_MILLIS = 750L
        private const val MAX_WAIT_ATTEMPTS = 11
        private const val CONFIRM_DELAY_MILLIS = 350L
        private const val CONFIRM_POLLS = 2
        private const val MAX_RESTORE_RETRIES = 2
        const val LAST_MODE = "android.extension.car.LAST_MEDIA_MODE"
        // Audio sources only. Never start video, phone projection, or an online app on a timer.
        private val MEDIA_SOURCES = 1..14
        internal val SOURCES = mapOf(
            4 to ("com.humaxdigital.media.usbmusic" to "USB_MUSIC"),
            6 to ("com.humaxdigital.media.mymusic" to "MY_MUSIC"),
            8 to ("com.humaxdigital.automotive.btaudio" to "BT_AUDIO"),
            9 to ("com.humaxdigital.automotive.btaudio" to "BT_AUDIO_2"),
        )

        fun create(context: Context, ownerActive: () -> Boolean, report: (String) -> Unit,
            voiceActiveChanged: (Boolean) -> Unit = {}): ElantraVoiceSourceReturn {
            val app = context.applicationContext
            val handler = Handler(Looper.getMainLooper())
            val manager = app.getSystemService(AudioManager::class.java)
            fun read(key: String, fallback: Int) = runCatching {
                Settings.Global.getInt(app.contentResolver, key, fallback)
            }.getOrDefault(fallback)
            return ElantraVoiceSourceReturn(
                currentMode = { read(LAST_MODE, 0) },
                ready = {
                    @Suppress("DEPRECATION")
                    val sco = manager?.isBluetoothScoOn == true
                    read("android.extension.car.POWER_STATE", 6) == 6 &&
                        read("vr_shown", 0) == 0 &&
                        read("android.extension.car.VOLUME_SETTING_STATUS", 0) == 0 &&
                        manager?.mode == AudioManager.MODE_NORMAL && !sco
                },
                restore = { mode ->
                    val (pkg, suffix) = SOURCES.getValue(mode)
                    runCatching {
                        if (mode == 8 || mode == 9) {
                            // These actions have no protected-broadcast declarations in any of the
                            // 125 inspected firmware APKs. The native BT app requests focus with the
                            // first action too; it supplies the platform's own LAS_BT_AUDIO routing.
                            app.sendBroadcast(Intent("action_request_a2dpsink_audio_focus").setPackage("com.android.bluetooth"))
                            report("Elantra Siri BT restore: focus request sent; focus=${read("android.extension.car.BT_AUDIO_FOCUS_STATE", -1)}")
                        } else {
                            // USB/My Music LAST_MODE actions are unprotected in the inspected build.
                            app.sendBroadcast(Intent("com.humaxdigital.automotive.ACTION_LAST_MODE_$suffix").setPackage(pkg))
                        }
                    }.onFailure { report("Elantra Siri return unavailable: ${it.javaClass.simpleName}") }
                },
                schedule = { delay, action -> handler.postDelayed({ action() }, delay); Unit },
                ownerActive = ownerActive,
                report = { message ->
                    report("$message [source=${read(LAST_MODE, -1)} btFocus=${read("android.extension.car.BT_AUDIO_FOCUS_STATE", -1)} " +
                        "mode=${manager?.mode} sco=${manager?.isBluetoothScoOn} vr=${read("vr_shown", -1)}]")
                },
                voiceActiveChanged = voiceActiveChanged,
                isRestored = { mode -> if (mode == 8 || mode == 9)
                    read("android.extension.car.BT_AUDIO_FOCUS_STATE", 0) > 0 else read(LAST_MODE, 0) == mode },
                didRestore = { mode ->
                    if (mode == 8 || mode == 9) {
                        val pkg = SOURCES.getValue(mode).first
                        runCatching {
                            app.sendBroadcast(Intent("com.humaxdigital.automotive.btaudio.AUDIO_STREAM_PLAY").setPackage(pkg))
                            if (read("last_BTMedia_state", 0) == 1)
                                app.sendBroadcast(Intent("com.humaxdigital.automotive.btaudio.play").setPackage(pkg))
                        }.onFailure { report("Elantra Siri native resume unavailable: ${it.javaClass.simpleName}") }
                    }
                },
            )
        }
    }
}
