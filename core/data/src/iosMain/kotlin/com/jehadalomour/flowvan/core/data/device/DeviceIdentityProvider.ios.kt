package com.jehadalomour.flowvan.core.data.device

import platform.UIKit.UIDevice

actual class DeviceIdentityProvider {

    /**
     * `identifierForVendor` is the closest iOS equivalent: stable across
     * reinstalls as long as any app from this vendor remains installed, and iOS
     * offers nothing more durable to a normal app. It can be null while the
     * device is locked right after boot, so the model string stands in — a
     * stable-but-shared id fails safe (the office is asked to release), whereas
     * a random one would silently mint a new binding on every launch.
     */
    actual fun identity(): DeviceIdentity {
        val device = UIDevice.currentDevice
        val vendorId = device.identifierForVendor?.UUIDString
        return DeviceIdentity(
            deviceId = vendorId?.takeIf { it.isNotBlank() } ?: "fallback-ios-${device.model}",
            platform = "ios",
            model = device.name,
        )
    }
}
