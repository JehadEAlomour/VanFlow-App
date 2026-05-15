package com.jehadalomour.flowvan.shared.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.local.dao.LocationPointDao
import com.jehadalomour.flowvan.shared.data.local.dao.PaymentDao
import com.jehadalomour.flowvan.shared.data.remote.SyncApi
import com.jehadalomour.flowvan.shared.data.settings.SyncConfig
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SyncRepository(
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
    private val locationPointDao: LocationPointDao,
    private val syncApi: SyncApi,
    private val syncConfig: SyncConfig,
) {
    private val log = Logger.withTag("SyncRepository")

    @OptIn(ExperimentalTime::class)
    suspend fun syncPending(): SyncResult {
        if (!syncConfig.isEnabled) return SyncResult(0, 0, 0, skipped = true)
        val baseUrl = syncConfig.baseUrl
        val now = Clock.System.now().toEpochMilliseconds()
        var invoicesSynced = 0
        var paymentsSynced = 0
        var pointsSynced = 0

        try {
            val invoices = invoiceDao.findUnsynced(50)
            if (invoices.isNotEmpty()) {
                syncApi.postInvoices(baseUrl, invoices)
                invoiceDao.markSynced(invoices.map { it.id }, now)
                invoicesSynced = invoices.size
            }
        } catch (e: Exception) {
            log.e("Invoice sync failed: ${e.message}")
        }

        try {
            val payments = paymentDao.findUnsynced(50)
            if (payments.isNotEmpty()) {
                syncApi.postPayments(baseUrl, payments)
                paymentDao.markSynced(payments.map { it.id }, now)
                paymentsSynced = payments.size
            }
        } catch (e: Exception) {
            log.e("Payment sync failed: ${e.message}")
        }

        try {
            val points = locationPointDao.findUnsynced(100)
            if (points.isNotEmpty()) {
                syncApi.postTracking(baseUrl, points)
                locationPointDao.markSynced(points.map { it.id })
                pointsSynced = points.size
            }
        } catch (e: Exception) {
            log.e("Tracking sync failed: ${e.message}")
        }

        return SyncResult(invoicesSynced, paymentsSynced, pointsSynced)
    }
}

data class SyncResult(
    val invoicesSynced: Int,
    val paymentsSynced: Int,
    val pointsSynced: Int,
    val skipped: Boolean = false,
)
