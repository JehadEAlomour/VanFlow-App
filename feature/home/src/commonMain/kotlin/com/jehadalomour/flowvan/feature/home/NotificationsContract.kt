package com.jehadalomour.flowvan.feature.home

import com.jehadalomour.flowvan.core.network.dto.AppNotificationDto

data class NotificationsState(
    val isLoading: Boolean = true,
    val items: List<AppNotificationDto> = emptyList(),
    val unread: Int = 0,
    val error: String? = null,
)

sealed interface NotificationsEvent {
    data object Refresh : NotificationsEvent
    data class MarkRead(val id: String) : NotificationsEvent
    data object MarkAllRead : NotificationsEvent
}
