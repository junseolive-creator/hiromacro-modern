package com.hiromacro.modern

import android.graphics.Color
import kotlin.math.abs

sealed interface MacroStep {
    data class Tap(val xPct: Float, val yPct: Float) : MacroStep
    data class Wait(val ms: Long) : MacroStep
    data class Label(val name: String) : MacroStep
    data class Jump(val label: String) : MacroStep
    data class ColorCheck(
        val xPct: Float,
        val yPct: Float,
        val r: Int,
        val g: Int,
        val b: Int,
        val tolerance: Int = 15,
        val jumpIfMatch: String? = null,
        val jumpIfNotMatch: String? = null
    ) : MacroStep
}

class MacroEngine(
    private val tap: (xPct: Float, yPct: Float) -> Unit = { _, _ -> },
    private val getColor: (xPct: Float, yPct: Float) -> Int = { _, _ -> 0 }
) {
    @Volatile
    var running: Boolean = false
        private set

    private var worker: Thread? = null

    fun isColorMatch(actual: Int, r: Int, g: Int, b: Int, tolerance: Int): Boolean {
        return abs(Color.red(actual) - r) <= tolerance &&
            abs(Color.green(actual) - g) <= tolerance &&
            abs(Color.blue(actual) - b) <= tolerance
    }

    fun start(steps: List<MacroStep>) {
        if (running) return
        running = true
        worker = Thread {
            val labels = mutableMapOf<String, Int>()
            steps.forEachIndexed { index, step ->
                if (step is MacroStep.Label) labels[step.name] = index
            }
            var pc = 0
            var guard = 0
            while (running && pc < steps.size && guard < 100000) {
                guard++
                when (val step = steps[pc]) {
                    is MacroStep.Tap -> {
                        tap(step.xPct, step.yPct)
                        pc++
                    }
                    is MacroStep.Wait -> {
                        try { Thread.sleep(step.ms) } catch (_: InterruptedException) { break }
                        pc++
                    }
                    is MacroStep.Label -> pc++
                    is MacroStep.Jump -> {
                        pc = labels[step.label] ?: (pc + 1)
                    }
                    is MacroStep.ColorCheck -> {
                        val actual = try { getColor(step.xPct, step.yPct) } catch (_: Exception) { 0 }
                        val match = isColorMatch(actual, step.r, step.g, step.b, step.tolerance)
                        val target = if (match) step.jumpIfMatch else step.jumpIfNotMatch
                        pc = if (target != null) (labels[target] ?: (pc + 1)) else pc + 1
                    }
                }
            }
            running = false
        }
        worker?.start()
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }
}
