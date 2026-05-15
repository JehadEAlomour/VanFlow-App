package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.dao.ShiftDao
import com.jehadalomour.flowvan.shared.data.local.entity.ShiftEntity
import com.jehadalomour.flowvan.shared.data.settings.SessionStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class StartShiftUseCase(
    private val shiftDao: ShiftDao,
    private val sessionStore: SessionStore,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): String {
        val userId = sessionStore.currentUserId ?: return ""
        shiftDao.findActive(userId)?.let { return it.id }
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val shiftId = "SHF-$nowMs"
        shiftDao.upsert(
            ShiftEntity(
                id = shiftId,
                userId = userId,
                startedAt = nowMs,
                endedAt = null,
                status = "ACTIVE",
                startLat = null,
                startLng = null,
                endLat = null,
                endLng = null,
            ),
        )
        return shiftId
    }
}
