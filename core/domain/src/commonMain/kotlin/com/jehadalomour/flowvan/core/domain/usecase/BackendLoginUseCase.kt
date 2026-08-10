package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.UserEntity
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.mapper.toUser
import com.jehadalomour.flowvan.core.network.http.NetworkException
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.data.device.DeviceIdentityProvider
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import com.jehadalomour.flowvan.core.model.User
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Real backend login by `userNumber` + password (distinct from the demo phone login).
 * Persists the JWT to [SessionStore] and caches the user in Room so session-restore works.
 */
class BackendLoginUseCase(
    private val authApi: AuthApi,
    private val users: UserRepository,
    private val session: SessionStore,
    private val deviceIdentity: DeviceIdentityProvider,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(userNumber: String, password: String): Result<User> {
        if (userNumber.isBlank()) {
            return Result.failure(AuthException(CashFlowError.Auth.InvalidPhone))
        }
        if (password.length < 6) {
            return Result.failure(AuthException(CashFlowError.Auth.InvalidPassword))
        }
        return try {
            // Identify the handset so the server can enforce one-phone-per-rep
            // and hand back the tracking credential. A refusal surfaces as
            // CashFlowError.Auth.DeviceBoundToOtherUser / UserActiveOnOtherDevice.
            val device = deviceIdentity.identity()
            val resp = authApi.login(
                userNumber = userNumber,
                password = password,
                deviceId = device.deviceId,
                platform = device.platform,
                deviceModel = device.model,
            )
            val user = resp.user.toUser(resp.accessToken)
            val now = Clock.System.now().toEpochMilliseconds()
            users.cache(
                UserEntity(
                    id = user.id,
                    nameAr = user.nameAr,
                    nameEn = user.nameEn,
                    phone = user.phone,
                    passwordHash = "",
                    role = user.role.name,
                    token = resp.accessToken,
                    lastLoginAt = now,
                    lastLoginLat = null,
                    lastLoginLng = null,
                ),
            )
            session.currentUserId = user.id
            session.currentToken = resp.accessToken
            session.currentRepId = resp.user.repId
            session.currentUserCode = resp.user.userNumber
            session.currentPermKeys = resp.user.permKeys.joinToString(",")
            session.canAddCustomer = resp.user.permissions["canAddCustomer"] == true
            session.canCreateCustomerDirect =
                resp.user.permissions["canCreateCustomerDirect"] == true
            session.canPrintLineDiscount = resp.user.permissions["canPrintLineDiscount"] == true
            session.boundDeviceId = device.deviceId
            // Kept outside the session on purpose: this is what keeps the phone
            // reporting once the rep signs out.
            resp.trackingToken?.let {
                session.trackingToken = it
                session.trackingRepId = resp.user.repId
            }
            Result.success(user)
        } catch (e: NetworkException) {
            Result.failure(AuthException(e.error))
        } catch (e: Exception) {
            Result.failure(AuthException(CashFlowError.Network.Unreachable))
        }
    }
}
