package com.jehadalomour.flowvan.feature.ai

sealed class AiAssistantEvent {
    data class InputChanged(val text: String) : AiAssistantEvent()
    data object Send : AiAssistantEvent()
    data class ChipTapped(val text: String) : AiAssistantEvent()
    data object OpenApiKeyDialog : AiAssistantEvent()
    data object DismissApiKeyDialog : AiAssistantEvent()
    data class ApiKeyInputChanged(val text: String) : AiAssistantEvent()
    data object SaveApiKey : AiAssistantEvent()
}
