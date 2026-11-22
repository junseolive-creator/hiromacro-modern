package com.hiromacro.modern

import android.content.Context

object MacroStore {
    private const val PREF = "hiromacro_pref"
    private const val KEY = "last_macro"

    fun save(context: Context, name: String, steps: List<MacroStep>) {
        // T,100,200 / W,500 형태로 저장
        val text = steps.joinToString(";") {
            when (it) {
                is MacroStep.Touch -> "T,${it.x},${it.y}"
                is MacroStep.Wait -> "W,${it.millis}"
            }
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, text).apply()
    }

    fun loadLast(context: Context): List<MacroStep>? {
        val text = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        if (text.isBlank()) return null
        return text.split(";").mapNotNull { part ->
            val p = part.split(",")
            if (p.isEmpty()) return@mapNotNull null
            when (p[0]) {
                "T" -> MacroStep.Touch(p[1].toInt(), p[2].toInt())
                "W" -> MacroStep.Wait(p[1].toLong())
                else -> null
            }
        }
    }
}
