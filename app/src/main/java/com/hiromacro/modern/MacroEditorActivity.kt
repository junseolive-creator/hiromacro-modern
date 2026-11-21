package com.hiromacro.modern

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import android.app.Activity

object MacroStore {
    var current: Macro = Macro("새 매크로", mutableListOf())
    var list: MutableList<Macro> = mutableListOf()
}

class MacroEditorActivity : Activity() {

    private lateinit var listLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "편집: ${MacroStore.current.name}"
            textSize = 22f
        }
        root.addView(title)

        listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listLayout)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        fun addBtn(label: String, onClick: () -> Unit) {
            val b = Button(this).apply { text = label }
            b.setOnClickListener { onClick() }
            btnRow.addView(b)
        }

        addBtn("● 녹화 시작") {
MacroAccessibilityService.startRecording()
    Toast.makeText(this, "녹화 시작 - 플로팅 버튼 확인", Toast.LENGTH_SHORT).show()
    finish()
}
        addBtn("터치+") { askTouch() }
        addBtn("대기+") { askWait() }
        addBtn("스와이프+") { askSwipe() }
        addBtn("색상+") { askColor() }
        addBtn("Goto+") { askGoto() }
        
    
        root.addView(btnRow)

        val save = Button(this).apply { text = "저장하고 돌아가기" }
        save.setOnClickListener {
            if (!MacroStore.list.contains(MacroStore.current)) {
                MacroStore.list.add(MacroStore.current)
            }
            finish()
        }
        root.addView(save)

        refresh()
    }

    private fun refresh() {
        listLayout.removeAllViews()
        MacroStore.current.steps.forEachIndexed { idx, s ->
            val label = when (s) {
                is MacroStep.Touch -> "[$idx] 터치 ${s.x},${s.y}"
                is MacroStep.Wait -> "[$idx] 대기 ${s.ms}ms"
                is MacroStep.Swipe -> "[$idx] 스와이프 (${s.startX},${s.startY}) -> (${s.endX},${s.endY}) ${s.durationMs}ms"
                is MacroStep.ColorCheck -> "[$idx] 색상 ${s.x},${s.y} = ${s.r},${s.g},${s.b} 틀리면 Goto ${s.gotoOnFail}"
                is MacroStep.Goto -> "[$idx] Goto ${s.line}"
                is MacroStep.End -> "[$idx] 종료"
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(this).apply { text = label; textSize = 16f }
            val del = Button(this).apply { text = "삭제" }
            del.setOnClickListener {
                MacroStore.current.steps.removeAt(idx)
                refresh()
            }
            row.addView(tv)
            row.addView(del)
            listLayout.addView(row)
        }
    }

    private fun inputDialog(title: String, hints: List<String>, onDone: (List<String>) -> Unit) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val edits = hints.map { h ->
            EditText(this).apply {
                hint = h
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                layout.addView(this)
            }
        }
        AlertDialog.Builder(this).setTitle(title).setView(layout)
            .setPositiveButton("추가") { _, _ -> onDone(edits.map { it.text.toString() }) }
            .setNegativeButton("취소", null).show()
    }

    private fun askTouch() {
        inputDialog("터치 추가", listOf("X", "Y")) { v ->
            val x = v[0].toIntOrNull() ?: 0
            val y = v[1].toIntOrNull() ?: 0
            MacroStore.current.steps.add(MacroStep.Touch(x, y))
            refresh()
        }
    }
    private fun askWait() {
        inputDialog("대기 추가", listOf("밀리초, 예: 1000 = 1초")) { v ->
            val ms = v[0].toLongOrNull() ?: 1000
            MacroStore.current.steps.add(MacroStep.Wait(ms))
            refresh()
        }
    }
    private fun askSwipe() {
        inputDialog("스와이프 추가", listOf("시작 X", "시작 Y", "끝 X", "끝 Y", "시간 ms, 예: 300")) { v ->
            val sx = v[0].toIntOrNull() ?: 0
            val sy = v[1].toIntOrNull() ?: 0
            val ex = v[2].toIntOrNull() ?: 0
            val ey = v[3].toIntOrNull() ?: 0
            val d = v[4].toLongOrNull() ?: 300
            MacroStore.current.steps.add(MacroStep.Swipe(sx, sy, ex, ey, d))
            refresh()
        }
    }
    private fun askColor() {
        inputDialog("색상 체크 추가", listOf("X", "Y", "R", "G", "B", "틀리면 Goto 라인, 예: 0")) { v ->
            val x = v[0].toIntOrNull() ?: 0
            val y = v[1].toIntOrNull() ?: 0
            val r = v[2].toIntOrNull() ?: 0
            val g = v[3].toIntOrNull() ?: 0
            val b = v[4].toIntOrNull() ?: 0
            val go = v[5].toIntOrNull() ?: -1
            MacroStore.current.steps.add(MacroStep.ColorCheck(x, y, r, g, b, 20, go))
            refresh()
        }
    }
    private fun askGoto() {
        inputDialog("Goto 추가", listOf("이동할 라인 번호, 예: 0")) { v ->
            val line = v[0].toIntOrNull() ?: 0
            MacroStore.current.steps.add(MacroStep.Goto(line))
            refresh()
        }
    }
}
