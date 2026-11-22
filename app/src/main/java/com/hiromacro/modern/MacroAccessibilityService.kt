package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MacroAccessibilityService? = null

        fun showControlOverlay() {
            instance?.showControlOverlayInternal()
        }

        fun hideControlOverlay() {
            instance?.hideControlOverlayInternal()
        }

        fun startRecording() {
            instance?.startRecordingInternal()
        }

        fun stopRecordingAndSave() {
            instance?.stopRecordingAndSaveInternal()
        }

        fun startPlayback() {
            instance?.startPlaybackInternal()
        }
    }

    private var windowManager: WindowManager? = null
    private var controlView: View? = null
    private var markerView: View? = null
    private var markerText: TextView? = null

    private var isRecording = false
    private var currentSteps = mutableListOf<MacroStep>()
    private var lastEventTime = 0L

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
        super.onDestroy()
        hideControlOverlayInternal()
        hidePlayMarker()
        instance = null
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording) return
        if (event == null) return

        // 클릭 / 롱클릭 / 선택만 녹화
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SELECTED) {
            return
        }

        val source = event.source ?: return
        val rect = Rect()
        source.getBoundsInScreen(rect)
        source.recycle()

        if (rect.width() <= 0 || rect.height() <= 0) return

        val x = (rect.left + rect.right) / 2
        val y = (rect.top + rect.bottom) / 2

        // 같은 위치 연속 중복 방지 (300ms 이내)
        val now = System.currentTimeMillis()
        if (currentSteps.isNotEmpty() && now - lastEventTime < 300) {
            val last = currentSteps.lastOrNull()
            if (last is MacroStep.Touch) {
                val dx = kotlin.math.abs(last.x - x)
                val dy = kotlin.math.abs(last.y - y)
                if (dx < 20 && dy < 20) return
            }
        }
        lastEventTime = now

        // 이전 터치와의 시간 간격 저장
        if (currentSteps.isNotEmpty()) {
            val wait = (now - lastEventTime).coerceIn(0, 5000)
        }

        currentSteps.add(MacroStep.Touch(x, y))
        showPlayMarker(x, y, currentSteps.size)
        Toast.makeText(this, "${currentSteps.size}번 터치 기록", Toast.LENGTH_SHORT).show()
    }

    // ================= 컨트롤 오버레이 =================

    private fun showControlOverlayInternal() {
        if (controlView != null) return
        val wm = windowManager ?: return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_control, null)

        val btnRecord = view.findViewById<Button>(R.id.btnRecord)
        val btnPlay = view.findViewById<Button>(R.id.btnPlay)
        val btnStop = view.findViewById<Button>(R.id.btnStop)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        btnRecord.setOnClickListener { startRecordingInternal() }
        btnPlay.setOnClickListener { startPlaybackInternal() }
        btnStop.setOnClickListener { stopRecordingAndSaveInternal() }
        btnClose.setOnClickListener { hideControlOverlayInternal() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.y = 200

        try {
            wm.addView(view, params)
            controlView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideControlOverlayInternal() {
        try {
            controlView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        controlView = null
    }

    // ================= 녹화 =================

    private fun startRecordingInternal() {
        if (isRecording) return
        isRecording = true
        currentSteps = mutableListOf()
        lastEventTime = System.currentTimeMillis()
        Toast.makeText(this, "녹화 시작", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecordingAndSaveInternal() {
        if (!isRecording) return
        isRecording = false
        hidePlayMarker()

        if (currentSteps.isEmpty()) {
            Toast.makeText(this, "기록된 터치가 없어요", Toast.LENGTH_SHORT).show()
            return
        }

        // 저장
        MacroStore.save(this, "매크로 1", currentSteps)
        Toast.makeText(this, "${currentSteps.size}개 터치 저장 완료", Toast.LENGTH_SHORT).show()
    }

    // ================= 재생 =================

    private fun startPlaybackInternal() {
        if (MacroEngine.isPlaying()) {
            Toast.makeText(this, "이미 재생 중", Toast.LENGTH_SHORT).show()
            return
        }

        val steps = MacroStore.loadLast(this)
        if (steps.isNullOrEmpty()) {
            Toast.makeText(this, "저장된 매크로가 없어요", Toast.LENGTH_SHORT).show()
            return
        }

        // 재생 중에는 컨트롤 오버레이를 잠깐 숨겨야 터치가 먹힘
        hideControlOverlayInternal()

        MacroEngine.play(this, steps,
            onShowMarker = { x, y, idx ->
                showPlayMarker(x, y, idx)
            },
            onFinished = {
                hidePlayMarker()
                showControlOverlayInternal()
            }
        )
    }

    // ================= 마커 =================

    private fun showPlayMarker(x: Int, y: Int, idx: Int) {
        handler.post {
            try {
                val wm = windowManager ?: return@post
                if (markerView == null) {
                    val inflater = LayoutInflater.from(this)
                    val v = inflater.inflate(R.layout.view_play_marker, null)
                    markerText = v.findViewById(R.id.markerNumber)
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                    )
                    params.gravity = Gravity.TOP or Gravity.START
                    wm.addView(v, params)
                    markerView = v
                }
                markerText?.text = idx.toString()
                val params = markerView?.layoutParams as? WindowManager.LayoutParams
                if (params != null) {
                    params.x = x - 50
                    params.y = y - 50
                    wm.updateViewLayout(markerView, params)
                }
                markerView?.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hidePlayMarker() {
        handler.post {
            try {
                markerView?.let { windowManager?.removeView(it) }
            } catch (e: Exception) {
            }
            markerView = null
            markerText = null
        }
    }
}
