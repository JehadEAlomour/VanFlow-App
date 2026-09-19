package com.jehadalomour.flowvan.core.data.update

/** How far along a download+install is, for the screen the salesman is looking at. */
sealed interface InstallProgress {
    data object Preparing : InstallProgress

    /** [downloaded] and [total] in bytes; [total] is 0 when the server did not say how big it is. */
    data class Downloading(val downloaded: Long, val total: Long) : InstallProgress {
        /** 0f..1f, or null when the size is unknown and the bar must be indeterminate. */
        val fraction: Float? get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
    }

    data object Verifying : InstallProgress

    /** Handed to the OS. On a provisioned handset the app is killed from here. */
    data object Installing : InstallProgress
}

/** What came back. Only [Failed] ever reaches the salesman as words. */
sealed interface InstallResult {
    /**
     * The OS accepted the APK. Nothing follows this on Android: the process is
     * killed as it is replaced, so no code of ours runs afterwards.
     */
    data object Started : InstallResult

    /** The OS is showing its own confirm dialog. See [AppInstaller.canInstallSilently]. */
    data object AwaitingUser : InstallResult

    data class Failed(val reason: String) : InstallResult
}

/**
 * Downloads a build and installs it over this one.
 *
 * WHY THIS IS ONE CALL rather than a download step and an install step. The
 * downloaded file is a half-gigabyte-adjacent artefact that must never outlive
 * the attempt: leaving APKs in the cache for a later step to pick up fills the
 * 8 GB handsets these vans carry, and a file that survives a failed verify is a
 * file that will eventually be installed by mistake. So the bytes are fetched,
 * checked and handed over inside one call, and deleted on every exit from it.
 *
 * ANDROID DOES THE WORK; iOS CANNOT. Apple has no route for an app to replace
 * itself, so the iOS implementation refuses. That is not a gap to fill later —
 * the fleet this exists for is Android handhelds, and the iOS build reaches the
 * App Store, which does its own updating.
 */
expect class AppInstaller {

    /** versionCode of the build currently running. What the server's number is compared against. */
    val installedVersionCode: Long

    /** versionName of the build currently running, for display. */
    val installedVersionName: String

    /**
     * True when this handset can be updated with NO dialog and no tap.
     *
     * On Android that means the app is the device owner — provisioned once,
     * before the handset was handed to anyone (see docs/auto-update.md). False
     * is not a failure: the update still installs, the salesman just has to
     * confirm it once. The screen wording changes accordingly, because telling
     * someone to "wait" in front of a dialog that is waiting for THEM is how a
     * van sits still for an hour.
     */
    val canInstallSilently: Boolean

    /**
     * Fetch [url], check it against [sha256], install it.
     *
     * [onProgress] is called from the IO thread doing the transfer, NOT the main
     * thread — writing Compose snapshot state from it is safe, touching anything
     * that is not is not.
     */
    suspend fun downloadAndInstall(
        url: String,
        sha256: String,
        expectedSize: Long,
        onProgress: (InstallProgress) -> Unit,
    ): InstallResult
}
