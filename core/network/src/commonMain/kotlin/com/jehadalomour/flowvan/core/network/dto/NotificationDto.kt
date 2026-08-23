package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

/**
 * One inbox notification. Mirrors the backend `AppNotification`.
 *
 * `refType`/`refId` let the app deep-link: a `refType == "stock-request"` alert
 * opens that request so the rep can confirm receipt. `readAt` null ⇒ unread.
 */
@Serializable
data class AppNotificationDto(
    val id: String,
    val kind: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val bodyAr: String? = null,
    val bodyEn: String? = null,
    val refType: String? = null,
    val refId: String? = null,
    val readAt: String? = null,
    val createdAt: String = "",
) {
    val isUnread: Boolean get() = readAt == null
}

/** GET /notifications envelope: rows plus the unread count for the badge. */
@Serializable
data class NotificationListDto(
    val items: List<AppNotificationDto> = emptyList(),
    val total: Int = 0,
    val unread: Int = 0,
)
