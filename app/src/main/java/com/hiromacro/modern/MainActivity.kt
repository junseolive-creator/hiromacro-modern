package com.hiromacro.modern

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val title = TextView(this).apply { text = "HiroMacro"; textSize = 28f }
        val status = TextView(this).apply { text = "1. 접근성 켜기 -> 2. 컨트롤 표시 -> 3. 다른 앱에서 녹화/재생"; textSize = 16f }

        val btnPermission = Button(this).apply { text = "1. 접근성 권한 켜기" }
        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val btnShow = Button(this).apply { text = "2. 컨트롤 표시" }
        btnShow.setOnClickListener {
            MacroAccessibilityService.showControlOverlay()
            Toast.makeText(this, "다른 앱으로 나가서 떠 있는 버튼을 눌러보세요", Toast.LENGTH_LONG).show()
        }

        val btnRecord = Button(this).apply { text = "3. 녹화 시작" }
        btnRecord.setOnClickListener { MacroAccessibilityService.startRecording() }

        val btnStop = Button(this).apply { text = "4. 녹화 중지 / 저장" }
        btnStop.setOnClickListener { MacroAccessibilityService.stopRecordingAndSave() }

        val btnPlay = Button(this).apply { text = "5. 마지막 매크로 재생 (테스트용)" }
        btnPlay.setOnClickListener { MacroAccessibilityService.startPlayback() }

        layout.addView(title)
        layout.addView(status)
        layout.addView(btnPermission)
        layout.addView(btnShow)
        layout.addView(btnRecord)
        layout.addView(btnStop)
        layout.addView(btnPlay)
        setContentView(layout)
    }
}
