package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterBarcodeHelper {
    fun printBarcode(context: Context, options: ReadableMap, device: UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val width = if (options.hasKey("width")) options.getDouble("width") else 2.0
        val height = if (options.hasKey("height")) options.getDouble("height") else 100.0
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val setH = byteArrayOf(0x1D, 0x68, height.toInt().toByte())
            val setW = byteArrayOf(0x1D, 0x77, width.toInt().toByte())
            val selectCode128 = byteArrayOf(0x1D, 0x6B, 0x49, text.length.toByte())
            connection.bulkTransfer(endpoint, setH, setH.size, 2000)
            connection.bulkTransfer(endpoint, setW, setW.size, 2000)
            connection.bulkTransfer(endpoint, selectCode128 + text.toByteArray(Charsets.UTF_8), selectCode128.size + text.length, 2000)
            result.putBoolean("success", true)
            result.putString("message", "Código de barras impresso com sucesso.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir código de barras: ${e.localizedMessage}")
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
