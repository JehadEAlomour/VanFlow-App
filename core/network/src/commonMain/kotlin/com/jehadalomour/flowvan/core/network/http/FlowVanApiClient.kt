package com.jehadalomour.flowvan.core.network.http

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** Raised by the network layer; carries a translated [CashFlowError]. */
class NetworkException(val error: CashFlowError) : Exception(error.messageEn)

/**
 * Thin wrapper over Ktor that knows the VanFlow base URL, attaches the bearer token,
 * unwraps the success envelope and maps HTTP errors to [CashFlowError.Network].
 *
 * Logs every request and the full response (status + body) to logcat under the `KtorHTTP`
 * tag. Members are public (not the impl) so the reified `get/post/...` extensions can inline.
 */
class FlowVanApiClient(
    val httpClient: HttpClient,
    val config: ApiConfig,
    val session: SessionStore,
    val json: Json,
) {
    val log = Logger.withTag("KtorHTTP")


    /** Performs the call, logs request + response, and returns the raw response body text. */
    suspend fun execute(
        method: HttpMethod,
        path: String,
        query: Map<String, String?> = emptyMap(),
        bodyJson: String? = null,
    ): String {
        if (!config.isEnabled) throw NetworkException(CashFlowError.Network.NotConfigured)
        val url = config.urlFor(path)
        val queryStr = query.filterValues { it != null }.entries.joinToString("&") { "${it.key}=${it.value}" }
        log.d { "→ ${method.value} $url${if (queryStr.isNotEmpty()) "?$queryStr" else ""}" }
        if (bodyJson != null) log.d { "→ body: $bodyJson" }

        val response = try {
            httpClient.request(url) {
                this.method = method
                session.currentToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                query.forEach { (k, v) -> if (v != null) parameter(k, v) }
                if (bodyJson != null) setBody(TextContent(bodyJson, ContentType.Application.Json))
            }
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            log.e { "✗ ${method.value} $url — ${e.message}" }
            throw NetworkException(CashFlowError.Network.Unreachable)
        }

        val text = response.bodyAsText()
        log.d { "← ${response.status.value} ${method.value} $path\n$text" }

        if (response.status.value == 401) session.signalUnauthorized()
        if (!response.status.isSuccess()) throw mapError(response.status.value, text)
        return text
    }

    private fun mapError(statusCode: Int, body: String): NetworkException {
        val detail = runCatching {
            json.decodeFromString(ApiErrorEnvelope.serializer(), body).message
        }.getOrNull().orEmpty()
        val error = when (statusCode) {
            401 -> CashFlowError.Network.Unauthorized
            403 ->
                if (detail.startsWith("APPROVAL_REQUIRED")) {
                    CashFlowError.Network.ApprovalRequired(detail.substringAfter(':', "UNKNOWN"))
                } else {
                    CashFlowError.Network.Forbidden
                }
            404 -> CashFlowError.Network.NotFound
            409 -> CashFlowError.Network.Conflict
            400, 422 -> CashFlowError.Network.Validation(detail.ifBlank { "validation failed" })
            in 500..599 -> CashFlowError.Network.Server
            else -> CashFlowError.Unknown
        }
        return NetworkException(error)
    }
}

/** GET → unwrap `data` of type [T]. */
suspend inline fun <reified T> FlowVanApiClient.getData(
    path: String,
    query: Map<String, String?> = emptyMap(),
): T = json.decodeFromString<ApiEnvelope<T>>(execute(HttpMethod.Get, path, query)).data

/** POST a serializable [body] → unwrap `data` of type [T]. */
suspend inline fun <reified B, reified T> FlowVanApiClient.postData(
    path: String,
    body: B,
): T = json.decodeFromString<ApiEnvelope<T>>(
    execute(HttpMethod.Post, path, bodyJson = json.encodeToString(body)),
).data

/** POST with no body → unwrap `data` of type [T]. */
suspend inline fun <reified T> FlowVanApiClient.postEmpty(
    path: String,
): T = json.decodeFromString<ApiEnvelope<T>>(execute(HttpMethod.Post, path)).data

/** PATCH a serializable [body] → unwrap `data` of type [T]. */
suspend inline fun <reified B, reified T> FlowVanApiClient.patchData(
    path: String,
    body: B,
): T = json.decodeFromString<ApiEnvelope<T>>(
    execute(HttpMethod.Patch, path, bodyJson = json.encodeToString(body)),
).data

/** DELETE; ignores the (usually empty 204) body. */
suspend fun FlowVanApiClient.deleteUnit(path: String) {
    execute(HttpMethod.Delete, path)
}
