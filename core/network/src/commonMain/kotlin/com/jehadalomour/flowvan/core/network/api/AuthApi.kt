package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CompanyInfoDto
import com.jehadalomour.flowvan.core.network.dto.LoginRequest
import com.jehadalomour.flowvan.core.network.dto.LoginResponseDto
import com.jehadalomour.flowvan.core.network.dto.LogoutRequest
import com.jehadalomour.flowvan.core.network.dto.MeDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

class AuthApi(private val client: FlowVanApiClient) {

    suspend fun login(
        userNumber: String,
        password: String,
        deviceId: String? = null,
        platform: String? = null,
        deviceModel: String? = null,
    ): LoginResponseDto = client.postData(
        "auth/login",
        LoginRequest(userNumber.trim(), password, deviceId, platform, deviceModel),
    )

    /**
     * Marks this handset's session closed. Deliberately does NOT release the
     * binding or the tracking token — the phone keeps reporting until the
     * office releases it.
     */
    suspend fun logout(deviceId: String?): Unit =
        client.postData("auth/logout", LogoutRequest(deviceId))

    suspend fun me(): MeDto = client.getData("auth/me")

    /** Company profile + authoritative tax mode (mirrors the ERP). */
    suspend fun companyInfo(): CompanyInfoDto = client.getData("company-info")
}
