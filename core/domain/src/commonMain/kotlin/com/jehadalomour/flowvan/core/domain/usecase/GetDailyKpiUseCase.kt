package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.model.DailyKpi
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class GetDailyKpiUseCase(
    private val invoices: InvoiceRepository,
    private val payments: PaymentRepository,
    private val customerDao: CustomerDao,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): DailyKpi {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val startOfTodayMs = today.atStartOfDayIn(tz).toEpochMilliseconds()

        return DailyKpi(
            salesTotal = invoices.salesTotalSince(startOfTodayMs),
            returnsTotal = invoices.returnsTotalSince(startOfTodayMs),
            collectionsTotal = payments.confirmedTotalSince(startOfTodayMs),
            customersVisited = invoices.distinctCustomersSince(startOfTodayMs),
            customersPlanned = customerDao.countOnRoute(),
        )
    }
}
