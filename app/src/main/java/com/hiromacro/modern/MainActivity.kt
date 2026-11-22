package com.example.hiromacromodern

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listLayout: LinearLayout
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 32)

        val title = TextView(this)
        title.text = "HiroMacro Modern"
        title.textSize = 24f
        root.addView(title)

        statusText = TextView(this)
        statusText.text = "접근성 서비스를 켜주세요"
        root.addView(statusText)

        fun makeBtn(text: String, action: () -> Unit): Button {
            val b = Button(this)
            b.text = text
            b.setOnClickListener { action() }
            root.addView(b)
            return b
        }

        makeBtn("1. 접근성 설정 열기") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        makeBtn("2. 컨트롤 버튼 띄우기") {
            MacroAccessibilityService.showControlOverlay()
        }

        makeBtn("3. 녹화 시작") {
            MacroAccessibilityService.startRecording()
            moveTaskToBack(true)
        }

        makeBtn("4. 녹화 정지 + 저장") {
            MacroAccessibilityService.stopRecording()
            saveMacro()
        }

        makeBtn("5. 마지막 매크로 재생") {
            val steps = loadLastMacro()
            if (steps.isEmpty()) {
                Toast.makeText(this, "저장된 매크로가 없어", Toast.LENGTH_SHORT).show()
            } else {
                moveTaskToBack(true)
                MacroAccessibilityService.startPlayback(steps)
            }
        }

        listLayout = LinearLayout(this)
        listLayout.orientation = LinearLayout.VERTICAL
        root.addView(listLayout)

        setContentView(root)

        MacroAccessibilityService.onStopListener = {
            runOnUiThread {
                saveMacro()
                refreshList()
            }
        }
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun saveMacro() {
        val steps = MacroAccessibilityService.recordedSteps.toList()
        if (steps.isEmpty()) return
        val prefs = getSharedPreferences("macros", Context.MODE_PRIVATE)
        val old = prefs.getString("list", "") ?: ""
        val one = steps.joinToString(";") { "${it.x},${it.y}" }
        prefs.edit().putString("list", if (old.isEmpty()) one else "$old\n$one").apply()
        Toast.makeText(this, "${steps.size}개 저장됨", Toast.LENGTH_SHORT).show()
    }

    private fun loadLastMacro(): List<MacroStep.Touch> {
        val prefs = getSharedPreferences("macros", Context.MODE_PRIVATE)
        val all = prefs.getString("list", "") ?: ""
        if (all.isEmpty()) return emptyList()
        val last = all.lines().last { it.isNotBlank() }
        return last.split(";").mapNotNull {
            val p = it.split(",")
            if (p.size == 2) MacroStep.Touch(p[0].toIntOrNull() ?: return@mapNotNull null, p[1].toIntOrNull() ?: return@mapNotNull null) else null
        }
    }

    private fun loadAll(): List<List<MacroStep.Touch>> {
        val prefs = getSharedPreferences("macros", Context.MODE_PRIVATE)
        val all = prefs.getString("list", "") ?: ""
        if (all.isEmpty()) return emptyList()
        return all.lines().filter { it.isNotBlank() }.map { line ->
            line.split(";").mapNotNull {
                val p = it.split(",")
                if (p.size == 2) MacroStep.Touch(p[0].toIntOrNull() ?: return@mapNotNull null, p[1].toIntOrNull() ?: return@mapNotNull null) else null
            }
        }
    }

    private fun refreshList() {
        listLayout.removeAllViews()
        val all = loadAll()
        all.forEachIndexed { index, steps ->
            val tv = TextView(this)
            tv.text = "매크로 ${index + 1} - ${steps.size}번 터치"
            tv.textSize = 16f
            tv.setPadding(0, 16, 0, 16)
            tv.setOnClickListener {
                moveTaskToBack(true)
                MacroAccessibilityService.startPlayback(steps)
            }
            listLayout.addView(tv)
        }
        if (all.isEmpty()) {
            val tv = TextView(this)
            tv.text = "저장된 매크로 없음"
            listLayout.addView(tv)
        }
    }
}
