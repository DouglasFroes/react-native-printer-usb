package com.usbprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.facebook.react.bridge.WritableMap

object UsbPrinterResetHelper {
    private const val TAG = "UsbPrinterResetHelper"

    fun reset(context: Context, device: UsbDevice): WritableMap {
        Log.d(TAG, "Iniciando operação de reset")

        val connectionData = UsbConnectionHelper.establishPrinterConnection(context, device)
        if (connectionData == null) {
            return UsbConnectionHelper.createErrorResponse("Falha ao conectar com a impressora para reset")
        }

        try {
            // Comandos de reset e limpeza
            val commands = mutableListOf<Byte>()

            Log.d(TAG, "Enviando comando ESC @ (reset)")
            // Comando de reset ESC/POS principal
            commands.addAll(listOf(0x1B, 0x40).map { it.toByte() }) // ESC @ - Reset completo

            Log.d(TAG, "Enviando comando CAN (cancelar dados pendentes)")
            // Cancela dados pendentes no buffer
            commands.add(0x18) // CAN - cancela dados pendentes

            Log.d(TAG, "Enviando comandos de inicialização padrão")
            // Comandos de inicialização adicional
            commands.addAll(listOf(0x1B, 0x61, 0x00).map { it.toByte() }) // ESC a - alinhamento à esquerda
            commands.addAll(listOf(0x1B, 0x21, 0x00).map { it.toByte() }) // ESC ! - fonte padrão
            commands.addAll(listOf(0x1B, 0x45, 0x00).map { it.toByte() }) // ESC E - desabilita negrito
            commands.addAll(listOf(0x1B, 0x2D, 0x00).map { it.toByte() }) // ESC - - desabilita sublinhado
            commands.addAll(listOf(0x1B, 0x4D, 0x00).map { it.toByte() }) // ESC M - fonte A

            Log.i(TAG, "Enviando ${commands.size} bytes para reset completo")
            val success = UsbConnectionHelper.sendDataInChunks(
                connectionData.connection,
                connectionData.endpoint,
                commands.toByteArray()
            )

            if (success) {
                // Aguarda processamento do reset
                Thread.sleep(300)
                Log.i(TAG, "Reset executado com sucesso")
                return UsbConnectionHelper.createSuccessResponse("Reset executado com sucesso - impressora reinicializada")
            } else {
                return UsbConnectionHelper.createErrorResponse("Falha ao enviar comando de reset")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao executar reset: ${e.message}", e)
            return UsbConnectionHelper.createErrorResponse("Erro ao executar reset: ${e.localizedMessage}")
        } finally {
            UsbConnectionHelper.closeConnection(connectionData)
        }
    }
}
