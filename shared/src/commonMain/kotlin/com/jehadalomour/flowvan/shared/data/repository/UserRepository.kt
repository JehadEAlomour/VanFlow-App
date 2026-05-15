package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.UserDao
import com.jehadalomour.flowvan.shared.data.local.entity.UserEntity
import com.jehadalomour.flowvan.shared.data.local.mapper.toDomain
import com.jehadalomour.flowvan.shared.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(private val dao: UserDao) {

    suspend fun findByPhone(phone: String): UserEntity? = dao.findByPhone(phone.trim())

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