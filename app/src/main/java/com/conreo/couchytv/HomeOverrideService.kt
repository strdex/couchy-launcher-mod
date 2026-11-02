package com.conreo.couchytv

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service to override Home button behavior on certified Android TV, Google TV,
 * and AOSP/Wisdom Share/CVTE/MStar Chinese TV boards (Dijitsu, Onvo, etc.).
 * Detects stock launcher window state changes or key events and redirects to Couchy TV immediately.
 */
class HomeOverrideService : AccessibilityService() {

    private var stockLaunchers: Set<String> = emptySet()
    private var lastLaunchTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateStockLaunchers()
    }

    private fun updateStockLaunchers() {
        runCatching {
            val pm = packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

            val detectedFromPm = (pm.queryIntentActivities(homeIntent, 0) +
                    pm.queryIntentActivities(leanbackIntent, 0))
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != packageName }
                .toSet()

            stockLaunchers = detectedFromPm + KNOWN_STOCK_LAUNCHERS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == packageName) return
            if (stockLaunchers.isEmpty()) updateStockLaunchers()

            if (isStockLauncher(pkg)) {
                launchCouchyHome()
            }
        }
    }

    private fun isStockLauncher(pkg: String): Boolean {
        if (pkg == packageName) return false
        if (stockLaunchers.contains(pkg)) return true

        // Ignore system dialogs, settings, input methods, and keyboards
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
            keyCode == 170 || keyCode == 220 // Common vendor home/smart keycodes
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                launchCouchyHome()
            }
            return true // Consume so stock launcher is intercepted
        }
        return super.onKeyEvent(event)
    }

    private fun launchCouchyHome() {
        val now = SystemClock.uptimeMillis()
        if (now - lastLaunchTime < 350) return // Debounce rapid window changes
        lastLaunchTime = now

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    companion object {
        val KNOWN_STOCK_LAUNCHERS = setOf(
            // Certified Android TV / Google TV
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher",
            "com.amazon.tv.launcher",
            "com.xiaomi.mitv.tvhome",
            "com.tcl.waterfall.overseas",

            // Wisdom Share / CVTE / Dijitsu / Onvo / Amlogic / MStar / AOSP
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
