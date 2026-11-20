package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.GestureDescription

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MacroAccessibilityService? = null
        var isRunning = false
    }

    private var engine: MacroEngine? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun tap(xPct: Float, yPct: Float) {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * xPct / 100f
        val y = metrics.heightPixels * yPct / 100f
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun getColor(xPct: Float, yPct: Float): Int {
        // TODO: 스크린샷 기반 색상 체크는 다음 단계에서 연결
        return 0
    }

    fun startAqua() {
        val svc = instance ?: return
        val eng = MacroEngine(
            tap = { x, y -> svc.tap(x, y) },
            getColor = { x, y -> svc.getColor(x, y) }
        )
        engine = eng
        // 아쿠아리움 기본 루프: 490,190 = 68.05%,14.84% / 에너지 453,1030 = 62.9%,80.46%
        val steps = listOf<MacroStep>(
            MacroStep.Label("start"),
            MacroStep.ColorCheck(68.05f, 14.84f, 47, 147, 193, 15, "aquaok", "start"),
            MacroStep.Label("aquaok"),
            MacroStep.Tap(62.9f, 80.46f),
            MacroStep.Wait(1000),
            MacroStep.Jump("start")
        )
        eng.start(steps)
    }

    fun stopMacro() {
        engine?.stop()
        engine = null
    }
}
