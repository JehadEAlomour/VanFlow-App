package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.repository.UserRepository
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import com.jehadalomour.flowvan.shared.domain.model.User

class GetCurrentUserUseCase(
    private val users: UserRepository,
    private val session: SessionStore,
) {
    suspend operator fun invoke(): User? {
        val id = session.currentUserId ?: return null
        return users.findById(id)
    }
}