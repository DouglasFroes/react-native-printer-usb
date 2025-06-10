package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object UsbPrinterCutHelper {
    fun printCut(context: Context, tailingLine: Boolean, beep: Boolean, device: UsbDevice): WritableMap {
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            if (tailingLine) {
                val feed = byteArrayOf(0x1B, 0x64, 0x05)
                connection.bulkTransfer(endpoint, feed, feed.size, 2000)
            }
            if (beep) {
                val beepCmd = byteArrayOf(0x1B, 0x42, 0x03, 0x01)
                connection.bulkTransfer(endpoint, beepCmd, beepCmd.size, 2000)
            }
            val cut = byteArrayOf(0x1D, 0x56, 0x00)
            connection.bulkTransfer(endpoint, cut, cut.size, 2000)
            result.putBoolean("success", true)
            result.putString("message", "Corte realizado com sucesso.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao cortar: ${e.localizedMessage}")
        } finally {
            try {
                connection?.releaseInterface(device.getInterface(0))
            } catch (_: Exception) {}
            try {
                connection?.close()
            } catch (_: Exception) {}
        }
        return result
    }
}
