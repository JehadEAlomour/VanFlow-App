package com.jehadalomour.flowvan.feature.ai

import com.jehadalomour.flowvan.core.model.AiMessage

data class AiAssistantState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val isOffline: Boolean = true,
    val apiKeySet: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val apiKeyInput: String = "",
)

val quickChips = listOf("ملخص اليوم", "أعلى عميل", "ذمم متأخرة", "توقع المبيعات", "آخر فاتورة", "أداء الأسبوع")
