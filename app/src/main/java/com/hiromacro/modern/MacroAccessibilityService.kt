package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MacroAccessibilityService? = null
        var engine: MacroEngine? = null

        fun startMacro(macro: Macro) {
            val svc = instance
            if (svc == null) {
                return
            }
            engine?.stop()
            // 색상 읽기는 다음 단계에서 MediaProjection으로 연결할 거야.
            // 지금은 터치 / 대기 / 스와이프 / Goto 먼저 돌아가게 한다.
            engine = MacroEngine(svc) { _, _ -> 0 }
            engine?.start(macro)
        }

        fun stopMacro() {
            engine?.stop()
            engine = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
