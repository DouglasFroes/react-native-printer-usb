package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterQrCodeHelper {
    private const val TAG = "UsbPrinterQrCodeHelper"

    fun printQrCode(context: Context, options: ReadableMap, device: UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val size = if (options.hasKey("size")) options.getDouble("size") else 6.0
        val align = if (options.hasKey("align")) options.getString("align") else null

        Log.d(TAG, "Iniciando impressão de QR Code: '$text' (tamanho: $size, alinhamento: $align)")

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora para QR Code")
        }

        try {
            // Inicializa a impressora
            if (!UsbConnectionHelper.initializePrinter(connectionData.connection, connectionData.endpoint)) {
                return UsbConnectionHelper.createErrorResponse("Falha na inicialização da impressora para QR Code")
            }

            val commands = mutableListOf<Byte>()

            // Alinhamento
            when (align) {
                "center" -> {
                    Log.d(TAG, "Configurando alinhamento central para QR Code")
                    commands.addAll(listOf(0x1B, 0x61, 0x01).map { it.toByte() })
                }
                "right" -> {
                    Log.d(TAG, "Configurando alinhamento à direita para QR Code")
                    commands.addAll(listOf(0x1B, 0x61, 0x02).map { it.toByte() })
                }
                else -> {
                    Log.d(TAG, "Configurando alinhamento à esquerda para QR Code")
                    commands.addAll(listOf(0x1B, 0x61, 0x00).map { it.toByte() })
                }
            }

            // Configuração do QR Code
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val storeLen = textBytes.size + 3
            val pL = (storeLen % 256).toByte()
            val pH = (storeLen / 256).toByte()

            Log.d(TAG, "Configurando modelo do QR Code")
            // Seleciona modelo de QR Code (modelo 2)
            commands.addAll(listOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00).map { it.toByte() })

            Log.d(TAG, "Configurando tamanho do QR Code: ${size.toInt()}")
            // Define tamanho do módulo
            commands.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toInt().toByte()).map { it.toByte() })

            Log.d(TAG, "Configurando nível de correção de erro")
            // Nível de correção de erro (L=48, M=49, Q=50, H=51)
            commands.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31).map { it.toByte() })

            Log.d(TAG, "Armazenando dados do QR Code (${textBytes.size} bytes)")
            // Armazena dados no símbolo
            commands.addAll(listOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30).map { it.toByte() })
            commands.addAll(textBytes.toList())

            Log.d(TAG, "Comandando impressão do QR Code")
            // Imprime o símbolo
            commands.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30).map { it.toByte() })

            // Restaura alinhamento à esquerda
            commands.addAll(listOf(0x1B, 0x61, 0x00).map { it.toByte() })

            // Alimenta papel após o QR Code
            commands.addAll(listOf(0x0A, 0x0A).map { it.toByte() })

            Log.i(TAG, "Enviando ${commands.size} bytes para impressão de QR Code")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                commands.toByteArray()
            )

            return if (success) {
                UsbConnectionHelper.createSuccessResponse("QR Code impresso com sucesso")
            } else {
                UsbConnectionHelper.createErrorResponse("Falha ao enviar dados do QR Code")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao imprimir QR Code: ${e.message}", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao imprimir QR Code: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
