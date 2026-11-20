package com.hiromacro.modern

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var logView: TextView
    private var picker: ColorPickerOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val title = TextView(this).apply {
            text = "HiroMacro Modern\n아쿠아리움 매크로"
            textSize = 24f
        }
        logView = TextView(this).apply {
            text = "로그 대기중..."
            textSize = 14f
        }
        val btnAccess = Button(this).apply { text = "1. 접근성 권한 켜기" }
        val btnOverlay = Button(this).apply { text = "2. 다른 앱 위에 표시 허용" }
        val btnStart = Button(this).apply { text = "3. 아쿠아리움 매크로 시작" }
        val btnStop = Button(this).apply { text = "중지" }
        val btnPicker = Button(this).apply { text = "스포이드로 색상 뽑기" }

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else Toast.makeText(this, "이미 허용됨", Toast.LENGTH_SHORT).show()
        }
        btnStart.setOnClickListener {
            val svc = MacroAccessibilityService.instance
            if (svc == null) {
                Toast.makeText(this, "접근성 서비스를 먼저 켜세요", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            MacroEngine.onLog = { msg -> runOnUiThread { logView.append("\n$msg") } }
            svc.startAqua()
            Toast.makeText(this, "매크로 시작!", Toast.LENGTH_SHORT).show()
        }
        btnStop.setOnClickListener {
            MacroEngine.stop()
            Toast.makeText(this, "중지됨", Toast.LENGTH_SHORT).show()
        }
        btnPicker.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "오버레이 권한 먼저 허용하세요", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            picker = ColorPickerOverlay(this)
            picker?.onColorPicked = { x, y, color ->
                val hex = String.format("#%06X", 0xFFFFFF and color)
                runOnUiThread { logView.append("\nPicked $x,$y $hex") }
            }
            picker?.show()
        }

        layout.addView(title)
        layout.addView(btnAccess)
        layout.addView(btnOverlay)
        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(btnPicker)
        layout.addView(logView)
        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
    }
}
