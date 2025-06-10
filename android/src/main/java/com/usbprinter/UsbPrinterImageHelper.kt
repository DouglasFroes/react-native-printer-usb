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
import java.net.URL
import java.io.File
import java.io.FileOutputStream
import com.usbprinter.UtilsImage

object UsbPrinterImageHelper {
    /**
     * Imprime uma imagem a partir de um base64 PNG/JPG.
     */
    fun printImageBase64(context: Context, options: com.facebook.react.bridge.ReadableMap, device: UsbDevice): WritableMap {
        return try {
            val base64Image = options.getString("base64Image") ?: ""
            val align = if (options.hasKey("align")) options.getString("align") else null
            // pageWidth vem em mm, converter para pixels (1mm ≈ 7.2px para 203dpi)
            val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else null
            val pageWidthPx = pageWidthMm?.let { (it * 7.2).toInt() }
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null && pageWidthPx != null && bitmap.width != pageWidthPx) {
                val aspect = bitmap.height.toFloat() / bitmap.width
                val newHeight = (pageWidthPx * aspect).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, pageWidthPx, newHeight, true)
            }
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
            // pageWidth vem em mm, converter para pixels (1mm ≈ 7.2px para 203dpi)
            val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else null
            val pageWidthPx = pageWidthMm?.let { (it * 7.2).toInt() }
            val uri = Uri.parse(imageUri)
            var inputStream = when {
                imageUri.startsWith("https://") || imageUri.startsWith("http://") -> {
                    // Baixa a imagem remota para arquivo temporário
                    val url = URL(imageUri)
                    val connection = url.openConnection()
                    connection.connect()
                    val input = connection.getInputStream()
                    val tempFile = File.createTempFile("usbprinter_img", ".tmp", context.cacheDir)
                    val output = FileOutputStream(tempFile)
                    input.copyTo(output)
                    output.close()
                    input.close()
                    context.contentResolver.openInputStream(Uri.fromFile(tempFile))
                }
                else -> context.contentResolver.openInputStream(uri)
            }
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null && pageWidthPx != null && bitmap.width != pageWidthPx) {
                val aspect = bitmap.height.toFloat() / bitmap.width
                val newHeight = (pageWidthPx * aspect).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, pageWidthPx, newHeight, true)
            }
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

            // Configuração de alinhamento
            val CENTER_ALIGN = byteArrayOf(0x1B, 0x61, 0x01)
            val LEFT_ALIGN = byteArrayOf(0x1B, 0x61, 0x00)
            if (align == "center") {
                connection.bulkTransfer(endpoint, CENTER_ALIGN, CENTER_ALIGN.size, 1000)
            } else {
                connection.bulkTransfer(endpoint, LEFT_ALIGN, LEFT_ALIGN.size, 1000)
            }

            // Configuração de espaçamento de linha
            val SET_LINE_SPACE_24 = byteArrayOf(0x1B, 0x33, 24)
            val SET_LINE_SPACE_32 = byteArrayOf(0x1B, 0x33, 32)
            val LINE_FEED = byteArrayOf(0x0A)
            connection.bulkTransfer(endpoint, SET_LINE_SPACE_24, SET_LINE_SPACE_24.size, 1000)

            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            val pixels = UtilsImage.getPixelsSlow(bitmap, imageWidth, imageHeight)

            // Para cada fatia vertical de 24 linhas
            for (y in 0 until imageHeight step 24) {
                // Comando ESC * para modo bit image
                val nL = (imageWidth and 0xFF).toByte()
                val nH = ((imageWidth shr 8) and 0xFF).toByte()
                val SELECT_BIT_IMAGE_MODE = byteArrayOf(0x1B, 0x2A, 33, nL, nH)
                connection.bulkTransfer(endpoint, SELECT_BIT_IMAGE_MODE, SELECT_BIT_IMAGE_MODE.size, 1000)

                // Para cada coluna da imagem
                for (x in 0 until imageWidth) {
                    val slice = UtilsImage.recollectSlice(y, x, pixels)
                    connection.bulkTransfer(endpoint, slice, slice.size, 1000)
                }
                connection.bulkTransfer(endpoint, LINE_FEED, LINE_FEED.size, 1000)
            }
            connection.bulkTransfer(endpoint, SET_LINE_SPACE_32, SET_LINE_SPACE_32.size, 1000)
            connection.bulkTransfer(endpoint, LINE_FEED, LINE_FEED.size, 1000)

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
}
