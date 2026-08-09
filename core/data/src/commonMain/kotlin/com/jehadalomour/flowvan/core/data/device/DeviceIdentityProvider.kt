package com.jehadalomour.flowvan.core.data.device

/** What the server needs to bind this handset to one salesman. */
data class DeviceIdentity(
    val deviceId: String,
    val platform: String,
    val model: String,
)

/**
 * Identifies this handset for device binding.
 *
 * The id must survive an app reinstall, or the binding is worthless: a rep who
 * wanted a second phone would just reinstall to get a fresh identity. Hence the
 * OS-issued values (ANDROID_ID / identifierForVendor) rather than a UUID this
 * app generates and stores — those are wiped with the app's data.
 */
expect class DeviceIdentityProvider {
    fun identity(): DeviceIdentity
}
