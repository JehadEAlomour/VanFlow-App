package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.UserDao
import com.jehadalomour.flowvan.core.database.entity.UserEntity
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(private val dao: UserDao) {

    suspend fun findByPhone(phone: String): UserEntity? = dao.findByPhone(phone.trim())

    /** Persist a backend-authenticated user so session-restore (findById) works on relaunch. */
    suspend fun cache(user: UserEntity) = dao.upsert(user)

    suspend fun findById(id: String): User? = dao.findById(id)?.toDomain()

    fun observeById(id: String): Flow<User?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun updateLastLogin(
        user: UserEntity,
        token: String,
        lat: Double?,
        lng: Double?,
        loggedInAt: Long,
    ) {
        dao.upsert(
            user.copy(
                token = token,
                lastLoginAt = loggedInAt,
                lastLoginLat = lat,
                lastLoginLng = lng,
            ),
        )
    }
}