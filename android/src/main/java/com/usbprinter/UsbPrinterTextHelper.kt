package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

class UsbPrinterTextHelper(private val context: Context) {
    fun printText(text: String, device: UsbDevice): WritableMap {
        val result = Arguments.createMap()
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            if (!usbManager.hasPermission(device)) {
                result.putBoolean("success", false)
                result.putString("message", "Sem permissão para o dispositivo USB")
                return result
            }
            val usbInterface: UsbInterface? = (0 until device.interfaceCount)
                .map { device.getInterface(it) }
                .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }
                ?: device.getInterface(0)
            val endpoint: UsbEndpoint? = usbInterface?.let {
                (0 until it.endpointCount)
                    .map { idx -> it.getEndpoint(idx) }
                    .firstOrNull { ep -> ep.direction == UsbConstants.USB_DIR_OUT }
            }
            if (usbInterface == null || endpoint == null) {
                result.putBoolean("success", false)
                result.putString("message", "Interface ou endpoint não encontrado")
                return result
            }
            val connection = usbManager.openDevice(device) ?: run {
                result.putBoolean("success", false)
                result.putString("message", "Não foi possível abrir conexão com o dispositivo")
                return result
            }
            connection.claimInterface(usbInterface, true)
            val bytes = (text + "\n").toByteArray(Charsets.UTF_8)
            val transfered = connection.bulkTransfer(endpoint, bytes, bytes.size, 2000)
            connection.releaseInterface(usbInterface)
            connection.close()
            if (transfered > 0) {
                result.putBoolean("success", true)
            } else {
                result.putBoolean("success", false)
                result.putString("message", "Nada foi impresso (bulkTransfer retornou $transfered)")
            }
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", e.message)
        }
        return result
    }
}
