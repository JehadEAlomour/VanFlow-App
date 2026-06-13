package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val userNumber: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val user: ApiUserDto,
)

@Serializable
data class ApiUserDto(
    val id: String,
    val userNumber: String = "",
    val name: String = "",
    val userType: String = "SALES",          // ADMIN | MANAGER | SALES | DRIVER
    val role: String? = null,                 // admin | manager | supervisor | viewer
    val repId: String? = null,                // backend rep id — used by invoice/collection endpoints
    val permissions: Map<String, Boolean> = emptyMap(),
    /** F10 granular permission keys (e.g. vouchers.return.direct). */
    val permKeys: List<String> = emptyList(),
)

/** `GET /auth/me` payload. */
@Serializable
data class MeDto(
    val sub: String,
    val userNumber: String = "",
    val userType: String = "SALES",
    val role: String? = null,
    val repId: String? = null,
    val permissions: Map<String, Boolean> = emptyMap(),
    /** F10 granular permission keys (e.g. vouchers.return.direct). */
    val permKeys: List<String> = emptyList(),
)
