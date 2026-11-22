package com.example.hiromacromodern

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MacroAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Companion.instance = this
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!Companion.isRecording) return
        if (Companion.recordingOverlay != null) return
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event?.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return
        val node = event.source ?: return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val x = rect.centerX()
        val y = rect.centerY()
        if (y < 250) return
        Companion.recordedSteps.add(MacroStep.Touch(x, y))
        Companion.showMarker2(x, y, Companion.recordedSteps.size)
        node.recycle()
    }

    companion object {
        var instance: MacroAccessibilityService? = null
        var isRecording = false
        var recordedSteps = mutableListOf<MacroStep.Touch>()
        var recordingOverlay: View? = null
        var markerOverlay: FrameLayout? = null
        var controlOverlay: View? = null
        var onStopListener: (() -> Unit)? = null

        fun showControlOverlay() {
            val ctx = instance ?: return
            hideControlOverlay()
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 80
            val layout = LinearLayout(ctx)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setBackgroundColor(Color.parseColor("#CC000000"))
            layout.setPadding(12, 12, 12, 12)
            fun makeBtn(text: String, action: () -> Unit): Button {
                val b = Button(ctx)
                b.text = text
                b.setTextColor(Color.WHITE)
                b.setOnClickListener { action() }
                return b
            }
            layout.addView(makeBtn("●") { startRecording() })
            layout.addView(makeBtn("■") { stopRecording() })
            layout.addView(makeBtn("▶") { startPlayback(recordedSteps.toList()) })
            layout.addView(makeBtn("X") { hideControlOverlay(); hideRecordingOverlay(); hideMarkerLayer() })
            controlOverlay = layout
            wm.addView(layout, params)
        }

        fun hideControlOverlay() {
            val ctx = instance ?: return
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            try { if (controlOverlay != null) wm.removeView(controlOverlay) } catch (_: Exception) {}
            controlOverlay = null
        }

        fun showMarkerLayer() {
            if (markerOverlay != null) return
            val ctx = instance ?: return
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            val layout = FrameLayout(ctx)
            markerOverlay = layout
            wm.addView(layout, params)
        }

        fun hideMarkerLayer() {
            val ctx = instance ?: return
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            try { if (markerOverlay != null) wm.removeView(markerOverlay) } catch (_: Exception) {}
            markerOverlay = null
        }

        fun showMarker2(x: Int, y: Int, num: Int) {
            val overlay = markerOverlay ?: return
            val ctx = instance ?: return
            val tv = TextView(ctx)
            tv.text = num.toString()
            tv.setTextColor(Color.WHITE)
            tv.setBackgroundColor(Color.RED)
            tv.textSize = 14f
            tv.gravity = Gravity.CENTER
            val size = 80
            val lp = FrameLayout.LayoutParams(size, size)
            lp.leftMargin = x - size / 2
            lp.topMargin = y - size / 2
            overlay.addView(tv, lp)
        }

        fun showPlayMarker(x: Int, y: Int) {
            showMarkerLayer()
            val overlay = markerOverlay ?: return
            val ctx = instance ?: return
            val tv = TextView(ctx)
            tv.text = "▶"
            tv.setTextColor(Color.WHITE)
            tv.setBackgroundColor(Color.BLUE)
            tv.textSize = 14f
            tv.gravity = Gravity.CENTER
            val size = 80
            val lp = FrameLayout.LayoutParams(size, size)
            lp.leftMargin = x - size / 2
            lp.topMargin = y - size / 2
            overlay.addView(tv, lp)
            Handler(Looper.getMainLooper()).postDelayed({
                try { overlay.removeView(tv) } catch (_: Exception) {}
            }, 500)
        }

        fun showRecordingOverlay() {
            hideRecordingOverlay()
            showMarkerLayer()
            val ctx = instance ?: return
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            val layout = FrameLayout(ctx)
            layout.setBackgroundColor(Color.TRANSPARENT)
            layout.setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_DOWN && isRecording) {
                    val x = ev.x.toInt()
                    val y = ev.y.toInt()
                    recordedSteps.add(MacroStep.Touch(x, y))
                    showMarker2(x, y, recordedSteps.size)
                    hideRecordingOverlay()
                    val path = GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(
                            Path().apply { moveTo(x.toFloat(), y.toFloat()) }, 0, 100
                        )).build()
                    instance?.dispatchGesture(path, null, null)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isRecording) showRecordingOverlay()
                    }, 200)
                    true
                } else false
            }
            recordingOverlay = layout
            wm.addView(layout, params)
            showControlOverlay()
        }

        fun hideRecordingOverlay() {
            val ctx = instance ?: return
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            try { if (recordingOverlay != null) wm.removeView(recordingOverlay) } catch (_: Exception) {}
            recordingOverlay = null
        }

        fun startRecording() {
            recordedSteps.clear()
            isRecording = true
            showRecordingOverlay()
        }

        fun stopRecording() {
            isRecording = false
            hideRecordingOverlay()
            onStopListener?.invoke()
        }

        fun startPlayback(steps: List<MacroStep.Touch>) {
            if (steps.isEmpty()) return
            showMarkerLayer()
            var index = 0
            val handler = Handler(Looper.getMainLooper())
            fun next() {
                if (index >= steps.size) return
                val s = steps[index]
                showPlayMarker(s.x, s.y)
                val path = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(
                        Path().apply { moveTo(s.x.toFloat(), s.y.toFloat()) }, 0, 100
                    )).build()
                instance?.dispatchGesture(path, null, null)
                index++
                handler.postDelayed({ next() }, 400)
            }
            next()
        }
    }
}
