package com.hiromacro.modern

class MacroAccessibilityService : android.accessibilityservice.AccessibilityService() {
    companion object {
        var instance: MacroAccessibilityService? = null
        fun showControlOverlay() { instance?.showControlOverlayInternal() }
        fun hideControlOverlay() { instance?.hideControlOverlayInternal() }
        fun startRecording() { instance?.startRecordingInternal() }
        fun stopRecordingAndSave() { instance?.stopRecordingAndSaveInternal() }
        fun startPlayback() { instance?.startPlaybackInternal() }
    }

    private var windowManager: android.view.WindowManager? = null
    private var controlView: android.view.View? = null
    private var markerView: android.view.View? = null
    private var markerText: android.widget.TextView? = null
    private var isRecording = false
    private var currentSteps = mutableListOf<MacroStep>()
    private var lastEventTime = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
    }
    override fun onDestroy() {
        super.onDestroy()
        hideControlOverlayInternal()
        hidePlayMarker()
        instance = null
    }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        if (!isRecording || event == null) return
        if (event.eventType != android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED &&
            event.eventType != android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED) return
        val source = event.source ?: return
        val rect = android.graphics.Rect()
        source.getBoundsInScreen(rect)
        source.recycle()
        if (rect.width() <= 0 || rect.height() <= 0) return
        val x = (rect.left + rect.right) / 2
        val y = (rect.top + rect.bottom) / 2
        val now = System.currentTimeMillis()
        if (currentSteps.isNotEmpty() && now - lastEventTime < 300) {
            val last = currentSteps.lastOrNull()
            if (last is MacroStep.Touch && kotlin.math.abs(last.x - x) < 20 && kotlin.math.abs(last.y - y) < 20) return
        }
        lastEventTime = now
        currentSteps.add(MacroStep.Touch(x, y))
        showPlayMarker(x, y, currentSteps.size)
        android.widget.Toast.makeText(this, "${currentSteps.size}번 터치 기록", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showControlOverlayInternal() {
        if (controlView != null) return
        val wm = windowManager ?: return
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
        }
        fun makeBtn(text: String, onClick: () -> Unit): android.widget.Button {
            val b = android.widget.Button(this)
            b.text = text
            b.setOnClickListener { onClick() }
            return b
        }
        layout.addView(makeBtn("● 녹화") { startRecordingInternal() })
        layout.addView(makeBtn("▶ 재생") { startPlaybackInternal() })
        layout.addView(makeBtn("■ 정지") { stopRecordingAndSaveInternal() })
        layout.addView(makeBtn("X 닫기") { hideControlOverlayInternal() })

        val params = android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        params.y = 200
        try { wm.addView(layout, params); controlView = layout } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideControlOverlayInternal() {
        try { controlView?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
        controlView = null
    }

    private fun startRecordingInternal() {
        if (isRecording) return
        isRecording = true
        currentSteps = mutableListOf()
        lastEventTime = System.currentTimeMillis()
        android.widget.Toast.makeText(this, "녹화 시작", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun stopRecordingAndSaveInternal() {
        if (!isRecording) return
        isRecording = false
        hidePlayMarker()
        if (currentSteps.isEmpty()) { android.widget.Toast.makeText(this, "기록된 터치가 없어요", android.widget.Toast.LENGTH_SHORT).show(); return }
        MacroStore.save(this, "매크로 1", currentSteps)
        android.widget.Toast.makeText(this, "${currentSteps.size}개 터치 저장 완료", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun startPlaybackInternal() {
        if (MacroEngine.isPlaying()) { android.widget.Toast.makeText(this, "이미 재생 중", android.widget.Toast.LENGTH_SHORT).show(); return }
        val steps = MacroStore.loadLast(this)
        if (steps.isNullOrEmpty()) { android.widget.Toast.makeText(this, "저장된 매크로가 없어요", android.widget.Toast.LENGTH_SHORT).show(); return }
        hideControlOverlayInternal()
        MacroEngine.play(this, steps,
            onShowMarker = { x, y, idx -> showPlayMarker(x, y, idx) },
            onFinished = { hidePlayMarker(); showControlOverlayInternal() }
        )
    }

    private fun showPlayMarker(x: Int, y: Int, idx: Int) {
        handler.post {
            try {
                val wm = windowManager ?: return@post
                if (markerView == null) {
                    val tv = android.widget.TextView(this).apply {
                        textSize = 30f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#AAFF0000"))
                        gravity = android.view.Gravity.CENTER
                    }
                    markerText = tv
                    val params = android.view.WindowManager.LayoutParams(
                        100, 100,
                        android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        android.graphics.PixelFormat.TRANSLUCENT
                    )
                    params.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    wm.addView(tv, params)
                    markerView = tv
                }
                markerText?.text = idx.toString()
                val params = markerView?.layoutParams as? android.view.WindowManager.LayoutParams
                if (params != null) { params.x = x - 50; params.y = y - 50; wm.updateViewLayout(markerView, params) }
                markerView?.visibility = android.view.View.VISIBLE
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun hidePlayMarker() {
        handler.post {
            try { markerView?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
            markerView = null; markerText = null
        }
    }
}
