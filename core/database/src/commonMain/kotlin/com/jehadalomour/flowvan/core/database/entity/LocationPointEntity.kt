package com.jehadalomour.flowvan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_points")
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val shiftId: String,
    val userId: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float?,
    val recordedAt: Long,
    val synced: Boolean,
)