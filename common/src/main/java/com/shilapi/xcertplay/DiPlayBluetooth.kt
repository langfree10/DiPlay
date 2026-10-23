package com.shilapi.xcertplay

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.provider.Settings

internal object DiPlayBluetooth {
    fun localAddress(context: Context): String? {
        val adapter = runCatching { (context.getSystemService(BluetoothManager::class.java)?.adapter ?: BluetoothAdapter.getDefaultAdapter())?.address }.getOrNull()
        val setting = runCatching { Settings.Secure.getString(context.contentResolver, "bluetooth_address") }.getOrNull()
        // Humax's system CarLife can read the real adapter address; an ordinary app may only
        // receive 02:00:00:00:00:00. The owner's getprop export confirms this vendor property.
        // No hidden-API exemption is installed; denied access simply keeps the existing fallback.
        val vendor = if (com.shilapi.xcertplay.media.ElantraAudioCompatibility.supported()) runCatching {
            Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
                .invoke(null, "persist.service.bdroid.bdaddr") as? String
        }.getOrNull() else null
        return firstUsableAddress(adapter, setting, vendor).also { address ->
            val source = when {
                address == null -> "unavailable"
                address == adapter -> "adapter"
                address == setting -> "secure-setting"
                else -> "vendor-property"
            }
            WheelKeyLog.note("Bluetooth identity source=$source")
        }
    }

    internal fun firstUsableAddress(vararg candidates: String?): String? =
        candidates.filterNotNull().firstOrNull {
            Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}").matches(it) &&
                !it.startsWith("02:00:00:00:00:") && it != "00:00:00:00:00:00"
        }
}
