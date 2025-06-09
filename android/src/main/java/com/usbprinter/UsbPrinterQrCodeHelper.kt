package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise

object UsbPrinterQrCodeHelper {
    fun printQrCode(context: Context, text: String, size: Double, productId: Int, promise: Promise, device: android.hardware.usb.UsbDevice) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
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
            connection.releaseInterface(usbInterface)
            connection.close()
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            result.putString("message", "QR Code impresso.")
            promise.resolve(result)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir QR Code: ${e.localizedMessage}")
            promise.resolve(result)
        }
    }
}
