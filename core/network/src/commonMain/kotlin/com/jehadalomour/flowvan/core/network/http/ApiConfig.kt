package com.jehadalomour.flowvan.core.network.http

import com.jehadalomour.flowvan.core.datastore.SettingsKeys
import com.russhwolf.settings.Settings

/**
 * Base URL of the VanFlow backend. You can store either the bare host
 * ("https://host") or the full API root ("https://host/api/v1") — [resolvedBaseUrl]
 * appends the "/api/v1" prefix when it is missing, since every endpoint lives under it.
 * When blank the network layer is disabled and the app stays in pure offline/demo mode.
 *
 * [defaultBaseUrl] comes from the build variant (see AppBuildConfig). It used to be
 * a `const val` here with the other customers' URLs commented out above it, which
 * made "which customer is this APK for?" a question about editor history rather
 * than about the build.
 */
class ApiConfig(
    private val settings: Settings,
    private val defaultBaseUrl: String,
) {

    /**
     * Raw value as entered/saved (what the Settings field shows). Reads back the
     * value saved from the Settings page so a change to the backend URL takes effect
     * directly (resolved per request — no app restart). Falls back to the variant's
     * [defaultBaseUrl] when nothing has been saved yet.
     */
    var baseUrl: String
        get() = settings.getStringOrNull(SettingsKeys.API_BASE_URL)?.takeIf { it.isNotBlank() }
            ?: defaultBaseUrl
        set(value) = settings.putString(SettingsKeys.API_BASE_URL, value.trim())

    val isEnabled: Boolean get() = baseUrl.isNotBlank()

    /** [baseUrl] guaranteed to end in the "/api/v1" prefix the backend serves under. */
    val resolvedBaseUrl: String
        get() {
            val trimmed = baseUrl.trim().trimEnd('/')
            return if (trimmed.contains("/api/")) trimmed else "$trimmed/$API_PREFIX"
        }

    fun urlFor(path: String): String =
        resolvedBaseUrl + "/" + path.trimStart('/')

    companion object {
        const val API_PREFIX = "api/v1"
    }
}
