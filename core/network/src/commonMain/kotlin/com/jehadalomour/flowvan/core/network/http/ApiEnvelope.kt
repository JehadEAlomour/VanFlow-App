package com.jehadalomour.flowvan.core.network.http

import kotlinx.serialization.Serializable

/** Success envelope wrapping every 2xx JSON response: `{ success, data, timestamp }`. */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T,
    val timestamp: String? = null,
)

/** offset/limit list payload: `data: { items, total }`. */
@Serializable
data class OffsetPage<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
)

/** page/limit list payload: `data: { items, total, page, limit, pages }`. */
@Serializable
data class KeysetPage<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 25,
    val pages: Int = 0,
)

/** Non-2xx error envelope. */
@Serializable
data class ApiErrorEnvelope(
    val statusCode: Int = 0,
    val message: String = "",
    val error: String = "",
    /**
     * Machine-readable reason, when the server sends one. Branch on this rather
     * than on [message]: a sign-in refused because the handset belongs to
     * someone else needs a different screen from a wrong password, and the
     * sentence alone cannot be told apart once it is translated.
     */
    val code: String? = null,
    val path: String? = null,
    val timestamp: String? = null,
)
