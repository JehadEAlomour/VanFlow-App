package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object VoucherNumber {
    /**
     * Format: PREFIX-YYYYMMDD-####
     * The day-sequence portion is derived from a millisecond-of-day fraction so it's
     * monotonic without needing a counter table; collisions across two devices are
     * resolved by the backend in M17.
     *
     * Used for non-voucher numbers (e.g. collection receipts). SALE/RETURN/ORDER vouchers
     * use [VoucherNumberGenerator] for a sequential per-type yearly counter.
     */
    @OptIn(ExperimentalTime::class)
    fun next(prefix: String): String {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val ldt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
        val ymd = "${ldt.year}${ldt.monthNumber.pad2()}${ldt.dayOfMonth.pad2()}"
        val secondOfDay = ldt.hour * 3600 + ldt.minute * 60 + ldt.second
        val seq = (secondOfDay % 10000).toString().padStart(4, '0')
        return "$prefix-$ymd-$seq"
    }

    private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
}

/**
 * Builds SALE/RETURN/ORDER voucher numbers as `PREFIX-YEAR-userCode-seq`, e.g. `INV-2026-U-0001-1`.
 * Each [type] keeps its own sequence that restarts at 1 each calendar year; the `userCode`
 * segment keeps numbers unique across devices/reps (the backend dedupes on the full number).
 *
 * The sequence is the highest trailing sequence used this year + 1 (i.e. last voucher
 * number + 1), so a voucher's number is generated once at creation, stored, and reused
 * verbatim on every sync retry — and offline numbers never collide even after a
 * cancelled/removed voucher.
 */
class VoucherNumberGenerator(
    private val invoices: InvoiceRepository,
    private val session: SessionStore,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun next(prefix: String, type: String): String {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val year = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).year
        val yearStart = LocalDateTime(year, 1, 1, 0, 0, 0).toInstant(tz).toEpochMilliseconds()
        val yearEnd = LocalDateTime(year + 1, 1, 1, 0, 0, 0).toInstant(tz).toEpochMilliseconds()
        // Last voucher number + 1 (max trailing sequence + 1), not a COUNT — so an offline
        // number is never reused after a voucher is cancelled/removed.
        val seq = invoices.nextSeqForType(type, yearStart, yearEnd)
        val userCode = session.currentUserCode?.takeIf { it.isNotBlank() } ?: "U-0000"
        return "$prefix-$year-$userCode-$seq"
    }
}
