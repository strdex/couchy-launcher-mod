package com.conreo.couchybridge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("CouchyBridge", "Boot completed received, launching Couchy TV...")
            Handler(Looper.getMainLooper()).postDelayed({
                val svc = CouchyBridgeService.instance
                if (svc != null) {
                    svc.launchCouchyHome()
                } else {
                    val launch = context.packageManager
                        .getLaunchIntentForPackage(CouchyBridgeService.TARGET_PACKAGE)
                        ?: Intent(Intent.ACTION_MAIN).apply {
                            component = ComponentName(
                                CouchyBridgeService.TARGET_PACKAGE,
                                CouchyBridgeService.TARGET_ACTIVITY
                            )
                        }
                    launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    runCatching { context.startActivity(launch) }
                }
            }, 1500)
        }
    }
}
