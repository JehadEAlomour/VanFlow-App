package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.LocationPointDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.network.api.CollectionApi
import com.jehadalomour.flowvan.core.network.api.ApprovalApi
import com.jehadalomour.flowvan.core.network.api.RepApi
import com.jehadalomour.flowvan.core.network.api.VoucherApi
import com.jehadalomour.flowvan.core.network.dto.LocationBulkRequest
import com.jehadalomour.flowvan.core.network.dto.LocationPingRequest
import com.jehadalomour.flowvan.core.network.mapper.toCreateCollectionRequest
import com.jehadalomour.flowvan.core.network.mapper.toVoucherRequest
import com.jehadalomour.flowvan.core.network.http.ApiConfig
import com.jehadalomour.flowvan.core.network.http.NetworkException
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.common.error.CashFlowError
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Pushes locally-saved (offline) transactions to the VanFlow backend and marks them synced.
 * Records that fail keep `syncedAt = null` so the next run (periodic, on-create, or on-reconnect)
 * retries them. SALE invoices and cash/cheque collections are pushed; RETURN/REQUEST vouchers
 * stay flagged until a backend mapping exists (logged, never lost).
 */
class SyncRepository(
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
    private val locationPointDao: LocationPointDao,
    private val customerDao: CustomerDao,
    private val voucherApi: VoucherApi,
    private val collectionApi: CollectionApi,
    private val repApi: RepApi,
    private val approvalApi: ApprovalApi,
    private val apiConfig: ApiConfig,
    private val session: SessionStore,
    private val json: Json,
) {
    private val log = Logger.withTag("SyncRepository")

    @OptIn(ExperimentalTime::class)
    suspend fun syncPending(): SyncResult {
        if (!apiConfig.isEnabled) return SyncResult(0, 0, 0, skipped = true)
        val repId = session.currentRepId
        if (repId.isNullOrBlank()) {
            log.w("no repId in session — cannot push transactions yet")
            return SyncResult(0, 0, 0, skipped = true)
        }
        val userCode = session.currentUserCode
        if (userCode.isNullOrBlank()) {
            log.w("no userCode in session — cannot push vouchers yet")
            return SyncResult(0, 0, 0, skipped = true)
        }
        val now = Clock.System.now().toEpochMilliseconds()
        var invoicesSynced = 0
        var paymentsSynced = 0
        var pointsSynced = 0

        // ── Vouchers: SALE / RETURN / ORDER via POST /sync/vouchers ────
        // (Transaction kinds are seeded server-side — no ensure step needed.)
        for (inv in invoiceDao.findUnsynced(50)) {
            try {
                val customerNumber = customerDao.findById(inv.customerId)?.code
                val result = voucherApi.create(inv.toVoucherRequest(userCode, customerNumber, json))
                // Adopt the server's authoritative voucher number (the local one was provisional).
                result.voucherNumber.takeIf { it.isNotBlank() && it != inv.number }
                    ?.let { invoiceDao.updateNumber(inv.id, it) }
                invoiceDao.markSynced(listOf(inv.id), now)
                invoicesSynced++
            } catch (e: NetworkException) {
                val err = e.error
                when {
                    err is CashFlowError.Network.Conflict -> {
                        // Already created on a previous attempt — treat as synced.
                        invoiceDao.markSynced(listOf(inv.id), now)
                        invoicesSynced++
                    }
                    err is CashFlowError.Network.ApprovalRequired -> {
                        // F10: the salesman lacks the permission — convert the push
                        // into a manager approval request carrying the SAME payload.
                        // The backend creates the voucher itself on approval.
                        try {
                            val customerNumber = customerDao.findById(inv.customerId)?.code
                            approvalApi.create(
                                type = err.type,
                                payload = inv.toVoucherRequest(userCode, customerNumber, json),
                                note = inv.notes,
                                customerNumber = customerNumber,
                            )
                            // Mark synced so we don't re-file every minute; the approved
                            // voucher arrives via the normal catalog/voucher refresh.
                            invoiceDao.markSynced(listOf(inv.id), now)
                            log.w("voucher ${inv.number} → filed approval request (${err.type})")
                        } catch (fileErr: Exception) {
                            log.e("voucher ${inv.number} approval filing failed: ${fileErr.message}")
                        }
                    }
                    else -> log.e("voucher ${inv.number} push failed: ${err.messageEn}")
                }
            } catch (e: Exception) {
                log.e("voucher ${inv.number} push failed: ${e.message}")
            }
        }

        // ── Collections (payments) ─────────────────────────────────────
        for (pmt in paymentDao.findUnsynced(50)) {
            try {
                collectionApi.create(pmt.toCreateCollectionRequest(repId))
                paymentDao.markSynced(listOf(pmt.id), now)
                paymentsSynced++
            } catch (e: Exception) {
                log.e("collection ${pmt.number} push failed: ${e.message}")
            }
        }

        // ── GPS trail (bulk) ───────────────────────────────────────────
        // Drain the whole queue in ≤500-point batches (backend bulk cap), keeping the
        // true on-device capture time so offline catch-ups land on the right trail slot.
        var batches = 0
        while (batches < 10) {
            val points = locationPointDao.findUnsynced(500)
            if (points.isEmpty()) break
            val pings = points.map {
                LocationPingRequest(
                    lat = it.lat,
                    lng = it.lng,
                    accuracyM = it.accuracy?.toDouble(),
                    recordedAt = Instant.fromEpochMilliseconds(it.recordedAt).toString(),
                )
            }
            val path = "reps/$repId/location/bulk"
            val bodyJson = json.encodeToString(LocationBulkRequest.serializer(), LocationBulkRequest(pings))
            // A copy-paste-runnable curl for the exact request (debug only).
            log.d { "GPS bulk → ${pings.size} pts\n${trackingCurl(path, bodyJson)}" }
            try {
                repApi.postLocationBulk(repId, pings)
                locationPointDao.markSynced(points.map { it.id })
                pointsSynced += points.size
            } catch (e: NetworkException) {
                // Keep the batch in the queue, retry next cycle. Log the reason + the exact body.
                log.e("GPS bulk push failed: ${e.error.messageEn}\nbody sent: $bodyJson")
                break
            } catch (e: Exception) {
                log.e("GPS bulk push failed: ${e.message}\nbody sent: $bodyJson")
                break
            }
            batches++
        }

        return SyncResult(invoicesSynced, paymentsSynced, pointsSynced)
    }

    /** Builds the equivalent `curl` for a tracking POST so a failing request can be replayed. */
    private fun trackingCurl(path: String, bodyJson: String): String {
        val url = apiConfig.urlFor(path)
        val auth = session.currentToken?.let { "Bearer $it" } ?: "(no token)"
        return "curl -X POST '$url' " +
            "-H 'Authorization: $auth' " +
            "-H 'Content-Type: application/json' " +
            "-d '$bodyJson'"
    }

}

data class SyncResult(
    val invoicesSynced: Int,
    val paymentsSynced: Int,
    val pointsSynced: Int,
    val skipped: Boolean = false,
)
