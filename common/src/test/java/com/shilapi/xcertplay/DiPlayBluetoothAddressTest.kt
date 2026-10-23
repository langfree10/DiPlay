package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test

class DiPlayBluetoothAddressTest {
    @Test fun maskedPublicAddressCanFallBackToTheVendorProperty() {
        assertEquals("10:20:30:40:50:60", DiPlayBluetooth.firstUsableAddress(
            "02:00:00:00:00:00", null, "10:20:30:40:50:60",
        ))
    }

    @Test fun validStandardAddressWinsAndInvalidValuesAreNeverAdvertised() {
        assertEquals("10:20:30:40:50:60", DiPlayBluetooth.firstUsableAddress(
            "10:20:30:40:50:60", "20:30:40:50:60:70",
        ))
        assertNull(DiPlayBluetooth.firstUsableAddress(null, "", "not a MAC", "00:00:00:00:00:00", "02:00:00:00:00:00"))
    }
}
