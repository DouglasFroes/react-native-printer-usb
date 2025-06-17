package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

object UsbPrinterBarcodeHelper {
    private const val TAG = "UsbPrinterBarcodeHelper"

    fun printBarcode(context: Context, options: ReadableMap, device: UsbDevice): WritableMap {
        val text = options.getString("text") ?: ""
        val width = if (options.hasKey("width")) options.getDouble("width") else 2.0
        val height = if (options.hasKey("height")) options.getDouble("height") else 100.0

        Log.d(TAG, "Iniciando impressão de código de barras: '$text' (largura: $width, altura: $height)")

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora para código de barras")
        }

        try {
            // Inicializa a impressora
            if (!UsbConnectionHelper.initializePrinter(connectionData.connection, connectionData.endpoint)) {
                return UsbConnectionHelper.createErrorResponse("Falha na inicialização da impressora para código de barras")
            }

            val commands = mutableListOf<Byte>()

            // Configurações do código de barras
            Log.d(TAG, "Configurando altura do código de barras: ${height.toInt()}")
            commands.addAll(listOf(0x1D, 0x68, height.toInt().toByte()).map { it.toByte() }) // GS h (height)

            Log.d(TAG, "Configurando largura do código de barras: ${width.toInt()}")
            commands.addAll(listOf(0x1D, 0x77, width.toInt().toByte()).map { it.toByte() }) // GS w (width)

            // Posição do texto HRI (Human Readable Interpretation)
            Log.d(TAG, "Configurando posição do texto HRI")
            commands.addAll(listOf(0x1D, 0x48, 0x02).map { it.toByte() }) // GS H (HRI position: below)

            // Fonte do texto HRI
            Log.d(TAG, "Configurando fonte do texto HRI")
            commands.addAll(listOf(0x1D, 0x66, 0x00).map { it.toByte() }) // GS f (HRI font: A)

            // Comando para imprimir código de barras CODE128
            Log.d(TAG, "Iniciando impressão CODE128 com ${text.length} caracteres")
            commands.addAll(listOf(0x1D, 0x6B, 0x49, text.length.toByte()).map { it.toByte() }) // GS k (CODE128)

            // Dados do código de barras
            val textBytes = text.toByteArray(Charsets.UTF_8)
            commands.addAll(textBytes.toList())

            // Alimenta papel após o código de barras
            commands.addAll(listOf(0x0A, 0x0A).map { it.toByte() })

            Log.i(TAG, "Enviando ${commands.size} bytes para impressão de código de barras")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                commands.toByteArray()
            )

            return if (success) {
                UsbConnectionHelper.createSuccessResponse("Código de barras impresso com sucesso")
            } else {
                UsbConnectionHelper.createErrorResponse("Falha ao enviar dados do código de barras")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao imprimir código de barras: ${e.message}", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao imprimir código de barras: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
