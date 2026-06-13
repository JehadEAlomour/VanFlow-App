package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import com.jehadalomour.flowvan.core.model.User
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LoginUseCase(
    private val users: UserRepository,
    private val session: SessionStore,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        phone: String,
        password: String,
        lat: Double?,
        lng: Double?,
    ): Result<User> {
        val cleanPhone = phone.trim()
        if (cleanPhone.length != 10 || !cleanPhone.startsWith("07")) {
            return Result.failure(AuthException(CashFlowError.Auth.InvalidPhone))
        }
        if (password.length < 4) {
            return Result.failure(AuthException(CashFlowError.Auth.InvalidPassword))
        }
        val entity = users.findByPhone(cleanPhone)
            ?: return Result.failure(AuthException(CashFlowError.Auth.UserNotFound))
        if (entity.passwordHash != "demo_hash_$password") {
            return Result.failure(AuthException(CashFlowError.Auth.WrongPassword))
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val token = "demo-token-${entity.id}-$now"
        users.updateLastLogin(entity, token, lat, lng, now)
        session.currentUserId = entity.id
        session.currentToken = token
        return Result.success(entity.copy(token = token, lastLoginAt = now, lastLoginLat = lat, lastLoginLng = lng).toDomain())
    }
}

class AuthException(val error: CashFlowError) : Exception(error.messageEn)