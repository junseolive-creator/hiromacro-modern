package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MacroAccessibilityService? = null
        var isRunning = false
    }

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

    fun startAqua() {
        val svc = instance ?: return
        val metrics = resources.displayMetrics
        MacroEngine.onLog = { msg ->
            android.util.Log.d("HiroMacro", msg)
        }
        MacroEngine.start(svc, MacroEngine.aquaPreset(), metrics.widthPixels, metrics.heightPixels)
    }

    fun stopMacro() {
        MacroEngine.stop()
    }
}
