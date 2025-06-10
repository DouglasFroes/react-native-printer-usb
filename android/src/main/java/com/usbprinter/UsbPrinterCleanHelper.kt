package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object UsbPrinterCleanHelper {
    /**
     * Limpa completamente a impressora e a sessão USB:
     * - Avança o papel (limpa buffer de impressão)
     * - Envia comando de reset ESC @ (limpa buffer interno da impressora)
     * - Libera interface e fecha conexão USB (limpa recursos do Android)
     *
     * Observação: Para limpeza total, recomenda-se também limpar variáveis/estados do lado do app após chamar este método.
     */
    fun clean(context: Context, device: UsbDevice): WritableMap {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        val result = Arguments.createMap()
        try {
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val feed = byteArrayOf(0x1B, 0x64, 0x05) // Avanço de papel
            val reset = byteArrayOf(0x1B, 0x40) // ESC @ (reset)
            connection.bulkTransfer(endpoint, feed, feed.size, 2000)
            connection.bulkTransfer(endpoint, reset, reset.size, 2000)
            result.putBoolean("success", true)
            result.putString("message", "Limpeza completa: buffer da impressora e conexão Android resetados.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao limpar: ${e.localizedMessage}")
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
