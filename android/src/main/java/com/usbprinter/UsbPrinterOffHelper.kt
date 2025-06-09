package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise

/**
 * Desconecta da impressora USB, libera recursos e encerra a sessão USB.
 * Observação: Não desliga fisicamente a impressora, apenas envia comandos de avanço/corte e fecha a conexão.
 */
object UsbPrinterOffHelper {
    fun off(context: Context, productId: Int, promise: Promise, device: android.hardware.usb.UsbDevice) {
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val cut = byteArrayOf(0x1D, 0x56, 0x00)
            val feed = byteArrayOf(0x1B, 0x64, 0x05)
            connection.bulkTransfer(endpoint, feed, feed.size, 2000)
            connection.bulkTransfer(endpoint, cut, cut.size, 2000)
            result.putBoolean("success", true)
            result.putString("message", "Comando de desligar enviado (avanço e corte). Conexão encerrada.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao desligar: ${e.localizedMessage}")
        } finally {
            try {
                connection?.releaseInterface(device.getInterface(0))
            } catch (_: Exception) {}
            try {
                connection?.close()
            } catch (_: Exception) {}
            promise.resolve(result)
        }
    }
}
