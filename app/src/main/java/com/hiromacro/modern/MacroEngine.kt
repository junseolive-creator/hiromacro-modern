package com.hiromacro.modern

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import kotlinx.coroutines.*

class MacroEngine(
    private val service: AccessibilityService,
    private val getColor: (x: Int, y: Int) -> Int
) {
    @Volatile var running = false
    private var job: Job? = null

    fun start(macro: Macro) {
    job?.cancel()
    running = false
    running = true
        job = CoroutineScope(Dispatchers.Default).launch {
            var pc = 0 // 라인 번호, 0부터 시작
            var guard = 0
            while (running && pc in macro.steps.indices) {
                guard++
                if (guard > 100000) break
                when (val s = macro.steps[pc]) {
                    is MacroStep.Touch -> {
                        tap(s.x, s.y)
                        pc++
                    }
                    is MacroStep.Wait -> {
                        delay(s.ms)
                        pc++
                    }
                    is MacroStep.Swipe -> {
                        swipe(s.startX, s.startY, s.endX, s.endY, s.durationMs)
                        pc++
                    }
                    is MacroStep.ColorCheck -> {
                        val c = getColor(s.x, s.y)
                        val r = android.graphics.Color.red(c)
                        val g = android.graphics.Color.green(c)
                        val b = android.graphics.Color.blue(c)
                        val ok = kotlin.math.abs(r - s.r) <= s.tolerance &&
                               kotlin.math.abs(g - s.g) <= s.tolerance &&
                               kotlin.math.abs(b - s.b) <= s.tolerance
                        if (ok) {
                            pc++
                        } else {
                            if (s.gotoOnFail >= 0) {
                                pc = s.gotoOnFail
                            } else {
                                delay(100)
                            }
                        }
                    }
                    is MacroStep.Goto -> {
                        pc = s.line
                    }
                    is MacroStep.End -> break
                }
            }
            running = false
        }
    }

    fun stop() {
        running = false
        job?.cancel()
    }

    private suspend fun tap(x: Int, y: Int) {
    val p = Path().apply {
        moveTo(x.toFloat(), y.toFloat())
        lineTo(x.toFloat() + 1f, y.toFloat() + 1f)
    }
    val g = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(p, 0, 100))
        .build()
    val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
    service.dispatchGesture(g, object : AccessibilityService.GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription?) {
        deferred.complete(true)
        }
    override fun onCancelled(gestureDescription: GestureDescription?) {
        deferred.complete(false)
        }
    }, null)
val ok = deferred.await()
    android.util.Log.d("HiroMacro", "tap $x,$y result=$ok")
    delay(200)
    }

    private suspend fun swipe(sx: Int, sy: Int, ex: Int, ey: Int, dur: Long) {
        val p = Path().apply {
            moveTo(sx.toFloat(), sy.toFloat())
            lineTo(ex.toFloat(), ey.toFloat())
        }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, dur))
            .build()
        withContext(Dispatchers.Main) {
            service.dispatchGesture(g, null, null)
        }
        delay(50)
    }
}
