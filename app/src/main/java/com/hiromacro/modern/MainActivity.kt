package com.hiromacro.modern

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.app.Activity

class MainActivity : Activity() {

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
            text = "HiroMacro Modern"
            textSize = 26f
        }
        root.addView(title)

        val btnAccess = Button(this).apply { text = "1. 접근성 권한 켜기" }
        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        root.addView(btnAccess)

        val btnOverlay = Button(this).apply { text = "2. 다른 앱 위에 표시 허용" }
        btnOverlay.setOnClickListener {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(i)
        }
        root.addView(btnOverlay)

        val btnNew = Button(this).apply { text = "+ 새 매크로 만들기" }
        btnNew.setOnClickListener {
            MacroStore.current = Macro("매크로 ${MacroStore.list.size + 1}", mutableListOf())
            startActivity(Intent(this, MacroEditorActivity::class.java))
        }
        root.addView(btnNew)

        val stop = Button(this).apply { text = "■ 중지" }
        stop.setOnClickListener { MacroAccessibilityService.stopMacro() }
        root.addView(stop)

        listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listLayout)
    }

    override fun onResume() {
        super.onResume()
        listLayout.removeAllViews()
        if (MacroStore.list.isEmpty()) {
            val tv = TextView(this).apply { text = "아직 매크로가 없어.\n+ 새 매크로 만들기로 시작해." }
            listLayout.addView(tv)
        }
        MacroStore.list.forEach { m ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val tv = TextView(this).apply { text = "${m.name} - ${m.steps.size}줄"; textSize = 18f }
            val edit = Button(this).apply { text = "편집" }
            edit.setOnClickListener {
                MacroStore.current = m
                startActivity(Intent(this, MacroEditorActivity::class.java))
            }
            val run = Button(this).apply { text = "▶ 실행" }
            run.setOnClickListener {
                MacroAccessibilityService.startMacro(m)
                Toast.makeText(this, "실행 시작: ${m.name}", Toast.LENGTH_SHORT).show()
            }
            row.addView(tv); row.addView(edit); row.addView(run)
            listLayout.addView(row)
        }
    }
}
