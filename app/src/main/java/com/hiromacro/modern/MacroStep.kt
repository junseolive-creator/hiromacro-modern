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
object MacroStore {
    var list = mutableListOf<Macro>()
    var current: Macro = Macro("새 매크로", mutableListOf())

    fun save(ctx: android.content.Context) {
        val prefs = ctx.getSharedPreferences("macros", android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (m in list) {
            val o = org.json.JSONObject()
            o.put("name", m.name)
            val steps = org.json.JSONArray()
            for (s in m.steps) {
                val so = org.json.JSONObject()
                when (s) {
                    is MacroStep.Touch -> {
                        so.put("type", "Touch")
                        so.put("x", s.x); so.put("y", s.y)
                    }
                    is MacroStep.Wait -> {
                        so.put("type", "Wait")
                        so.put("ms", s.ms)
                    }
                    is MacroStep.Swipe -> {
                        so.put("type", "Swipe")
                        so.put("startX", s.startX); so.put("startY", s.startY)
                        so.put("endX", s.endX); so.put("endY", s.endY)
                        so.put("durationMs", s.durationMs)
                    }
                    is MacroStep.ColorCheck -> {
                        so.put("type", "ColorCheck")
                        so.put("x", s.x); so.put("y", s.y)
                        so.put("r", s.r); so.put("g", s.g); so.put("b", s.b)
                        so.put("tolerance", s.tolerance); so.put("gotoOnFail", s.gotoOnFail)
                    }
                    is MacroStep.Goto -> {
                        so.put("type", "Goto")
                        so.put("line", s.line)
                    }
                    is MacroStep.End -> {
                        so.put("type", "End")
                    }
                }
                steps.put(so)
            }
            o.put("steps", steps)
            arr.put(o)
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    fun load(ctx: android.content.Context) {
        val prefs = ctx.getSharedPreferences("macros", android.content.Context.MODE_PRIVATE)
        val str = prefs.getString("list", null) ?: return
        list.clear()
        val arr = org.json.JSONArray(str)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val steps = mutableListOf<MacroStep>()
            val ja = o.getJSONArray("steps")
            for (j in 0 until ja.length()) {
                val so = ja.getJSONObject(j)
                when (so.getString("type")) {
                    "Touch" -> steps.add(MacroStep.Touch(so.getInt("x"), so.getInt("y")))
                    "Wait" -> steps.add(MacroStep.Wait(so.getLong("ms")))
                    "Swipe" -> steps.add(MacroStep.Swipe(so.getInt("startX"), so.getInt("startY"), so.getInt("endX"), so.getInt("endY"), so.getLong("durationMs")))
                    "ColorCheck" -> steps.add(MacroStep.ColorCheck(so.getInt("x"), so.getInt("y"), so.getInt("r"), so.getInt("g"), so.getInt("b"), so.getInt("tolerance"), so.getInt("gotoOnFail")))
                    "Goto" -> steps.add(MacroStep.Goto(so.getInt("line")))
                    "End" -> steps.add(MacroStep.End)
                }
            }
            list.add(Macro(o.getString("name"), steps))
        }
        if (list.isNotEmpty()) current = list.last()
        }
    }
