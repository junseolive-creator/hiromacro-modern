package com.hiromacro.modern

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class ColorPickerOverlay(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    var onColorPicked: ((x: Int, y: Int, color: Int) -> Unit)? = null

    fun show() {
        if (overlayView != null) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = View(context).apply {
            setBackgroundColor(0x22000000)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    val color = ColorChecker.getColorAt(x, y)
                    val hex = String.format("#%06X", 0xFFFFFF and color)
                    Toast.makeText(context, "Picked $x,$y $hex", Toast.LENGTH_LONG).show()
                    onColorPicked?.invoke(x, y, color)
                    hide()
                }
                true
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager?.addView(overlayView, params)
        Toast.makeText(context, "원하는 위치를 터치하세요 - 스포이드", Toast.LENGTH_LONG).show()
    }

    fun hide() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {}
        overlayView = null
    }
}
