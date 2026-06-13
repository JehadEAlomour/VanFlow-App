package com.jehadalomour.flowvan.core.database.mapper

import com.jehadalomour.flowvan.core.database.entity.UserEntity
import com.jehadalomour.flowvan.core.model.User
import com.jehadalomour.flowvan.core.model.UserRole

fun UserEntity.toDomain(): User = User(
    id = id,
    nameAr = nameAr,
    nameEn = nameEn,
    phone = phone,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.SALESMAN),
    token = token,
    lastLoginAt = lastLoginAt,
    lastLoginLat = lastLoginLat,
    lastLoginLng = lastLoginLng,
)