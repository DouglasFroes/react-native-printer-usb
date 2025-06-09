package com.usbprinter

import android.content.Context
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.Arguments
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream

object UsbPrinterHtmlHelper {
    /**
     * Renderiza HTML em um bitmap e imprime na impressora USB.
     * @param context Contexto Android
     * @param html String HTML a ser renderizada
     * @param promise Promise para retorno do resultado
     * @param device UsbDevice já autorizado
     */
    fun printHtml(context: Context, html: String, promise: Promise, device: android.hardware.usb.UsbDevice) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(0xFFFFFFFF.toInt())
        webView.layout(0, 0, 384, 6000) // Largura típica de impressora térmica 58mm (384px)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val width = webView.width
                        val height = webView.contentHeight * webView.scale.toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        webView.draw(canvas)
                        UsbPrinterImageHelper.printImageBase64(
                            context,
                            bitmapToBase64(bitmap),
                            promise,
                            device
                        )
                    } catch (e: Exception) {
                        val result = Arguments.createMap()
                        result.putBoolean("success", false)
                        result.putString("message", "Erro ao renderizar HTML: ${e.localizedMessage}")
                        promise.resolve(result)
                    }
                }, 500) // Pequeno delay para garantir renderização
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
    }
}
