package com.jehadalomour.flowvan.core.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import co.touchlab.kermit.Logger

/**
 * Where the OS reports what became of an install session.
 *
 * On a provisioned (device-owner) handset this is almost ceremonial: the session
 * succeeds and the process is killed before the broadcast is worth reading. It
 * earns its place on the handsets that are NOT provisioned, where the OS answers
 * STATUS_PENDING_USER_ACTION and hands back an intent that — and only that —
 * opens the confirm dialog. Without this receiver those devices commit a session
 * that then sits waiting forever with nothing on screen.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    private val log = Logger.withTag("AppUpdate")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                // NEW_TASK because a broadcast receiver has no task of its own to
                // put an activity into; without it Android refuses to start it and
                // the update stalls with no dialog and no error.
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
                    .onFailure { log.e(it) { "could not show the install confirmation" } }
            }

            PackageInstaller.STATUS_SUCCESS ->
                log.i { "update installed" }

            else ->
                log.e {
                    "install failed: status=$status " +
                        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.jehadalomour.flowvan.INSTALL_STATUS"
    }
}
