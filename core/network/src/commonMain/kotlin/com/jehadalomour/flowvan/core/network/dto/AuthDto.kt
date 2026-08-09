package com.jehadalomour.flowvan.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val userNumber: String,
    val password: String,
    /**
     * Handset identity. Sending it turns on device binding server-side — one
     * live handset per salesman and one salesman per handset — and earns the
     * `trackingToken` that keeps this phone reporting after sign-out.
     */
    val deviceId: String? = null,
    val platform: String? = null,
    val deviceModel: String? = null,
)

@Serializable
data class LogoutRequest(val deviceId: String? = null)

@Serializable
data class LoginResponseDto(
    val accessToken: String,
    /**
     * Long-lived, location-only credential, returned only when a deviceId was
     * sent. Stored outside the session so signing out does not take the trail
     * down with it.
     */
    val trackingToken: String? = null,
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

/** `GET /company-info` payload — company profile + the authoritative tax mode (mirrors the ERP). */
@Serializable
data class CompanyInfoDto(
    val companyNameAr: String = "",
    val companyNameEn: String? = null,
    val sellerTin: String? = null,
    val sellerAddress: String? = null,
    val sellerPhone: String? = null,
    val logoUrl: String? = null,
    /** INCLUSIVE | EXCLUSIVE — the source of truth for the app's tax mode. */
    val taxCalcMethod: String = "EXCLUSIVE",
    val timezone: String = "Asia/Amman",
    val locale: String = "ar",
)
