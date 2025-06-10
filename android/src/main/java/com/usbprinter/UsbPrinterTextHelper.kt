package com.usbprinter

import android.hardware.usb.UsbDevice
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterTextHelper {
    fun printText(context: android.content.Context, options: ReadableMap, device: android.hardware.usb.UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val size = if (options.hasKey("size")) options.getInt("size") else null
        val align = if (options.hasKey("align")) options.getString("align") else null
        val encoding = if (options.hasKey("encoding")) options.getString("encoding") else null
        val bold = if (options.hasKey("bold")) options.getBoolean("bold") else null
        val font = if (options.hasKey("font")) options.getString("font") else null
        val cut = if (options.hasKey("cut")) options.getBoolean("cut") else null
        val beep = if (options.hasKey("beep")) options.getBoolean("beep") else null
        val result = Arguments.createMap()
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(android.content.Context.USB_SERVICE) as android.hardware.usb.UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            val commands = mutableListOf<Byte>()
            // Alinhamento
            when (align) {
                "center" -> commands.addAll(listOf(0x1B, 0x61, 0x01).map { it.toByte() })
                "right" -> commands.addAll(listOf(0x1B, 0x61, 0x02).map { it.toByte() })
                else -> commands.addAll(listOf(0x1B, 0x61, 0x00).map { it.toByte() })
            }
            // Fonte
            when (font?.uppercase()) {
                "B" -> commands.addAll(listOf(0x1B, 0x4D, 0x01).map { it.toByte() })
                "C" -> commands.addAll(listOf(0x1B, 0x4D, 0x02).map { it.toByte() })
                else -> commands.addAll(listOf(0x1B, 0x4D, 0x00).map { it.toByte() })
            }
            // Tamanho
            when (size) {
                2 -> commands.addAll(listOf(0x1B, 0x21, 0x30).map { it.toByte() }) // 2x
                4 -> commands.addAll(listOf(0x1B, 0x21, 0x77).map { it.toByte() }) // 4x (max)
                else -> commands.addAll(listOf(0x1B, 0x21, 0x00).map { it.toByte() }) // normal
            }
            // Negrito
            if (bold == true) commands.addAll(listOf(0x1B, 0x45, 0x01).map { it.toByte() })
            else if (bold == false) commands.addAll(listOf(0x1B, 0x45, 0x00).map { it.toByte() })
            // Texto
            val textBytes = if (encoding != null) text.toByteArray(Charsets.UTF_8) else text.toByteArray()
            commands.addAll(textBytes.toList())
            commands.add(0x0A)
            // Beep
            if (beep == true) commands.addAll(listOf(0x1B, 0x42, 0x03, 0x01).map { it.toByte() })
            // Corte
            if (cut == true) commands.addAll(listOf(0x1D, 0x56, 0x00).map { it.toByte() })
            connection.bulkTransfer(endpoint, commands.toByteArray(), commands.size, 4000)
            result.putBoolean("success", true)
            result.putString("message", "Texto impresso com sucesso.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir texto: ${e.localizedMessage}")
        } finally {
            try { connection?.releaseInterface(device.getInterface(0)) } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
        return result
    }
}
