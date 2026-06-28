package com.jehadalomour.flowvan.core.model

enum class UserRole { SALESMAN, SUPERVISOR, MANAGER, ADMIN }

data class User(
    val id: String,
    val nameAr: String,
    val nameEn: String?,
    val phone: String,
    val role: UserRole,
    val token: String? = null,
    val lastLoginAt: Long? = null,
    val lastLoginLat: Double? = null,
    val lastLoginLng: Double? = null,
)