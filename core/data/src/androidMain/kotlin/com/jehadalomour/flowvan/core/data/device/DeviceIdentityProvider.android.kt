package com.jehadalomour.flowvan.core.data.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

actual class DeviceIdentityProvider(private val context: Context) {

    /**
     * `ANDROID_ID` is stable for the life of the install *and* survives the app
     * being uninstalled and reinstalled (it is keyed to the signing key and the
     * device user), which is exactly the property binding needs — a rep cannot
     * shake off their handset binding by clearing app data.
     *
     * It can be null on a badly-behaved ROM; the fallback is the hardware
     * description, which is not unique but is stable, and a false "this device
     * is taken" is safer than silently binding nothing.
     */
    @SuppressLint("HardwareIds")
    actual fun identity(): DeviceIdentity {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        return DeviceIdentity(
            deviceId = androidId?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
                ?: "fallback-${Build.MANUFACTURER}-${Build.MODEL}-${Build.ID}",
            platform = "android",
            model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        )
    }
}
