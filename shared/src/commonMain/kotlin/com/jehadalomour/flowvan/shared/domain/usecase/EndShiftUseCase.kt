package com.jehadalomour.flowvan.shared.domain.usecase

import com.jehadalomour.flowvan.shared.data.local.dao.ShiftDao
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EndShiftUseCase(private val shiftDao: ShiftDao) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(shiftId: String) {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        shiftDao.endShift(shiftId, nowMs)
    }
}
