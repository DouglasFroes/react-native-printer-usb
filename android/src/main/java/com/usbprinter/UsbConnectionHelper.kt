package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

object UsbConnectionHelper {
    private const val TAG = "UsbConnectionHelper"

    data class ConnectionData(
        val connection: UsbDeviceConnection,
        val endpoint: UsbEndpoint,
        val usbInterface: UsbInterface
    )

    /**
     * Estabelece conexão robusta com impressora térmica USB.
     * Tenta todas as interfaces e endpoints OUT disponíveis.
     */
    fun establishPrinterConnection(context: Context, device: UsbDevice): ConnectionData? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        try {
            val connection = usbManager.openDevice(device) ?: run {
                Log.e(TAG, "Failed to open USB device for thermal printer")
                return null
            }

            Log.d(TAG, "Attempting to connect to thermal printer with ${device.interfaceCount} interfaces")

            // Tenta várias interfaces para máxima compatibilidade
            for (interfaceIndex in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(interfaceIndex)
                Log.d(TAG, "Trying interface $interfaceIndex with ${usbInterface.endpointCount} endpoints")

                // Procura endpoint OUT para impressão
                for (endpointIndex in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(endpointIndex)
                    if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                        Log.d(TAG, "Found OUT endpoint at interface $interfaceIndex")

                        if (connection.claimInterface(usbInterface, true)) {
                            Log.i(TAG, "Successfully claimed interface for thermal printer")

                            // Envia comando de inicialização ESC @
                            initializePrinter(connection, endpoint)

                            return ConnectionData(connection, endpoint, usbInterface)
                        } else {
                            Log.w(TAG, "Failed to claim interface $interfaceIndex")
                        }
                    }
                }
            }

            Log.e(TAG, "No suitable interface found for thermal printer")
            connection.close()
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error establishing connection to thermal printer", e)
            return null
        }
    }

    /**
     * Inicializa a impressora com comando ESC @ (reset).
     */
    fun initializePrinter(connection: UsbDeviceConnection, endpoint: UsbEndpoint): Boolean {
        try {
            Log.d(TAG, "Initializing thermal printer with ESC @")
            val initCommand = byteArrayOf(0x1B, 0x40) // ESC @ - Reset completo
            val bytesTransferred = connection.bulkTransfer(endpoint, initCommand, initCommand.size, 3000)
            if (bytesTransferred >= 0) {
                Thread.sleep(100) // Aguarda processamento do reset
                Log.d(TAG, "Thermal printer initialized successfully")
                return true
            } else {
                Log.w(TAG, "Failed to send initialization command to thermal printer")
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing thermal printer", e)
            return false
        }
    }

    /**
     * Envia dados em chunks pequenos para máxima compatibilidade.
     */
    fun sendDataInChunks(connection: UsbDeviceConnection, endpoint: UsbEndpoint, data: ByteArray): Boolean {
        try {
            val chunkSize = 64 // Tamanho pequeno para máxima compatibilidade
            var offset = 0

            Log.d(TAG, "Sending ${data.size} bytes to thermal printer in chunks of $chunkSize")

            while (offset < data.size) {
                val remainingBytes = data.size - offset
                val currentChunkSize = minOf(chunkSize, remainingBytes)
                val chunk = data.copyOfRange(offset, offset + currentChunkSize)

                val bytesTransferred = connection.bulkTransfer(endpoint, chunk, chunk.size, 5000)
                if (bytesTransferred < 0) {
                    Log.e(TAG, "Failed to transfer chunk at offset $offset to thermal printer")
                    return false
                }

                Log.v(TAG, "Sent chunk of $currentChunkSize bytes (offset: $offset)")
                offset += currentChunkSize

                // Delay entre chunks para impressoras térmicas
                if (offset < data.size) {
                    Thread.sleep(10)
                }
            }

            Log.d(TAG, "Successfully sent all data chunks to thermal printer")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data chunks to thermal printer", e)
            return false
        }
    }

    /**
     * Fecha a conexão de forma segura.
     */
    fun closeConnection(connectionData: ConnectionData?) {
        connectionData?.let {
            try {
                Log.d(TAG, "Releasing interface for thermal printer")
                it.connection.releaseInterface(it.usbInterface)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing interface for thermal printer", e)
            }

            try {
                Log.d(TAG, "Closing connection to thermal printer")
                it.connection.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing connection to thermal printer", e)
            }
        }
    }

    /**
     * Cria resposta de sucesso padronizada.
     */
    fun createSuccessResponse(message: String): WritableMap {
        val result = Arguments.createMap()
        result.putBoolean("success", true)
        result.putString("message", message)
        return result
    }

    /**
     * Cria resposta de erro padronizada.
     */
    fun createErrorResponse(message: String): WritableMap {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("message", message)
        return result
    }
}
