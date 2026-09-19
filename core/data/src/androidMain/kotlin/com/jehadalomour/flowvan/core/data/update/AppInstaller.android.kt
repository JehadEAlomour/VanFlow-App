package com.jehadalomour.flowvan.core.data.update

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

actual class AppInstaller(private val context: Context) {

    private val log = Logger.withTag("AppUpdate")

    private val packageInfo
        get() = context.packageManager.getPackageInfo(context.packageName, 0)

    actual val installedVersionCode: Long
        get() = runCatching {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        }.getOrDefault(0L)

    actual val installedVersionName: String
        get() = runCatching { packageInfo.versionName.orEmpty() }.getOrDefault("")

    /**
     * Device owner is the whole trick.
     *
     * A device-owner app may commit a PackageInstaller session with no dialog at
     * all — which is what makes this fleet updatable without asking a salesman
     * to understand what an APK is. It is granted once, over ADB, on a handset
     * with no accounts on it yet, and cannot be granted afterwards; see
     * docs/auto-update.md.
     *
     * Read every time rather than cached: a handset can lose device-owner status
     * (it is cleared with the work profile, and by some OEM factory tools), and
     * a cached `true` would mean the app silently stops offering the dialog it
     * now needs and simply never updates again.
     */
    actual val canInstallSilently: Boolean
        get() = runCatching {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isDeviceOwnerApp(context.packageName)
        }.getOrDefault(false)

    actual suspend fun downloadAndInstall(
        url: String,
        sha256: String,
        expectedSize: Long,
        onProgress: (InstallProgress) -> Unit,
    ): InstallResult = withContext(Dispatchers.IO) {
        onProgress(InstallProgress.Preparing)

        // Named for the package rather than the version so a retry overwrites the
        // last attempt instead of adding to it. A van that loses signal three
        // times on one update would otherwise leave three part-files behind.
        val apk = File(context.cacheDir, "flowvan-update.apk")
        try {
            val digest = download(url, apk, expectedSize, onProgress)
                ?: return@withContext InstallResult.Failed(REASON_DOWNLOAD)

            onProgress(InstallProgress.Verifying)
            if (sha256.isNotBlank() && !digest.equals(sha256.trim(), ignoreCase = true)) {
                log.e { "checksum mismatch: expected $sha256, got $digest" }
                return@withContext InstallResult.Failed(REASON_CHECKSUM)
            }

            onProgress(InstallProgress.Installing)
            commit(apk)
        } catch (e: Exception) {
            log.e(e) { "update failed" }
            InstallResult.Failed(e.message ?: REASON_DOWNLOAD)
        } finally {
            // Always, including the success path. The bytes were copied into the
            // installer session by commit(), so nothing downstream still reads
            // this file — and a kept APK is dead weight on an 8 GB handset, while
            // a kept APK that FAILED verification is a file that will eventually
            // be installed by mistake.
            runCatching { apk.delete() }
        }
    }

    /**
     * Streams the APK to [target], hashing as it goes, and returns the hex digest.
     * Null means the transfer did not complete.
     *
     * Hashing DURING the copy rather than re-reading the file afterwards: these
     * handsets have slow flash and an APK is tens of megabytes, so a second full
     * read is a visible pause on a screen whose whole job is to not feel stuck.
     */
    private fun download(
        url: String,
        target: File,
        expectedSize: Long,
        onProgress: (InstallProgress) -> Unit,
    ): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) {
                log.e { "download HTTP ${connection.responseCode} for $url" }
                return null
            }
            // The server's own Content-Length beats the number from the version
            // manifest when both are present — a CDN that re-compresses or ranges
            // the file knows the truth about what is arriving here.
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize

            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var lastReported = 0L

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        // Throttled: a progress callback per 8 KB chunk recomposes
                        // the screen thousands of times a second and makes the
                        // download measurably slower than the radio.
                        if (downloaded - lastReported >= PROGRESS_STEP_BYTES) {
                            lastReported = downloaded
                            onProgress(InstallProgress.Downloading(downloaded, total))
                        }
                    }
                }
            }
            onProgress(InstallProgress.Downloading(downloaded, total))

            // A stream that ends early looks exactly like a stream that ended —
            // no exception, just fewer bytes. This is the common failure out here
            // (the van drives behind a building), so it is checked explicitly
            // rather than left to the checksum, which cannot say WHY it failed.
            if (total > 0 && downloaded < total) {
                log.e { "truncated download: $downloaded of $total bytes" }
                return null
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Hands the APK to the OS through a PackageInstaller session.
     *
     * A session rather than the old ACTION_INSTALL_PACKAGE intent because the
     * intent has no silent mode at all — it always shows the dialog, even to a
     * device owner — and reports nothing back about what happened. The session
     * gives us both.
     */
    private fun commit(apk: File): InstallResult {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Asks for no dialog. Honoured for a device owner; ignored (and the
                // dialog shown) for anyone else, which is the fallback we want
                // rather than an error.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite(WRITE_NAME, 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }

            val intent = Intent(UpdateInstallReceiver.ACTION_INSTALL_STATUS)
                .setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                // Mutable because the OS writes the install status into this intent
                // before delivering it. FLAG_IMMUTABLE here means the receiver is
                // handed an empty intent and the confirm dialog never appears.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)

            session.commit(pending.intentSender)
        }

        log.i { "install session $sessionId committed (silent=$canInstallSilently)" }
        return if (canInstallSilently) InstallResult.Started else InstallResult.AwaitingUser
    }

    private companion object {
        const val WRITE_NAME = "flowvan-update"
        const val BUFFER_BYTES = 64 * 1024
        const val PROGRESS_STEP_BYTES = 256 * 1024L
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
        const val REASON_DOWNLOAD = "download-failed"
        const val REASON_CHECKSUM = "checksum-mismatch"
    }
}
