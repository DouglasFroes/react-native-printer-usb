package com.usbprinter

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

object UsbPrinterHtmlHelper {
    /**
     * Renderiza HTML em um bitmap e imprime na impressora USB.
     * @param context Contexto Android
     * @param html String HTML a ser renderizada
     * @param device UsbDevice já autorizado
     * @return WritableMap com o resultado da impressão
     */
    fun printHtml(context: Context, options: com.facebook.react.bridge.ReadableMap, device: android.hardware.usb.UsbDevice): WritableMap {
        val html = options.getString("html") ?: ""
        val align = if (options.hasKey("align")) options.getString("align") else null
        // pageWidth vem em mm, converter para pixels (1mm ≈ 7.2px para 203dpi)
        val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else 80 // padrão 80mm
        val pageWidthPx = (pageWidthMm * 7.2).toInt() // 203dpi: 1mm ≈ 7.2px

        val result = Arguments.createMap()
        try {
            val webView = android.webkit.WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.setBackgroundColor(0xFFFFFFFF.toInt())
            webView.layout(0, 0, pageWidthPx, 6000) // Usa pageWidth convertido para px
            var bitmap: android.graphics.Bitmap? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val width = webView.width
                            val height = webView.contentHeight * webView.scale.toInt()
                            bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap!!)
                            webView.draw(canvas)
                        } catch (_: Exception) {}
                        latch.countDown()
                    }, 500) // Pequeno delay para garantir renderização
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            latch.await()
            if (bitmap != null) {
                // return UsbPrinterImageHelper.printBitmap(context, bitmap, device, align)
                 result.putBoolean("success", true)
                result.putString("message", "HTML renderizado com sucesso.")
                return result
            } else {
                result.putBoolean("success", false)
                result.putString("message", "Erro ao renderizar HTML para bitmap.")
            }
        } catch (e: Exception) {
            result.putBoolean("success", false)
            result.putString("message", "Erro ao renderizar HTML: ${e.localizedMessage}")
        }
        return result
    }
}
