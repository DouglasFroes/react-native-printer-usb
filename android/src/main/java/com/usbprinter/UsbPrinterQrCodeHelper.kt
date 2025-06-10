package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterQrCodeHelper {
    fun printQrCode(context: Context, options: ReadableMap, device: UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val size = if (options.hasKey("size")) options.getDouble("size") else 6.0
        val align = if (options.hasKey("align")) options.getString("align") else null
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            // Alinhamento
            when (align) {
                "center" -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x01), 3, 2000)
                "right" -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x02), 3, 2000)
                else -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x00), 3, 2000)
            }
            val storeLen = text.toByteArray(Charsets.UTF_8).size + 3
            val pL = (storeLen % 256).toByte()
            val pH = (storeLen / 256).toByte()
            val model = byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)
            val sizeCmd = byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toInt().toByte())
            val store = byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30) + text.toByteArray(Charsets.UTF_8)
            val print = byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
            connection.bulkTransfer(endpoint, model, model.size, 2000)
            connection.bulkTransfer(endpoint, sizeCmd, sizeCmd.size, 2000)
            connection.bulkTransfer(endpoint, store, store.size, 2000)
            connection.bulkTransfer(endpoint, print, print.size, 2000)
            result.putBoolean("success", true)
            result.putString("message", "QR Code impresso com sucesso.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir QR Code: ${e.localizedMessage}")
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
