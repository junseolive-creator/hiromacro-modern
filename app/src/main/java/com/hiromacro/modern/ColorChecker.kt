package com.hiromacro.modern

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import kotlin.math.abs

object ColorChecker {
    var lastBitmap: Bitmap? = null
    var onScreenshotNeeded: (() -> Unit)? = null

    fun check(x: Int, y: Int, targetColor: Int, tolerance: Int): Boolean {
        val bmp = lastBitmap ?: return false
        if (x < 0 || y < 0 || x >= bmp.width || y >= bmp.height) return false
        val pixel = bmp.getPixel(x, y)
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        val tr = android.graphics.Color.red(targetColor)
        val tg = android.graphics.Color.green(targetColor)
        val tb = android.graphics.Color.blue(targetColor)
        val diff = abs(r - tr) + abs(g - tg) + abs(b - tb)
        return diff <= tolerance * 3
    }

    fun getColorAt(x: Int, y: Int): Int {
        val bmp = lastBitmap ?: return 0
        if (x < 0 || y < 0 || x >= bmp.width || y >= bmp.height) return 0
        return bmp.getPixel(x, y)
    }

    fun updateBitmap(bmp: Bitmap) {
        lastBitmap?.recycle()
        lastBitmap = bmp.copy(Bitmap.Config.ARGB_8888, false)
    }
}
