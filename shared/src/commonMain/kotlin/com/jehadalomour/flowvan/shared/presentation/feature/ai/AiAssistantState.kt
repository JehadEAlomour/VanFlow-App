package com.jehadalomour.flowvan.shared.presentation.feature.ai

import com.jehadalomour.flowvan.shared.domain.model.AiMessage

data class AiAssistantState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isOffline: Boolean = true,
)

val quickChips = listOf("ملخص اليوم", "أعلى عميل", "ذمم متأخرة", "توقع المبيعات", "آخر فاتورة", "أداء الأسبوع")
