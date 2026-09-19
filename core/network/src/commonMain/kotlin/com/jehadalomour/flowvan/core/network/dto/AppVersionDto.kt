package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * What the server says the newest build is, from `GET /app-version?platform=android`.
 *
 * Served to a handset that may be running ANY past build, including one written
 * before a field here existed — so every field carries a default and nothing is
 * required. A response the old app cannot parse is a response that stops it
 * updating, which is precisely the failure the updater exists to prevent.
 */
@Serializable
data class AppVersionDto(
    /** The newest build's versionCode. The only number the updater compares. */
    val versionCode: Long = 0,

    /** Shown to a human ("1.4.2"). Never compared. */
    val versionName: String = "",

    /** Absolute URL of the APK. Absolute, not a path: the file is usually not on the API host. */
    val apkUrl: String = "",

    /**
     * Lowercase hex SHA-256 of the APK bytes.
     *
     * Checked before the file is handed to the installer. Android would refuse a
     * differently-signed APK anyway, so this is not the only line of defence —
     * but it also catches the ordinary case this fleet will actually hit: a
     * download truncated when the van drove out of coverage. Installing a
     * half-file fails in a far more confusing way than not installing at all.
     */
    val sha256: String = "",

    /** Bytes, for the progress bar. 0 when the server does not say. */
    val sizeBytes: Long = 0,

    /**
     * The oldest build still allowed to run. Below this the app is walled off
     * until it has updated.
     *
     * Separate from [versionCode] on purpose: most releases should NOT wall
     * anyone off mid-round, so this stays where it was and only moves for a
     * build that genuinely must not be skipped (a pricing fix, a tax change, a
     * server contract that has already changed underneath the old app).
     */
    val minVersionCode: Long = 0,

    /** Release notes, in the salesman's language. Optional. */
    val notes: String = "",
)
