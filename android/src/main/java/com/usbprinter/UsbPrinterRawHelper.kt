package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.WritableMap
import android.util.Base64

/**
 * Envia dados brutos (comando ESC/POS em base64) diretamente para a impressora USB.
 * @param context Contexto Android
 * @param base64Data Dados ESC/POS codificados em base64
 * @param device UsbDevice já autorizado
 */
object UsbPrinterRawHelper {
    private const val TAG = "UsbPrinterRawHelper"

    fun sendRawData(context: Context, base64Data: String, device: UsbDevice): WritableMap {
        Log.d(TAG, "Iniciando envio de dados RAW (${base64Data.length} caracteres base64)")

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora para dados RAW")
        }

        try {
            // Decodifica os dados base64
            val rawBytes = try {
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Erro ao decodificar dados base64: ${e.message}")
                return UsbConnectionHelper.createErrorResponse("Dados base64 inválidos")
            }

            Log.i(TAG, "Decodificados ${rawBytes.size} bytes de dados RAW")

            // Inicializa a impressora
            if (!UsbConnectionHelper.initializePrinter(connectionData.connection, connectionData.endpoint)) {
                return UsbConnectionHelper.createErrorResponse("Falha na inicialização da impressora para dados RAW")
            }

            // Envia os dados RAW diretamente
            Log.i(TAG, "Enviando ${rawBytes.size} bytes de dados RAW para impressora")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                rawBytes
            )

            return if (success) {
                UsbConnectionHelper.createSuccessResponse("Dados RAW enviados com sucesso")
            } else {
                UsbConnectionHelper.createErrorResponse("Falha ao enviar dados RAW")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar dados RAW: ${e.message}", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao enviar dados RAW: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
