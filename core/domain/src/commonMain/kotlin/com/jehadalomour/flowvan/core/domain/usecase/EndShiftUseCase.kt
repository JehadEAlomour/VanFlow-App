package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.dao.ShiftDao
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EndShiftUseCase(private val shiftDao: ShiftDao) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(shiftId: String) {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        shiftDao.endShift(shiftId, nowMs)
    }
}
