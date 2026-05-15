package com.jehadalomour.flowvan.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val MODEL = "claude-haiku-4-5-20251001"

class ClaudeApiClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun streamResponse(apiKey: String, systemPrompt: String, userMessage: String): Flow<String> = flow {
        val systemEscaped = systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        val userEscaped = userMessage.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        val body = """{"model":"$MODEL","max_tokens":1024,"stream":true,"system":"$systemEscaped","messages":[{"role":"user","content":"$userEscaped"}]}"""

        httpClient.preparePost(CLAUDE_API_URL) {
            contentType(ContentType.Application.Json)
            headers {
                append("x-api-key", apiKey)
                append("anthropic-version", ANTHROPIC_VERSION)
                append(HttpHeaders.Accept, "text/event-stream")
            }
            setBody(body)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]" || data.isBlank()) continue
                try {
                    val obj = json.parseToJsonElement(data).jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: continue
                    if (type == "content_block_delta") {
                        val delta = obj["delta"]?.jsonObject ?: continue
                        if (delta["type"]?.jsonPrimitive?.content == "text_delta") {
                            val text = delta["text"]?.jsonPrimitive?.content ?: continue
                            emit(text)
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
