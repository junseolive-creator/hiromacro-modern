package com.hiromacro.modern

sealed class MacroStep {
    data class Touch(val x: Int, val y: Int) : MacroStep()
    data class Wait(val millis: Long) : MacroStep()
}
