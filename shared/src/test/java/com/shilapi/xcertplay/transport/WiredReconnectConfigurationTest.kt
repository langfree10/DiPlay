package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class WiredReconnectConfigurationTest {
    private fun usbInterface(type: Int, subclass: Int, protocol: Int) =
        ReflectionHelpers.newInstance(UsbInterface::class.java).apply {
            ReflectionHelpers.setField(this, "mClass", type)
            ReflectionHelpers.setField(this, "mSubclass", subclass)
            ReflectionHelpers.setField(this, "mProtocol", protocol)
        }
    private fun config(id: Int, vararg interfaces: UsbInterface) =
        ReflectionHelpers.newInstance(UsbConfiguration::class.java).apply {
            ReflectionHelpers.setField(this, "mId", id)
            ReflectionHelpers.setField(this, "mInterfaces", interfaces)
        }
    private fun device(vararg configurations: UsbConfiguration) =
        ReflectionHelpers.newInstance(UsbDevice::class.java).apply {
            ReflectionHelpers.setField(this, "mConfigurations", configurations)
        }
    @Test fun reconnectUsesADescriptorBasedNonNetworkConfigurationRatherThanAssumingOne() {
        val mux = usbInterface(0xff, 0xfe, 2)
        val ncm = usbInterface(2, 0x0d, 0)
        val ethernet = usbInterface(0xff, 0xfd, 1)
        val ordinary = config(7, mux)
        val phone = device(config(1, mux, ncm), config(6, mux, ncm, ethernet), ordinary)
        assertSame(ordinary, IphoneCarPlayConfiguration.ordinaryUsbConfiguration(phone))
    }
    @Test fun aPhoneWithoutAnOrdinaryUsbmuxConfigurationKeepsTheExistingTransitionPath() {
        val mux = usbInterface(0xff, 0xfe, 2)
        val ncm = usbInterface(2, 0x0d, 0)
        assertNull(IphoneCarPlayConfiguration.ordinaryUsbConfiguration(device(config(6, mux, ncm))))
    }
}
