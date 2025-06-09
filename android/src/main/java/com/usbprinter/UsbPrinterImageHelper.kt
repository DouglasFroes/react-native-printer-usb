import android.net.Uri
import android.graphics.BitmapFactory
import android.content.Context
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.Arguments

object UsbPrinterImageHelper {
    /**
     * Imprime uma imagem a partir de um base64 PNG/JPG.
     */
    fun printImageBase64(context: Context, base64Image: String, promise: Promise, device: android.hardware.usb.UsbDevice) {
        try {
            val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            printBitmap(context, bitmap, promise, device)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao decodificar imagem base64: ${e.localizedMessage}")
            promise.resolve(result)
        }
    }

    /**
     * Imprime uma imagem a partir de uma URI (content:// ou file://).
     */
    fun printImageUri(context: Context, imageUri: String, promise: Promise, device: android.hardware.usb.UsbDevice) {
        try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            printBitmap(context, bitmap, promise, device)
        } catch (e: Exception) {
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("message", "Erro ao carregar imagem da URI: ${e.localizedMessage}")
            promise.resolve(result)
        }
    }

    /**
     * Converte o Bitmap para comandos ESC/POS e envia para a impressora.
     */
    private fun printBitmap(context: Context, bitmap: android.graphics.Bitmap?, promise: Promise, device: android.hardware.usb.UsbDevice) {
        val result = Arguments.createMap()
        if (bitmap == null) {
            result.putBoolean("success", false)
            result.putString("message", "Bitmap inválido ou nulo.")
            promise.resolve(result)
            return
        }
        var connection: android.hardware.usb.UsbDeviceConnection? = null
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
            connection = usbManager.openDevice(device)
            val usbInterface = device.getInterface(0)
            val endpoint = usbInterface.getEndpoint(0)
            connection.claimInterface(usbInterface, true)
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
            promise.resolve(result)
        }
    }

    /**
     * Converte um Bitmap para comandos ESC/POS (modo gráfico simples, 8-dot single density).
     * Suporta apenas preto e branco (threshold simples).
     */
    private fun bitmapToEscPos(bitmap: android.graphics.Bitmap): ByteArray {
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
                    val gray = (android.graphics.Color.red(px) + android.graphics.Color.green(px) + android.graphics.Color.blue(px)) / 3
                    if (gray < 128) b = b or (1 shl (7 - bit))
                }
                escpos.add(b.toByte())
            }
            escpos.add(0x0A.toByte()) // LF
        }
        return escpos.toByteArray()
    }
}
