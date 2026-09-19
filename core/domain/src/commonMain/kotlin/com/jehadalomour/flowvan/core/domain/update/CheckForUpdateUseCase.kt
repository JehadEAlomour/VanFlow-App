package com.jehadalomour.flowvan.core.domain.update

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.update.AppInstaller
import com.jehadalomour.flowvan.core.network.api.AppVersionApi

/** The build the server is offering. */
data class AvailableUpdate(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val notes: String,
)

sealed interface UpdateStatus {
    /** Running the newest build, or the server did not answer. Either way: carry on. */
    data object None : UpdateStatus

    /** Newer build exists; the salesman may keep working. */
    data class Optional(val update: AvailableUpdate) : UpdateStatus

    /** This build is below the floor the office set. Nothing else may run. */
    data class Required(val update: AvailableUpdate) : UpdateStatus
}

/**
 * Asks the server what the newest build is and decides whether this one may keep running.
 *
 * FAILS OPEN, ALWAYS. Every error path here — no network, no base URL configured,
 * a 500, a response shape this build predates — returns [UpdateStatus.None] and
 * lets the app through. That asymmetry is deliberate and it is the whole safety
 * design of this feature: the wall this use case can raise is unconditional and
 * has no way past it, so a bug that raises it wrongly does not inconvenience a
 * salesman, it ends their working day in a van somewhere with no way to sell,
 * return or collect. A salesman who wrongly keeps running an old build for
 * another hour costs nothing by comparison.
 *
 * The same reasoning is why the floor is [AppVersionDto.minVersionCode] and not
 * "newest build", and why a zero or absent floor blocks nobody: a server that
 * has forgotten the field, or is answering with defaults, must not be able to
 * wall off the fleet by accident.
 */
class CheckForUpdateUseCase(
    private val api: AppVersionApi,
    private val installer: AppInstaller,
) {
    private val log = Logger.withTag("AppUpdate")

    suspend operator fun invoke(): UpdateStatus {
        val installed = installer.installedVersionCode
        // 0 means the package could not be read at all. Comparing against it would
        // make every server answer look newer and wall off a handset over what is
        // really a failure to read our OWN version.
        if (installed <= 0L) {
            log.w { "cannot read the installed versionCode — skipping the update check" }
            return UpdateStatus.None
        }

        val latest = runCatching { api.latest() }
            .onFailure { log.w { "update check failed: ${it.message}" } }
            .getOrNull()
            ?: return UpdateStatus.None

        if (latest.versionCode <= installed) return UpdateStatus.None

        // An announced build with nowhere to download it from is not an update.
        // Saying so out loud matters: with a mandatory floor set, taking this as
        // a real update would wall the fleet off behind a button that cannot work.
        if (latest.apkUrl.isBlank()) {
            log.e { "server offers versionCode ${latest.versionCode} with no apkUrl" }
            return UpdateStatus.None
        }

        val update = AvailableUpdate(
            versionCode = latest.versionCode,
            versionName = latest.versionName,
            apkUrl = latest.apkUrl,
            sha256 = latest.sha256,
            sizeBytes = latest.sizeBytes,
            notes = latest.notes,
        )

        return if (latest.minVersionCode > installed) {
            UpdateStatus.Required(update)
        } else {
            UpdateStatus.Optional(update)
        }
    }
}
