package com.hiromacro.modern
import android.graphics.Rect
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var recordedSteps = mutableListOf<MacroStep>()
        var recordingTarget: Macro? = null
        var instance: MacroAccessibilityService? = null
        var engine: MacroEngine? = null
        var isRecording = false

        fun startMacro(macro: Macro) {
            val svc = instance ?: return
            engine?.stop()
            engine = MacroEngine(svc) { _, _ -> 0 }
            engine?.start(macro)
        }

        fun stopMacro() {
            engine?.stop()
            engine = null
            instance?.hideFloating()
        }

        fun startRecording() {
            recordingTarget = MacroStore.current
            recordedSteps.clear()
            isRecording = true
            instance?.showFloating()
        }

        fun stopRecording() {
            isRecording = false
            instance?.hideFloating()
            recordingTarget?.let { target ->
                target.steps.addAll(recordedSteps)
                if (!MacroStore.list.contains(target)) {
                   MacroStore.list.add(target)
                }
            }
            recordedSteps.clear()
         }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    private fun showFloating() {
        if (floatingView != null) return
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnStop = Button(this).apply {
                text = "■ 정지"
                setOnClickListener {
                    stopRecording()
                }
            }
            val btnPause = Button(this).apply {
                text = "|| 일시정지"
                setOnClickListener {
                    // 일시정지는 다음 단계에서 녹화 중단 플래그로 연결
                    isRecording = false
                    text = "▶ 계속"
                }
            }
            layout.addView(btnStop)
            layout.addView(btnPause)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100
            }

            floatingView = layout
            wm.addView(layout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideFloating() {
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        floatingView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording) return
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) return
        val node = event.source ?: return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        node.recycle()
        if (rect.width() == 0 || rect.height() == 0) return
        recordedSteps.add(MacroStep.Touch(rect.centerX(), rect.centerY()))
}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        hideFloating()
        instance = null
    }
}
