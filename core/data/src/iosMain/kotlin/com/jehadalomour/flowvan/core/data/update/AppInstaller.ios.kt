package com.jehadalomour.flowvan.core.data.update

import platform.Foundation.NSBundle

/**
 * iOS cannot replace its own binary — there is no API for it and no entitlement
 * that grants one. So this reports the running version honestly and refuses the
 * install, which lets the shared update check still run and still say "you are
 * behind"; only the button that would fix it is missing. Updating an iOS build
 * is the App Store's job.
 */
actual class AppInstaller {

    private val info: Map<Any?, *>? get() = NSBundle.mainBundle.infoDictionary

    /** CFBundleVersion is the iOS build number — the same role as Android's versionCode. */
    actual val installedVersionCode: Long
        get() = (info?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L

    actual val installedVersionName: String
        get() = (info?.get("CFBundleShortVersionString") as? String).orEmpty()

    actual val canInstallSilently: Boolean get() = false

    actual suspend fun downloadAndInstall(
        url: String,
        sha256: String,
        expectedSize: Long,
        onProgress: (InstallProgress) -> Unit,
    ): InstallResult = InstallResult.Failed("unsupported-on-ios")
}
