package com.jehadalomour.flowvan.shared.data.settings

import com.russhwolf.settings.Settings

class SessionStore(private val settings: Settings) {

    var currentUserId: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_USER_ID)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_USER_ID)
            else settings.putString(SettingsKeys.CURRENT_USER_ID, value)
        }

    var currentToken: String?
        get() = settings.getStringOrNull(SettingsKeys.CURRENT_TOKEN)
        set(value) {
            if (value == null) settings.remove(SettingsKeys.CURRENT_TOKEN)
            else settings.putString(SettingsKeys.CURRENT_TOKEN, value)
        }

    fun clear() {
        settings.remove(SettingsKeys.CURRENT_USER_ID)
        settings.remove(SettingsKeys.CURRENT_TOKEN)
    }
}