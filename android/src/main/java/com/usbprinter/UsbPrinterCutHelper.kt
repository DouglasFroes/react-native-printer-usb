package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise

object UsbPrinterCutHelper {
    fun printCut(context: Context, tailingLine: Boolean, beep: Boolean, productId: Int, promise: Promise, device: android.hardware.usb.UsbDevice) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val cut = byteArrayOf(0x1D, 0x56, 0x00)
            connection.bulkTransfer(endpoint, cut, cut.size, 2000)
            if (tailingLine) {
                val lf = byteArrayOf(0x0A, 0x0A)
                connection.bulkTransfer(endpoint, lf, lf.size, 2000)
            }
            if (beep) {
                val beepCmd = byteArrayOf(0x1B, 0x42, 0x03, 0x02)
                connection.bulkTransfer(endpoint, beepCmd, beepCmd.size, 2000)
            }
            connection.releaseInterface(usbInterface)
            connection.close()
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            result.putString("message", "Corte realizado.")
            promise.resolve(result)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao cortar: ${e.localizedMessage}")
            promise.resolve(result)
        }
    }
}
