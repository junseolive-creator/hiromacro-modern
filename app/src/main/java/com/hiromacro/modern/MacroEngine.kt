package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log

object MacroEngine {

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentJob: Runnable? = null

    fun isPlaying(): Boolean = isPlaying

    fun stop() {
        isPlaying = false
        currentJob?.let { handler.removeCallbacks(it) }
        currentJob = null
    }

    fun play(
        service: AccessibilityService,
        steps: List<MacroStep>,
        onShowMarker: (x: Int, y: Int, index: Int) -> Unit,
        onFinished: () -> Unit
    ) {
        if (isPlaying) return
        if (steps.isEmpty()) {
            onFinished()
            return
        }
        isPlaying = true

        // 재생 시작 전에 오버레이가 터치를 가로막지 않게 300ms 대기
        var index = 0

        val job = object : Runnable {
            override fun run() {
                if (!isPlaying) {
                    onFinished()
                    return
                }
                if (index >= steps.size) {
                    isPlaying = false
                    onFinished()
                    return
                }

                val step = steps[index]
                if (step is MacroStep.Touch) {
                    // 1. 마커 먼저 표시
                    onShowMarker(step.x, step.y, index + 1)

                    // 2. 150ms 뒤에 진짜 터치 발사
                    handler.postDelayed({
                        if (isPlaying) {
                            tap(service, step.x.toFloat(), step.y.toFloat())
                        }
                    }, 150)

                    index++
                    // 다음 터치까지 600ms 간격
                    handler.postDelayed(this, 600)
                } else if (step is MacroStep.Wait) {
                    index++
                    handler.postDelayed(this, step.millis)
                } else {
                    index++
                    handler.postDelayed(this, 100)
                }
            }
        }
        currentJob = job
        handler.postDelayed(job, 300)
    }

    private fun tap(service: AccessibilityService, x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        // 같은 점으로 lineTo 해야 안드로이드가 탭으로 인식함
        path.lineTo(x + 1f, y + 1f)

        val stroke = GestureDescription.StrokeDescription(path, 0, 120)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        val result = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("MacroEngine", "GESTURE OK $x,$y")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d("MacroEngine", "GESTURE CANCELLED $x,$y")
            }
        }, null)

        Log.d("MacroEngine", "dispatchGesture returned=$result x=$x y=$y")
    }
}
