package com.jehadalomour.flowvan.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String?,
    val phone: String,
    val passwordHash: String,
    val role: String,
    val token: String?,
    val lastLoginAt: Long?,
    val lastLoginLat: Double?,
    val lastLoginLng: Double?,
)