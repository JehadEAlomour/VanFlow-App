package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.settings.SessionStore

class LogoutUseCase(private val session: SessionStore) {
    operator fun invoke() {
        session.clear()
    }
}