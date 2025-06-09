package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise

/**
 * Envia dados brutos (comando ESC/POS em base64) diretamente para a impressora USB.
 * @param context Contexto Android
 * @param base64Data Dados ESC/POS codificados em base64
 * @param promise Promise para retorno do resultado
 * @param device UsbDevice já autorizado
 */
object UsbPrinterRawHelper {
    fun sendRawData(context: Context, base64Data: String, promise: Promise, device: android.hardware.usb.UsbDevice) {
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val rawBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val transferred = connection.bulkTransfer(endpoint, rawBytes, rawBytes.size, 2000)
            result.putBoolean("success", transferred == rawBytes.size)
            result.putString("message", if (transferred == rawBytes.size) "Dados enviados com sucesso." else "Nem todos os dados foram enviados.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao enviar dados brutos: ${e.localizedMessage}")
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
