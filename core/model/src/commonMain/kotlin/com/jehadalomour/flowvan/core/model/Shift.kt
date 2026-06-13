package com.jehadalomour.flowvan.core.model

enum class ShiftStatus { ACTIVE, ENDED }

data class Shift(
    val id: String,
    val userId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: ShiftStatus,
    val startLat: Double?,
    val startLng: Double?,
    val endLat: Double?,
    val endLng: Double?,
)

data class LocationPoint(
    val id: Long,
    val shiftId: String,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float?,
    val recordedAt: Long,
    val synced: Boolean,
)

data class RouteStop(
    val id: String,
    val userId: String,
    val customerId: String,
    val planDate: Long,
    val stopOrder: Int,
    val visited: Boolean,
)

data class AiMessage(
    val id: String,
    val conversationId: String,
    val role: String, // "user" | "assistant"
    val content: String,
    val createdAt: Long,
)