package com.hiromacro.modern

object MacroEngine {

    fun play(steps: List<MacroStep.Touch>) {
        if (steps.isEmpty()) return
        MacroAccessibilityService.startPlayback(steps)
    }

    fun playOne(x: Int, y: Int) {
        play(listOf(MacroStep.Touch(x, y)))
    }
}
