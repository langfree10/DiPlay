package com.shilapi.xcertplay

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Read-only observations: receiver-role profiles are distinct from a phone's source-role profiles. */
internal object CarBluetoothDiagnostics {
    @Volatile private var owner: Any? = null
    private var context: Context? = null
    private var receiver: BroadcastReceiver? = null

    @Synchronized fun start(app: Context, nextOwner: Any) {
        stop()
        owner = nextOwner
        context = app.applicationContext
        val observed = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (owner !== nextOwner) return
                // No device name, address, phone number or audio payload is retained.
                val adapterEvent = intent.action == BluetoothAdapter.ACTION_STATE_CHANGED
                WheelKeyLog.note("Bluetooth event=${intent.action?.substringAfter("android.bluetooth.")} " +
                    "state=${intent.getIntExtra(if (adapterEvent) BluetoothAdapter.EXTRA_STATE else BluetoothProfile.EXTRA_STATE, -1)} " +
                    "previous=${intent.getIntExtra(if (adapterEvent) BluetoothAdapter.EXTRA_PREVIOUS_STATE else BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)}")
            }
        }
        val filter = IntentFilter().apply {
            for (profile in listOf("a2dp", "a2dp-sink", "headset", "headsetclient")) {
                addAction("android.bluetooth.$profile.profile.action.CONNECTION_STATE_CHANGED")
                addAction("android.bluetooth.$profile.profile.action.PLAYING_STATE_CHANGED")
                addAction("android.bluetooth.$profile.profile.action.AUDIO_STATE_CHANGED")
            }
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(context!!, observed, filter, ContextCompat.RECEIVER_EXPORTED)
            receiver = observed
        }.onFailure { WheelKeyLog.note("Bluetooth diagnostics receiver unavailable=${it.javaClass.simpleName}") }
        val adapter = runCatching { app.getSystemService(BluetoothManager::class.java)?.adapter }.getOrNull()
            ?: return
        // Public Android constants expose only A2DP source/HFP AG. Humax receives A2DP and is an HFP client.
        for ((name, profile) in listOf("A2DP_SOURCE" to 2, "HFP_AG" to 1, "A2DP_SINK" to 11, "HFP_CLIENT" to 16)) {
            runCatching {
                val accepted = adapter.getProfileProxy(app.applicationContext, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(id: Int, proxy: BluetoothProfile) {
                        try {
                            if (owner === nextOwner) {
                                val count = runCatching { proxy.connectedDevices.size }.getOrNull()
                                WheelKeyLog.note("Bluetooth profile=$name connectedCount=${count ?: "unreadable"}")
                            }
                        } finally { adapter.closeProfileProxy(id, proxy) }
                    }
                    override fun onServiceDisconnected(id: Int) = Unit
                }, profile)
                if (!accepted) WheelKeyLog.note("Bluetooth profile=$name proxy unavailable")
            }.onFailure { WheelKeyLog.note("Bluetooth profile=$name unavailable=${it.javaClass.simpleName}") }
        }
    }

    @Synchronized fun stop() {
        owner = null
        receiver?.let { runCatching { context?.unregisterReceiver(it) } }
        receiver = null
        context = null
    }
}
