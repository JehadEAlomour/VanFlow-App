package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.datastore.SettingsKeys
import com.jehadalomour.flowvan.core.model.print.PrintTemplatesResponse
import com.jehadalomour.flowvan.core.model.print.Template
import com.jehadalomour.flowvan.core.network.api.PrintTemplateApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Offline cache of the dashboard-designed print templates.
 *
 * The whole `resolve-all` payload is kept as one JSON string in the settings store, so the
 * receipt prints from the company's layout with no signal. [refresh] is best effort by
 * contract — it runs after login and when a print screen opens, and a failure (or a slow
 * link) leaves the previous cache in place. Nothing cached → [templateFor] is null and the
 * app prints its own built-in receipt, exactly as before templates existed.
 */
class PrintTemplateRepository(
    private val api: PrintTemplateApi,
    private val settings: Settings,
    private val json: Json,
) {
    private val log = Logger.withTag("PrintTemplates")

    /** Decoded once per process; re-read when [refresh] stores a new payload. */
    private var cached: PrintTemplatesResponse? = null
    private var cachedRaw: String? = null

    /**
     * The template for a voucher kind (SALE, RETURN, ORDER, …) from the cache, or null when
     * nothing is cached, the cache does not parse, or the kind has no template.
     */
    suspend fun templateFor(kind: String): Template? = load()?.templates?.get(kind)

    /** The cached `version` stamp, or null with no cache. */
    fun cachedVersion(): String? = load()?.version

    /**
     * Pull the templates and replace the cache. Bounded by [timeoutMs] so a print screen
     * opening on a bad link is never held up; every failure is swallowed and logged.
     * Returns true when the cache was updated.
     */
    suspend fun refresh(storeNumber: String? = null, timeoutMs: Long = REFRESH_TIMEOUT_MS): Boolean {
        val response = runCatching {
            withTimeoutOrNull(timeoutMs) { api.resolveAll(storeNumber) }
        }.onFailure { log.w { "templates refresh failed: ${it.message}" } }
            .getOrNull()
        if (response == null) {
            log.w { "templates refresh skipped (timeout or failure); keeping cache" }
            return false
        }
        val raw = json.encodeToString(PrintTemplatesResponse.serializer(), response)
        settings.putString(SettingsKeys.PRINT_TEMPLATES_JSON, raw)
        cached = response
        cachedRaw = raw
        log.d { "templates cached: ${response.templates.keys} v=${response.version}" }
        return true
    }

    private fun load(): PrintTemplatesResponse? {
        val raw = settings.getStringOrNull(SettingsKeys.PRINT_TEMPLATES_JSON) ?: return null
        if (raw == cachedRaw) return cached
        val decoded = runCatching {
            json.decodeFromString(PrintTemplatesResponse.serializer(), raw)
        }.onFailure { log.w { "cached templates do not parse: ${it.message}" } }
            .getOrNull()
        cached = decoded
        cachedRaw = raw
        return decoded
    }

    companion object {
        const val REFRESH_TIMEOUT_MS = 5_000L
    }
}
