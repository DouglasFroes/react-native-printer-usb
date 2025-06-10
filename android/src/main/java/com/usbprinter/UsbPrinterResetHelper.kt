package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object UsbPrinterResetHelper {
    fun reset(context: Context, device: UsbDevice): WritableMap {
        val result = Arguments.createMap()
        val connection = (context.getSystemService(Context.USB_SERVICE) as UsbManager).openDevice(device)
        if (connection != null) {
            try {
                val usbInterface = device.getInterface(0)
                connection.claimInterface(usbInterface, true)
                val endpoint = usbInterface.getEndpoint(0)
                val resetCmd = byteArrayOf(0x1B, 0x40) // ESC @
                val sent = connection.bulkTransfer(endpoint, resetCmd, resetCmd.size, 2000)
                result.putBoolean("success", sent > 0)
                result.putString("message", if (sent > 0) "Reset enviado com sucesso." else "Falha ao enviar reset.")
                // Tenta resetar o contexto USB
                try {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    usbManager.requestPermission(device, null) // Remove permissões antigas
                } catch (_: Exception) {}
            } catch (e: Exception) {
                result.putBoolean("success", false)
                result.putString("message", "Erro ao enviar reset: ${e.localizedMessage}")
            } finally {
                try { connection.releaseInterface(device.getInterface(0)) } catch (_: Exception) {}
                try { connection.close() } catch (_: Exception) {}
                // Tenta desconectar a impressora do sistema
                try {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val method = usbManager.javaClass.getDeclaredMethod("disconnect", UsbDevice::class.java)
                    method.isAccessible = true
                    method.invoke(usbManager, device)
                } catch (_: Exception) {}
            }
        } else {
            result.putBoolean("success", false)
            result.putString("message", "Não foi possível abrir conexão USB.")
        }
        return result
    }
}
