package com.conreo.couchybridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLastPkg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLastPkg = findViewById(R.id.tvLastPkg)

        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val btnLaunchCouchy = findViewById<Button>(R.id.btnLaunchCouchy)

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnLaunchCouchy.setOnClickListener {
            CouchyBridgeService.instance?.launchCouchyHome() ?: run {
                val intent = packageManager.getLaunchIntentForPackage(CouchyBridgeService.TARGET_PACKAGE)
                if (intent != null) startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val isRunning = CouchyBridgeService.isServiceRunning(this)
        if (isRunning) {
            tvStatus.text = "Servis Durumu: AKTİF (Home tuşu ve stok ekranlar dinleniyor)"
            tvStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvStatus.text = "Servis Durumu: DEVRE DIŞI (Erişilebilirlik izni gerekli)"
            tvStatus.setTextColor(0xFFFF5252.toInt())
        }

        tvLastPkg.text = "Son Algılanan Sistem/Uygulama Paketi: " + CouchyBridgeService.lastDetectedPackage
    }
}
