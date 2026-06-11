package com.jehadalomour.flowvan.shared.data.settings

import com.russhwolf.settings.Settings

class SyncConfig(private val settings: Settings) {
    var baseUrl: String
        get() = settings.getString(SettingsKeys.SYNC_BASE_URL, "")
        set(value) = settings.putString(SettingsKeys.SYNC_BASE_URL, value)

    val isEnabled: Boolean get() = baseUrl.isNotBlank()
}
