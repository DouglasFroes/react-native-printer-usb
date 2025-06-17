package com.usbprinter

import android.hardware.usb.UsbDevice
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterTextHelper {
    private const val TAG = "UsbPrinterTextHelper"

    fun printText(context: Context, options: ReadableMap, device: UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val size = if (options.hasKey("size")) options.getInt("size") else null
        val align = if (options.hasKey("align")) options.getString("align") else null
        val encoding = if (options.hasKey("encoding")) options.getString("encoding") else null
        val bold = if (options.hasKey("bold")) options.getBoolean("bold") else null
        val font = if (options.hasKey("font")) options.getString("font") else null
        val cut = if (options.hasKey("cut")) options.getBoolean("cut") else null
        val beep = if (options.hasKey("beep")) options.getBoolean("beep") else null
        val underline = if (options.hasKey("underline")) options.getBoolean("underline") else null
        val tailingLine = if (options.hasKey("tailingLine")) options.getBoolean("tailingLine") else null

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora")
        }

        try {
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
            // Sublinhado
            if (underline == true) commands.addAll(listOf(0x1B, 0x2D, 0x01).map { it.toByte() })
            else if (underline == false) commands.addAll(listOf(0x1B, 0x2D, 0x00).map { it.toByte() })            // Texto com codificação adequada
            val textBytes = when (encoding?.lowercase()) {
                "cp850" -> text.toByteArray(Charsets.ISO_8859_1) // Aproximação
                "iso-8859-1" -> text.toByteArray(Charsets.ISO_8859_1)
                "utf8", "utf-8" -> text.toByteArray(Charsets.UTF_8)
                else -> text.toByteArray(Charsets.UTF_8)
            }
            commands.addAll(textBytes.toList())
            commands.add(0x0A) // Line feed

            // Tailing line
            if (tailingLine == true) commands.addAll(listOf(0x0A, 0x0A, 0x0A).map { it.toByte() })
            // Beep
            if (beep == true) commands.addAll(listOf(0x1B, 0x42, 0x03, 0x01).map { it.toByte() })
            // Corte
            if (cut == true) commands.addAll(listOf(0x1D, 0x56, 0x00).map { it.toByte() })

            Log.d(TAG, "Sending ${commands.size} bytes to thermal printer")
            val success = UsbConnectionHelper.sendDataInChunks(connectionData.connection, connectionData.endpoint, commands.toByteArray())

            if (success) {
                return UsbConnectionHelper.createSuccessResponse("Texto impresso com sucesso")
            } else {
                return UsbConnectionHelper.createErrorResponse("Falha ao enviar dados para a impressora")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error printing text", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao imprimir: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
