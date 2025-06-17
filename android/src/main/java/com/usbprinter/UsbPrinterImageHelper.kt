package com.usbprinter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
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
    private const val TAG = "UsbPrinterImageHelper"
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

    /**     * Converte o Bitmap para comandos ESC/POS e envia para a impressora.
     */
    fun printBitmap(context: Context, bitmap: Bitmap?, device: UsbDevice, align: String? = null): WritableMap {
        val result = Arguments.createMap()
        if (bitmap == null) {
            result.putBoolean("success", false)
            result.putString("message", "Bitmap inválido ou nulo.")
            return result
        }

        val connectionData = establishPrinterConnection(context, device)
        if (connectionData == null) {
            result.putBoolean("success", false)
            result.putString("message", "Falha ao conectar com a impressora para imagem")
            return result
        }

        try {
            // Comando de inicialização ESC/POS
            val initCommands = mutableListOf<Byte>()
            initCommands.addAll(listOf(0x1B, 0x40).map { it.toByte() }) // ESC @ - Reset
            sendDataInChunks(connectionData.connection, connectionData.endpoint, initCommands.toByteArray())
            Thread.sleep(100) // Aguarda reset

            // Configuração de alinhamento
            val alignCommands = when (align) {
                "center" -> byteArrayOf(0x1B, 0x61, 0x01)
                "right" -> byteArrayOf(0x1B, 0x61, 0x02)
                else -> byteArrayOf(0x1B, 0x61, 0x00) // left
            }
            sendDataInChunks(connectionData.connection, connectionData.endpoint, alignCommands)

            // Configuração de espaçamento de linha
            val setLineSpace24 = byteArrayOf(0x1B, 0x33, 24)
            sendDataInChunks(connectionData.connection, connectionData.endpoint, setLineSpace24)

            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            val pixels = UtilsImage.getPixelsSlow(bitmap, imageWidth, imageHeight)

            Log.d(TAG, "Printing image ${imageWidth}x${imageHeight}")

            // Para cada fatia vertical de 24 linhas
            for (y in 0 until imageHeight step 24) {
                // Comando ESC * para modo bit image
                val nL = (imageWidth and 0xFF).toByte()
                val nH = ((imageWidth shr 8) and 0xFF).toByte()
                val selectBitImageMode = byteArrayOf(0x1B, 0x2A, 33, nL, nH)
                sendDataInChunks(connectionData.connection, connectionData.endpoint, selectBitImageMode)

                // Para cada coluna da imagem - enviar em chunks menores
                val sliceData = mutableListOf<Byte>()
                for (x in 0 until imageWidth) {
                    val slice = UtilsImage.recollectSlice(y, x, pixels)
                    sliceData.addAll(slice.toList())

                    // Envia em chunks de 100 bytes
                    if (sliceData.size >= 100) {
                        sendDataInChunks(connectionData.connection, connectionData.endpoint, sliceData.toByteArray())
                        sliceData.clear()
                    }
                }

                // Envia dados restantes
                if (sliceData.isNotEmpty()) {
                    sendDataInChunks(connectionData.connection, connectionData.endpoint, sliceData.toByteArray())
                }

                // Line feed após cada fatia
                val lineFeed = byteArrayOf(0x0A)
                sendDataInChunks(connectionData.connection, connectionData.endpoint, lineFeed)
            }

            // Restaura espaçamento normal
            val setLineSpace32 = byteArrayOf(0x1B, 0x33, 32)
            sendDataInChunks(connectionData.connection, connectionData.endpoint, setLineSpace32)
            val finalLineFeed = byteArrayOf(0x0A)
            sendDataInChunks(connectionData.connection, connectionData.endpoint, finalLineFeed)

            result.putBoolean("success", true)
            result.putString("message", "Imagem impressa com sucesso.")
        } catch (e: Exception) {
            Log.e(TAG, "Error printing image", e)
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir imagem: ${e.localizedMessage}")
        } finally {
            closeConnection(connectionData)
        }
        return result
    }

    private data class ConnectionData(
        val connection: UsbDeviceConnection,
        val endpoint: UsbEndpoint,
        val usbInterface: UsbInterface
    )

    private fun establishPrinterConnection(context: Context, device: UsbDevice): ConnectionData? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        try {
            val connection = usbManager.openDevice(device) ?: run {
                Log.e(TAG, "Failed to open USB device for image printing")
                return null
            }

            // Tenta várias interfaces para impressoras térmicas USB
            for (interfaceIndex in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(interfaceIndex)
                Log.d(TAG, "Trying interface $interfaceIndex for image printing")

                // Procura endpoint OUT para impressão
                for (endpointIndex in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(endpointIndex)
                    if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                        Log.d(TAG, "Found OUT endpoint for image printing")

                        if (connection.claimInterface(usbInterface, true)) {
                            Log.d(TAG, "Successfully claimed interface for thermal printer image printing")
                            return ConnectionData(connection, endpoint, usbInterface)
                        } else {
                            Log.w(TAG, "Failed to claim interface for image printing")
                        }
                    }
                }
            }

            Log.e(TAG, "No suitable interface found for thermal printer image printing")
            connection.close()
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error establishing connection for thermal printer image printing", e)
            return null
        }
    }

    private fun sendDataInChunks(connection: UsbDeviceConnection, endpoint: UsbEndpoint, data: ByteArray): Boolean {
        try {
            val chunkSize = 64 // Tamanho pequeno para máxima compatibilidade
            var offset = 0

            while (offset < data.size) {
                val remainingBytes = data.size - offset
                val currentChunkSize = minOf(chunkSize, remainingBytes)
                val chunk = data.copyOfRange(offset, offset + currentChunkSize)

                val bytesTransferred = connection.bulkTransfer(endpoint, chunk, chunk.size, 5000)
                if (bytesTransferred < 0) {
                    Log.e(TAG, "Failed to transfer image data chunk at offset $offset")
                    return false
                }

                offset += currentChunkSize

                // Delay entre chunks para impressoras térmicas
                if (offset < data.size) {
                    Thread.sleep(5) // Delay menor para imagens
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image data chunks to thermal printer", e)
            return false
        }
    }

    private fun closeConnection(connectionData: ConnectionData?) {
        connectionData?.let {
            try {
                it.connection.releaseInterface(it.usbInterface)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing interface for image printing", e)
            }

            try {
                it.connection.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing connection for image printing", e)
            }
        }
    }
}
