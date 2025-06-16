package com.usbprinter

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Arguments
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
        val result = Arguments.createMap()

        val connectionData = establishPrinterConnection(context, device)
        if (connectionData == null) {
            result.putBoolean("success", false)
            result.putString("message", "Falha ao conectar com a impressora G250")
            return result
        }

        try {
            // Comando de inicialização ESC/POS para G250
            val initCommands = mutableListOf<Byte>()
            initCommands.addAll(listOf(0x1B, 0x40).map { it.toByte() }) // ESC @ - Reset
            sendDataInChunks(connectionData.connection, connectionData.endpoint, initCommands.toByteArray())
            Thread.sleep(100) // Aguarda reset

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
            else if (underline == false) commands.addAll(listOf(0x1B, 0x2D, 0x00).map { it.toByte() })

            // Texto com codificação adequada para G250
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

            Log.d(TAG, "Sending ${commands.size} bytes to G250 printer")
            val success = sendDataInChunks(connectionData.connection, connectionData.endpoint, commands.toByteArray())

            if (success) {
                result.putBoolean("success", true)
                result.putString("message", "Texto impresso com sucesso na G250.")
            } else {
                result.putBoolean("success", false)
                result.putString("message", "Falha ao enviar dados para a G250")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error printing text to G250", e)
            result.putBoolean("success", false)
            result.putString("message", "Erro ao imprimir na G250: ${e.localizedMessage}")
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
                Log.e(TAG, "Failed to open USB device")
                return null
            }

            // Tenta várias interfaces para G250
            for (interfaceIndex in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(interfaceIndex)
                Log.d(TAG, "Trying interface $interfaceIndex with ${usbInterface.endpointCount} endpoints")

                // Procura endpoint OUT para impressão
                for (endpointIndex in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(endpointIndex)
                    if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                        Log.d(TAG, "Found OUT endpoint at interface $interfaceIndex")

                        if (connection.claimInterface(usbInterface, true)) {
                            Log.d(TAG, "Successfully claimed interface for G250")
                            return ConnectionData(connection, endpoint, usbInterface)
                        } else {
                            Log.w(TAG, "Failed to claim interface $interfaceIndex")
                        }
                    }
                }
            }

            Log.e(TAG, "No suitable interface found for G250")
            connection.close()
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error establishing connection to G250", e)
            return null
        }
    }

    private fun sendDataInChunks(connection: UsbDeviceConnection, endpoint: UsbEndpoint, data: ByteArray): Boolean {
        try {
            val chunkSize = 64 // Tamanho pequeno para G250
            var offset = 0

            while (offset < data.size) {
                val remainingBytes = data.size - offset
                val currentChunkSize = minOf(chunkSize, remainingBytes)
                val chunk = data.copyOfRange(offset, offset + currentChunkSize)

                val bytesTransferred = connection.bulkTransfer(endpoint, chunk, chunk.size, 5000)
                if (bytesTransferred < 0) {
                    Log.e(TAG, "Failed to transfer chunk to G250 at offset $offset")
                    return false
                }

                offset += currentChunkSize

                // Delay entre chunks para G250
                if (offset < data.size) {
                    Thread.sleep(10)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data chunks to G250", e)
            return false
        }
    }

    private fun closeConnection(connectionData: ConnectionData?) {
        connectionData?.let {
            try {
                it.connection.releaseInterface(it.usbInterface)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing interface", e)
            }

            try {
                it.connection.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing connection", e)
            }
        }
    }
}
