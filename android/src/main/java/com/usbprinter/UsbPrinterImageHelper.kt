package com.usbprinter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import android.net.Uri
import android.content.ContentResolver
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64

object UsbPrinterImageHelper {
    /**
     * Imprime uma imagem a partir de um base64 PNG/JPG.
     */
    fun printImageBase64(context: Context, options: com.facebook.react.bridge.ReadableMap, device: UsbDevice): WritableMap {
        return try {
            val base64Image = options.getString("base64Image") ?: ""
            val align = if (options.hasKey("align")) options.getString("align") else null
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            printBitmap(context, bitmap, device, align)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao decodificar imagem base64: ${e.localizedMessage}")
            result
        }
    }

    /**
     * Imprime uma imagem a partir de uma URI (content:// ou file://).
     */
    fun printImageUri(context: Context, options: com.facebook.react.bridge.ReadableMap, device: UsbDevice): WritableMap {
        return try {
            val imageUri = options.getString("imageUri") ?: ""
            val align = if (options.hasKey("align")) options.getString("align") else null
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            printBitmap(context, bitmap, device, align)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao carregar imagem da URI: ${e.localizedMessage}")
            result
        }
    }

    /**
     * Converte o Bitmap para comandos ESC/POS e envia para a impressora.
     */
    fun printBitmap(context: Context, bitmap: Bitmap?, device: android.hardware.usb.UsbDevice, align: String? = null): WritableMap {
        val result = Arguments.createMap()
        if (bitmap == null) {
            result.putBoolean("success", false)
            result.putString("message", "Bitmap inválido ou nulo.")
            return result
        }
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
            // Alinhamento opcional
            when (align) {
                "center" -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x01), 3, 2000)
                "right" -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x02), 3, 2000)
                else -> connection.bulkTransfer(endpoint, byteArrayOf(0x1B, 0x61, 0x00), 3, 2000)
            }
            val escpos = bitmapToEscPos(bitmap)
            connection.bulkTransfer(endpoint, escpos, escpos.size, 4000)
            result.putBoolean("success", true)
            result.putString("message", "Imagem impressa com sucesso.")
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir imagem: ${e.localizedMessage}")
        } finally {
            try { connection?.releaseInterface(device.getInterface(0)) } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
        return result
    }

    /**
     * Converte um Bitmap para comandos ESC/POS (modo gráfico simples, 8-dot single density).
     * Suporta apenas preto e branco (threshold simples).
     */
    private fun bitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerLine = (width + 7) / 8
        val escpos = mutableListOf<Byte>()
        for (y in 0 until height) {
            escpos.add(0x1B.toByte()) // ESC
            escpos.add(0x2A.toByte()) // *
            escpos.add(0x21.toByte()) // m=33 (8-dot single density)
            escpos.add((width and 0xFF).toByte()) // nL
            escpos.add(((width shr 8) and 0xFF).toByte()) // nH
            for (x in 0 until bytesPerLine * 8 step 8) {
                var b = 0
                for (bit in 0..7) {
                    val px = if (x + bit < width) bitmap.getPixel(x + bit, y) else 0xFFFFFF
                    val gray = (Color.red(px) + Color.green(px) + Color.blue(px)) / 3
                    if (gray < 128) b = b or (1 shl (7 - bit))
                }
                escpos.add(b.toByte())
            }
            escpos.add(0x0A.toByte()) // LF
        }
        return escpos.toByteArray()
    }
}
