package com.usbprinter

import android.graphics.Bitmap
import android.graphics.Color

object UtilsImage {
    fun getBitmapResized(image: Bitmap, decreaseSizeBy: Float, imageWidth: Int, imageHeight: Int): Bitmap {
        var imageWidthForResize = image.width
        var imageHeightForResize = image.height
        if (imageWidth > 0) {
            imageWidthForResize = imageWidth
        }
        if (imageHeight > 0) {
            imageHeightForResize = imageHeight
        }
        return Bitmap.createScaledBitmap(
            image,
            (imageWidthForResize * decreaseSizeBy).toInt(),
            (imageHeightForResize * decreaseSizeBy).toInt(),
            true
        )
    }

    fun getRGB(bmpOriginal: Bitmap, col: Int, row: Int): Int {
        val pixel = bmpOriginal.getPixel(col, row)
        val R = Color.red(pixel)
        val G = Color.green(pixel)
        val B = Color.blue(pixel)
        return Color.rgb(R, G, B)
    }

    fun resizeTheImageForPrinting(image: Bitmap, imageWidth: Int, imageHeight: Int): Bitmap {
        val width = image.width
        val height = image.height
        if (imageWidth > 0 || imageHeight > 0) {
            return getBitmapResized(image, 1f, imageWidth, imageHeight)
        }
        if (width > 200 || height > 200) {
            val decreaseSizeBy = if (width > height) {
                200.0f / width
            } else {
                200.0f / height
            }
            return getBitmapResized(image, decreaseSizeBy, 0, 0)
        }
        return image
    }

    fun shouldPrintColor(col: Int): Boolean {
        val threshold = 127
        val a = (col shr 24) and 0xff
        if (a != 0xff) return false // Ignore transparencies
        val r = (col shr 16) and 0xff
        val g = (col shr 8) and 0xff
        val b = col and 0xff
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        return luminance < threshold
    }

    fun recollectSlice(y: Int, x: Int, img: Array<IntArray>): ByteArray {
        val slices = ByteArray(3) { 0 }
        for ((i, yy) in (y until y + 24 step 8).withIndex()) {
            var slice: Byte = 0
            for (b in 0 until 8) {
                val yyy = yy + b
                if (yyy >= img.size) continue
                val col = img[yyy][x]
                val v = shouldPrintColor(col)
                slice = (slice.toInt() or ((if (v) 1 else 0) shl (7 - b))).toByte()
            }
            slices[i] = slice
        }
        return slices
    }

    fun getPixelsSlow(image2: Bitmap, imageWidth: Int, imageHeight: Int): Array<IntArray> {
        val image = resizeTheImageForPrinting(image2, imageWidth, imageHeight)
        val width = image.width
        val height = image.height
        val result = Array(height) { IntArray(width) }
        for (row in 0 until height) {
            for (col in 0 until width) {
                result[row][col] = getRGB(image, col, row)
            }
        }
        return result
    }
}
