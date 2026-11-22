package com.hiromacro.modern

import 【entity-android¦canonical_name=android】.accessibilityservice.AccessibilityService
import 【entity-android¦canonical_name=android】.accessibilityservice.GestureDescription
import 【entity-android¦canonical_name=android】.graphics.Path
import 【entity-android¦canonical_name=android】.os.Handler
import 【entity-android¦canonical_name=android】.os.Looper

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
        if (steps.isEmpty()) { onFinished(); return }
        isPlaying = true
        var index = 0
        val job = object : Runnable {
            override fun run() {
                if (!isPlaying) { onFinished(); return }
                if (index >= steps.size) { isPlaying = false; onFinished(); return }
                val step = steps[index]
                if (step is MacroStep.Touch) {
                    onShowMarker(step.x, step.y, index + 1)
                    handler.postDelayed({ if (isPlaying) tap(service, step.x.toFloat(), step.y.toFloat()) }, 150)
                    index++
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
        path.lineTo(x + 1f, y + 1f)
        val stroke = GestureDescription.StrokeDescription(path, 0, 120)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }
}
