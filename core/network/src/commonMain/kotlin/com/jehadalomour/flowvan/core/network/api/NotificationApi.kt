package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.NotificationListDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postUnit

/**
 * The signed-in user's notification inbox. Delivery is POLLING, not push: the
 * client handsets are GMS-less (no Play Services), so FCM cannot run — the app
 * fetches this on foreground and on a light interval instead.
 */
class NotificationApi(private val client: FlowVanApiClient) {

    /** Newest first. [unreadOnly] filters to unread; the response always carries the unread count. */
    suspend fun list(unreadOnly: Boolean = false, limit: Int = 30): NotificationListDto =
        client.getData("notifications", mapOf("unread" to unreadOnly.toString(), "limit" to limit.toString()))

    suspend fun markRead(id: String) = client.postUnit("notifications/$id/read")

    suspend fun markAllRead() = client.postUnit("notifications/read-all")
}
