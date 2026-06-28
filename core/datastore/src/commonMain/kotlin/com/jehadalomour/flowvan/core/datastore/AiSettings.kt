package com.jehadalomour.flowvan.core.datastore

import com.russhwolf.settings.Settings

class AiSettings(private val settings: Settings) {

    var apiKey: String
        get() = settings.getString(SettingsKeys.AI_API_KEY, "")
        set(value) = settings.putString(SettingsKeys.AI_API_KEY, value)

    val isConfigured: Boolean get() = apiKey.isNotBlank()
}
