package com.usbprinter

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

object UsbPrinterHtmlHelper {
    private const val TAG = "UsbPrinterHtmlHelper"
    /**
     * Renderiza HTML em um bitmap e imprime na impressora USB.
     * @param context Contexto Android
     * @param html String HTML a ser renderizada
     * @param device UsbDevice já autorizado
     * @return WritableMap com o resultado da impressão
     */
    fun printHtml(context: Context, options: com.facebook.react.bridge.ReadableMap, device: android.hardware.usb.UsbDevice): WritableMap {
         val result = Arguments.createMap()

        val html = options.getString("html") ?: ""
        val align = if (options.hasKey("align")) options.getString("align") else null
        // pageWidth vem em mm, converter para pixels (1mm ≈ 7.2px para 203dpi)
        val pageWidthMm = if (options.hasKey("pageWidth")) options.getInt("pageWidth") else 80 // padrão 80mm
        val pageWidthPx = (pageWidthMm * 7.2).toInt() // 203dpi: 1mm ≈ 7.2px

        val htmlHeight = if (options.hasKey("htmlHeight")) options.getDouble("htmlHeight") else null // altura fixa em pixels, se fornecida
        val pageHeightPx = htmlHeight?.toInt() ?: 200 // valor padrão se não informado

        val latch = java.util.concurrent.CountDownLatch(1)
        val bitmapHolder = arrayOfNulls<Bitmap>(1)

        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.setBackgroundColor(0xFFFFFFFF.toInt())
                webView.layout(0, 0, pageWidthPx, pageHeightPx) // Usa pageWidth convertido para px e altura definida
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val width = webView.width
                                val bitmap = Bitmap.createBitmap(width, pageHeightPx, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                webView.draw(canvas)
                                bitmapHolder[0] = bitmap
                            } catch (_: Exception) {}
                            latch.countDown()
                        }, 500) // Pequeno delay para garantir renderização
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                latch.countDown()
            }
        }

        latch.await()

        val bitmap = bitmapHolder[0]
        if (bitmap != null) {
            Log.d(TAG, "HTML rendered successfully, printing")
            return UsbPrinterImageHelper.printBitmap(context, bitmap, device, align)
        } else {
            Log.e(TAG, "Failed to render HTML to bitmap")
            result.putBoolean("success", false)
            result.putString("message", "Erro ao renderizar HTML para bitmap.")
            return result
        }
    }
}
