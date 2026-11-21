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
        var markerCount = 0
        var recordingOverlay: android.view.View? = null
        var stopButtonView: android.view.View? = null
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
            markerCount = 0
            isRecording = true
            instance?.showFloating()
            instance?.showRecordingOverlay()
        }

        fun stopRecording() {
            isRecording = false
            instance?.hideFloating()
            instance?.hideRecordingOverlay()
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
fun showRecordingOverlay() {
    val ctx = instance ?: return
    if (recordingOverlay != null) return
    val wm = ctx.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager

    // 1. 동그라미만 보여주는 막 - 터치 절대 안 받음
    val params = android.view.WindowManager.LayoutParams(
        android.view.WindowManager.LayoutParams.MATCH_PARENT,
        android.view.WindowManager.LayoutParams.MATCH_PARENT,
        android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        android.graphics.PixelFormat.TRANSLUCENT
    )
    val layout = android.widget.FrameLayout(ctx)
    recordingOverlay = layout
    wm.addView(layout, params)

    // 2. 정지 버튼은 따로 띄움 - 얘만 터치 받음
    val stopBtn = android.widget.Button(ctx)
    stopBtn.text = "■ 정지"
    stopBtn.setBackgroundColor(android.graphics.Color.RED)
    stopBtn.setTextColor(android.graphics.Color.WHITE)
    stopBtn.textSize = 18f
    stopBtn.setOnClickListener {
        stopRecording()
    }
    val stopParams = android.view.WindowManager.LayoutParams(
        android.view.WindowManager.LayoutParams.WRAP_CONTENT,
        android.view.WindowManager.LayoutParams.WRAP_CONTENT,
        android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.TRANSLUCENT
    )
    stopParams.x = 50
    stopParams.y = 200
    stopParams.gravity = android.view.Gravity.TOP or android.view.Gravity.LEFT
    stopButtonView = stopBtn
    wm.addView(stopBtn, stopParams)
}
fun showControlOverlay() {
    val ctx = instance ?: return
    if (stopButtonView != null) return
    val wm = ctx.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager

    val layout = android.widget.LinearLayout(ctx)
    layout.orientation = android.widget.LinearLayout.HORIZONTAL

    val recBtn = android.widget.Button(ctx)
    recBtn.text = "● 녹화"
    recBtn.setBackgroundColor(android.graphics.Color.RED)
    recBtn.setTextColor(android.graphics.Color.WHITE)
    recBtn.setOnClickListener { startRecording() }

    val stopBtn = android.widget.Button(ctx)
    stopBtn.text = "■ 정지"
    stopBtn.setBackgroundColor(android.graphics.Color.BLACK)
    stopBtn.setTextColor(android.graphics.Color.WHITE)
    stopBtn.setOnClickListener { stopRecording() }

    val playBtn = android.widget.Button(ctx)
    playBtn.text = "▶ 플레이"
    playBtn.setBackgroundColor(android.graphics.Color.GREEN)
    playBtn.setTextColor(android.graphics.Color.WHITE)
    playBtn.setOnClickListener {
        val macro = recordingTarget
        if (macro != null) startMacro(macro)
    }

    layout.addView(recBtn)
    layout.addView(stopBtn)
    layout.addView(playBtn)

    val params = android.view.WindowManager.LayoutParams(
        android.view.WindowManager.LayoutParams.WRAP_CONTENT,
        android.view.WindowManager.LayoutParams.WRAP_CONTENT,
        android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.TRANSLUCENT
    )
    params.gravity = android.view.Gravity.TOP or android.view.Gravity.LEFT
    params.x = 20
    params.y = 200

    stopButtonView = layout
    wm.addView(layout, params)
}
fun showMarker(parent: android.widget.FrameLayout, x: Int, y: Int, num: Int) {
    val ctx = instance ?: return
    val tv = android.widget.TextView(ctx)
    tv.text = num.toString()
    tv.textSize = 16f
    tv.setTextColor(android.graphics.Color.WHITE)
    tv.gravity = android.view.Gravity.CENTER

    // 동그랗게 만들기
    val drawable = android.graphics.drawable.GradientDrawable()
    drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
    drawable.setColor(android.graphics.Color.RED)
    tv.background = drawable

    // 상태바 높이 빼기
    var statusBarHeight = 0
    val resId = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resId > 0) {
        statusBarHeight = ctx.resources.getDimensionPixelSize(resId)
    }

    val size = 80
    val p = android.widget.FrameLayout.LayoutParams(size, size)
    p.leftMargin = x - 40
    p.topMargin = y - 40 - statusBarHeight
    parent.addView(tv, p)
}

fun hideRecordingOverlay() {
    try {
        val ctx = instance ?: return
        val wm = ctx.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        recordingOverlay?.let { wm.removeView(it) }
    stopButtonView?.let { wm.removeView(it) }
    } catch (e: Exception) { }
    recordingOverlay = null
    stopButtonView = null
}
}
