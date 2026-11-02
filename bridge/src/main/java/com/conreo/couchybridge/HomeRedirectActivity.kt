package com.conreo.couchybridge

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

class HomeRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCouchyAndFinish()
    }

    override fun onResume() {
        super.onResume()
        launchCouchyAndFinish()
    }

    private fun launchCouchyAndFinish() {
        val targetPkg = CouchyBridgeService.TARGET_PACKAGE
        val intent = packageManager.getLaunchIntentForPackage(targetPkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(targetPkg, CouchyBridgeService.TARGET_ACTIVITY)
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
        } catch (_: Exception) {
        }

        finish()
    }
}
