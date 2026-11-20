package com.hiromacro.modern

import java.io.Serializable

sealed class MacroStep : Serializable {
    data class Touch(val x: Int, val y: Int) : MacroStep()
    data class Wait(val ms: Long) : MacroStep()
    data class Swipe(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        val durationMs: Long = 300
    ) : MacroStep()
    data class ColorCheck(
        val x: Int,
        val y: Int,
        val r: Int,
        val g: Int,
        val b: Int,
        val tolerance: Int = 20,
        val gotoOnFail: Int = -1
    ) : MacroStep()
    data class Goto(val line: Int) : MacroStep()
    object End : MacroStep()
}

data class Macro(
    val name: String,
    val steps: MutableList<MacroStep>
) : Serializable
