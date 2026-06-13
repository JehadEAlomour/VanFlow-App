package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.UserEntity
import com.jehadalomour.flowvan.core.network.api.AuthApi
import com.jehadalomour.flowvan.core.network.mapper.toUser
import com.jehadalomour.flowvan.core.network.http.NetworkException
import com.jehadalomour.flowvan.core.data.repository.UserRepository
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
            val resp = authApi.login(userNumber, password)
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
            Result.success(user)
        } catch (e: NetworkException) {
            Result.failure(AuthException(e.error))
        } catch (e: Exception) {
            Result.failure(AuthException(CashFlowError.Network.Unreachable))
        }
    }
}
