package com.conreo.couchybridge

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class CouchyBridgeService : AccessibilityService() {

    private var stockLaunchers: Set<String> = emptySet()
    private var lastLaunchTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateStockLaunchers()
        instance = this
        Log.i(TAG, "CouchyBridgeService connected and listening.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    fun updateStockLaunchers() {
        runCatching {
            val pm = packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

            val detected = (pm.queryIntentActivities(homeIntent, 0) +
                    pm.queryIntentActivities(leanbackIntent, 0))
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != TARGET_PACKAGE && it != packageName }
                .toSet()

            stockLaunchers = detected + KNOWN_STOCK_LAUNCHERS
            Log.d(TAG, "Stock launchers identified: $stockLaunchers")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == packageName || pkg == TARGET_PACKAGE) return

            lastDetectedPackage = pkg

            if (stockLaunchers.isEmpty()) updateStockLaunchers()

            if (isStockLauncher(pkg)) {
                Log.w(TAG, "Stock/Wisdom launcher detected ($pkg), redirecting to Couchy TV...")
                launchCouchyHome()
            }
        }
    }

    private fun isStockLauncher(pkg: String): Boolean {
        if (pkg == packageName || pkg == TARGET_PACKAGE) return false
        if (stockLaunchers.contains(pkg)) return true

        if (SYSTEM_IGNORE_PACKAGES.any { pkg.startsWith(it) }) return false

        val lower = pkg.lowercase()
        return lower.contains("launcher") ||
                lower.contains("cvte") ||
                lower.contains("wisdom") ||
                lower.contains("cloudtv") ||
                lower.contains("tvhome") ||
                lower.contains("smarttv")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_ALL_APPS ||
            keyCode == KeyEvent.KEYCODE_TV_INPUT ||
            keyCode == 170 || keyCode == 220
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                Log.i(TAG, "Key $keyCode intercepted! Launching Couchy TV...")
                launchCouchyHome()
            }
            return true // Consume so stock launcher is not invoked
        }
        return super.onKeyEvent(event)
    }

    fun launchCouchyHome() {
        val now = SystemClock.uptimeMillis()
        if (now - lastLaunchTime < 300) return
        lastLaunchTime = now

        val intent = packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)
            ?: Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY)
            }

        intent.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $TARGET_PACKAGE", e)
        }
    }

    override fun onInterrupt() {}

    companion object {
        const val TAG = "CouchyBridge"
        const val TARGET_PACKAGE = "com.conreo.couchytv"
        const val TARGET_ACTIVITY = "com.conreo.couchytv.MainActivity"

        var instance: CouchyBridgeService? = null
            private set

        var lastDetectedPackage: String = "Henüz algılanmadı"

        val KNOWN_STOCK_LAUNCHERS = setOf(
            // Certified Android TV / Google TV
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher",
            "com.amazon.tv.launcher",
            "com.xiaomi.mitv.tvhome",
            "com.tcl.waterfall.overseas",

            // Wisdom Share / CVTE / Dijitsu / Onvo / AOSP
            "com.cvte.tv.launcher",
            "com.cvte.tv.launcher.space",
            "com.cvte.tv.launcher.space4",
            "com.cvte.tv.launcher.mars",
            "com.clover.launcher",
            "com.softwinner.launcher",
            "com.mstar.tv.launcher",
            "com.jrm.localmm",
            "com.hisilicon.launcher",
            "com.spocky.projengmenu",
            "com.android.launcher",
            "com.android.launcher3",
            "com.utk.launcher",
            "com.smart.cloudtv.launcher",
            "com.eostek.tv.launcher"
        )

        val SYSTEM_IGNORE_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.tv.settings",
            "com.google.android.inputmethod",
            "com.android.inputmethod",
            "com.google.android.katniss",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )

        fun isServiceRunning(context: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName)
        }
    }
}
