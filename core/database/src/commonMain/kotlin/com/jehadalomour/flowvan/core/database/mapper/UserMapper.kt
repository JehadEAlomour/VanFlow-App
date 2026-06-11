package com.jehadalomour.flowvan.shared.data.local.mapper

import com.jehadalomour.flowvan.shared.data.local.entity.UserEntity
import com.jehadalomour.flowvan.shared.domain.model.User
import com.jehadalomour.flowvan.shared.domain.model.UserRole

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