package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.model.ledger.StatementDocType
import com.jehadalomour.flowvan.core.model.ledger.StatementMovement
import com.jehadalomour.flowvan.core.model.ledger.StatementSnapshot
import com.jehadalomour.flowvan.core.network.api.CollectionApi
import com.jehadalomour.flowvan.core.network.api.VoucherApi
import com.jehadalomour.flowvan.core.network.dto.CollectionDto
import com.jehadalomour.flowvan.core.network.dto.VoucherSummaryDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Builds كشف الحساب — a customer's account statement — from the SERVER.
 *
 * The statement used to be assembled from Room alone, and Room only ever holds what
 * this handset created: nothing writes another van's sales or the office's own entries
 * into it. A shop billed by two reps therefore saw a statement missing half its debt,
 * and a rep who reinstalled the app saw an empty one. That is the wrong answer to give
 * with a shopkeeper reading over your shoulder, so the account is read from the book of
 * record whenever there is a connection.
 *
 * Returns null when it could not be built — offline, or either call failed. The caller
 * falls back to the local ledger and says on screen that it did: a statement short of
 * its payments reads as unpaid debt, so a partial server answer is never merged into a
 * whole-looking one.
 */
@OptIn(ExperimentalTime::class)
class CustomerStatementRepository(
    private val vouchers: VoucherApi,
    private val collections: CollectionApi,
    private val connectivity: ConnectivityObserver,
) {
    private val log = Logger.withTag("CustomerStatement")

    /**
     * @param customerNumber the customer's CODE — what vouchers are keyed by.
     * @param customerId the customer's UUID — what collections are keyed by. The two
     *   endpoints identify a customer differently, and the wrong one returns an empty
     *   list rather than an error, so they are separate parameters.
     * @param fromMillis start of the period, inclusive (local midnight).
     * @param toMillis end of the period, inclusive (local end-of-day).
     */
    suspend fun load(
        customerNumber: String,
        customerId: String,
        fromMillis: Long,
        toMillis: Long,
    ): StatementSnapshot? = coroutineScope {
        if (customerNumber.isBlank() || !connectivity.isOnline()) return@coroutineScope null

        // The period, and everything before it. The prior window is fetched in full
        // rather than trusting the customer's CURRENT balance as an opening figure:
        // that number is today's, and a statement opened for last month would then
        // start from the wrong line and be wrong on every row after it.
        val priorEnd = fromMillis - 1
        val period = async { fetchWindow(customerNumber, customerId, fromMillis, toMillis) }
        val prior = async { fetchWindow(customerNumber, customerId, EPOCH_MILLIS, priorEnd) }
        val (periodRows, priorRows) = awaitAll(period, prior)

        if (periodRows == null || priorRows == null) return@coroutineScope null

        StatementSnapshot(
            openingBalance = priorRows.sumOf { it.movement },
            movements = periodRows.sortedBy { it.createdAt },
            isLive = true,
        )
    }

    /** One window of the account. Null when either half of it failed to load. */
    private suspend fun fetchWindow(
        customerNumber: String,
        customerId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<StatementMovement>? = coroutineScope {
        if (toMillis < fromMillis) return@coroutineScope emptyList()

        val voucherRows = async {
            runCatching {
                vouchers.customerTransactions(
                    customerNumber = customerNumber,
                    // `dateFrom`/`dateTo` are whole days: the API compares them
                    // against in_date as `>= dateFrom` and `< dateTo + 1`, so the
                    // last day is included.
                    dateFrom = fromMillis.toIsoDate(),
                    dateTo = toMillis.toIsoDate(),
                )
            }.onFailure { log.w("voucher fetch failed: ${it.message}") }.getOrNull()
        }
        val collectionRows = async {
            runCatching { fetchCollections(customerId, fromMillis, toMillis) }
                .onFailure { log.w("collection fetch failed: ${it.message}") }
                .getOrNull()
        }

        val v = voucherRows.await() ?: return@coroutineScope null
        val c = collectionRows.await() ?: return@coroutineScope null
        v.mapNotNull(::toMovement) + c.mapNotNull(::toMovement)
    }

    /**
     * Every collection in the window, paged.
     *
     * The endpoint caps a page at 200 and the opening-balance window is the shop's
     * whole history, so one page is not a safe assumption — a long-standing customer
     * would silently lose their oldest receipts and open the statement owing more than
     * they do.
     *
     * Bounds go as full instants rather than dates: the API compares `collected_at`
     * against them literally, so a plain `2026-09-06` means that day's midnight and
     * drops everything collected during the day — including, always, today's.
     */
    private suspend fun fetchCollections(
        customerId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<CollectionDto> {
        val all = mutableListOf<CollectionDto>()
        var offset = 0
        while (true) {
            val page = collections.list(
                customerId = customerId,
                from = fromMillis.toIsoInstant(),
                to = toMillis.toIsoInstant(),
                limit = PAGE,
                offset = offset,
            )
            all += page.items
            if (page.items.size < PAGE || all.size >= page.total) break
            offset += PAGE
            if (offset >= MAX_COLLECTIONS) break
        }
        return all
    }

    /**
     * A voucher as a ledger row, or null when it never touched the account.
     *
     * The test is the credit total — the slice of the voucher left ON ACCOUNT, which
     * the server computes from its CREDIT payment lines. A sale settled at the counter
     * carries zero and creates no receivable; listing it would inflate the debit column
     * and hand the customer a demand for money they already paid. Orders are excluded
     * outright: an order is a promise of goods, and nothing is owed until it is sold.
     */
    private fun toMovement(v: VoucherSummaryDto): StatementMovement? {
        val kind = v.transKind.uppercase()
        val docType = when (kind) {
            "SALE" -> StatementDocType.SALE
            "RETURN" -> StatementDocType.RETURN
            else -> return null
        }
        val credit = v.creditTotal.toDoubleOrNull() ?: 0.0
        if (credit <= 0.0) return null
        return StatementMovement(
            id = v.id,
            number = v.voucherNumber,
            createdAt = (v.inDate ?: v.createdAt).orEmpty().isoDateToMillis() ?: 0L,
            docType = docType,
            debit = if (docType == StatementDocType.SALE) credit else 0.0,
            credit = if (docType == StatementDocType.RETURN) credit else 0.0,
        )
    }

    /**
     * A receipt as a ledger row.
     *
     * A bounced cheque is dropped: the money never arrived, and crediting it would
     * show the debt as settled while the shop still owes it.
     */
    private fun toMovement(c: CollectionDto): StatementMovement? {
        if (c.status.equals("bounced", ignoreCase = true)) return null
        return StatementMovement(
            id = c.id,
            // Collections carry no voucher number of their own on this endpoint.
            number = c.id.take(8),
            createdAt = c.collectedAt.orEmpty().isoDateToMillis() ?: 0L,
            docType = StatementDocType.PAYMENT,
            // Collections are stored in FILS and vouchers in major units; mixing
            // the two inflates a payment a thousandfold.
            credit = c.amount / 1000.0,
            method = c.method.uppercase(),
            chequeDate = c.cheque?.dueDate?.isoDateToMillis(),
        )
    }

    private companion object {
        /** The API's own page cap. */
        const val PAGE = 200
        /** A shop with more receipts than this has an account no phone should print. */
        const val MAX_COLLECTIONS = 2000
        /** Far enough back to be "all history" without sending a negative date. */
        val EPOCH_MILLIS = 0L
    }
}

// ── Dates ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
private fun Long.toIsoDate(): String {
    val d = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${d.year}-${d.monthNumber.toString().padStart(2, '0')}-" +
        d.dayOfMonth.toString().padStart(2, '0')
}

/** A UTC instant the API can compare against a timestamp column exactly. */
@OptIn(ExperimentalTime::class)
private fun Long.toIsoInstant(): String = Instant.fromEpochMilliseconds(this).toString()

/**
 * The server's date as local millis. Accepts both shapes it sends: a full timestamp
 * (`collected_at`) and a bare day (`in_date`), the latter placed at local midnight so
 * it sorts among the day's receipts rather than ahead of every one of them.
 */
@OptIn(ExperimentalTime::class)
private fun String.isoDateToMillis(): Long? {
    if (isBlank()) return null
    val tz = TimeZone.currentSystemDefault()
    // An offset-bearing timestamp, then a bare local one, then the day alone.
    return runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(this).toInstant(tz).toEpochMilliseconds() }.getOrNull()
        ?: runCatching { LocalDate.parse(take(10)).atStartOfDayIn(tz).toEpochMilliseconds() }.getOrNull()
}
