package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.User

class GetCurrentUserUseCase(
    private val users: UserRepository,
    private val session: SessionStore,
) {
    suspend operator fun invoke(): User? {
        val id = session.currentUserId ?: return null
        return users.findById(id)
    }
}