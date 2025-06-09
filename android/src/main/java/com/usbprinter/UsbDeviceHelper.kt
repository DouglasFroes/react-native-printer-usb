package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbDeviceHelper(private val context: Context) {
    fun getConnectedUsbDevices(): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.toList()
    }
}
