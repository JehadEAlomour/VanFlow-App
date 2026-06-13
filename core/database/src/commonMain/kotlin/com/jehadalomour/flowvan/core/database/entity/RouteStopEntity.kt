package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_stops")
data class RouteStopEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val customerId: String,
    val planDate: Long,
    val stopOrder: Int,
    val visited: Boolean,
)