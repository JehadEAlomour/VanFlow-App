package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val dao: InvoiceDao) {

    suspend fun listSince(sinceMillis: Long) = dao.listSince(sinceMillis)

    suspend fun salesTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis).filter { it.type == "SALE" }.sumOf { it.total }

    /** Cash SALE total (CASH/CHEQUE/TRANSFER) — excludes credit (آجل) sales. Day cash. */
    suspend fun cashSalesTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis)
            .filter { it.type == "SALE" && it.paymentMethod != "CREDIT" }
            .sumOf { it.total }

    /** Credit (on-account, آجل) SALE total — receivables, NOT day cash. */
    suspend fun creditSalesTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis)
            .filter { it.type == "SALE" && it.paymentMethod == "CREDIT" }
            .sumOf { it.total }

    /** SALE vouchers paid in cash only — the cash that actually entered the drawer. */
    suspend fun cashOnlySalesTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis)
            .filter { it.type == "SALE" && it.paymentMethod == "CASH" }
            .sumOf { it.total }

    suspend fun returnsTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis).filter { it.type == "RETURN" }.sumOf { it.total }

    /**
     * Returns refunded in cash — cash that left the drawer. A CREDIT return is a credit note
     * (no cash moves); anything else, including older returns saved without a method, is cash.
     */
    suspend fun cashReturnsTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis)
            .filter { it.type == "RETURN" && it.paymentMethod != "CREDIT" }
            .sumOf { it.total }

    suspend fun distinctCustomersSince(sinceMillis: Long): Int =
        dao.listSince(sinceMillis).map { it.customerId }.toSet().size

    fun observeByCustomerAndType(customerId: String, type: String): Flow<List<InvoiceEntity>> =
        dao.observeByCustomerAndType(customerId, type)

    suspend fun countUnsyncedSince(sinceMillis: Long): Int =
        dao.countUnsyncedSince(sinceMillis)

    /** Count of [type] vouchers created within [fromMillis, toMillis) — drives the per-type yearly sequence. */
    suspend fun countByTypeInRange(type: String, fromMillis: Long, toMillis: Long): Int =
        dao.countByTypeInRange(type, fromMillis, toMillis)

    /**
     * Next per-type sequence = the highest trailing sequence already used this year + 1
     * (i.e. last voucher number + 1). Robust offline: unlike a COUNT, it never reuses a
     * number when a voucher was cancelled/removed. Numbers look like `INV-2026-U-0001-5`,
     * so the sequence is the last `-`-separated segment.
     */
    suspend fun nextSeqForType(type: String, fromMillis: Long, toMillis: Long): Int {
        val maxSeq = dao.numbersByTypeInRange(type, fromMillis, toMillis)
            .mapNotNull { it.substringAfterLast('-').toIntOrNull() }
            .maxOrNull() ?: 0
        return maxSeq + 1
    }

    suspend fun save(entity: InvoiceEntity) = dao.upsert(entity)
}
