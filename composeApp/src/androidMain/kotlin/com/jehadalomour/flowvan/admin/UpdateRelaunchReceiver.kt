package com.jehadalomour.flowvan.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.MainActivity

/**
 * Brings the app back after it has replaced itself.
 *
 * Installing an update kills the process mid-screen. On a phone that is fine —
 * the owner taps the icon again. On a handset that is the salesman's only tool,
 * a silent update they never asked for and never saw ends with the app simply
 * gone from in front of them, which is indistinguishable from a crash and gets
 * reported as one.
 *
 * MY_PACKAGE_REPLACED is delivered to the new build right after the swap, so
 * this puts it straight back on screen. Starting an activity from the background
 * is normally refused; a device owner is exempt, which is the same provisioning
 * that made the update silent in the first place. On a handset that is NOT
 * device owner the start is refused and nothing happens — acceptable, because
 * that salesman was standing at a confirm dialog anyway and knows the app just
 * updated.
 */
class UpdateRelaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { context.startActivity(launch) }
            .onFailure { Logger.withTag("AppUpdate").w { "could not relaunch after update: ${it.message}" } }
    }
}
