package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val dao: InvoiceDao) {

    suspend fun listSince(sinceMillis: Long) = dao.listSince(sinceMillis)

    suspend fun salesTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis).filter { it.type == "SALE" }.sumOf { it.total }

    suspend fun returnsTotalSince(sinceMillis: Long): Double =
        dao.listSince(sinceMillis).filter { it.type == "RETURN" }.sumOf { it.total }

    suspend fun distinctCustomersSince(sinceMillis: Long): Int =
        dao.listSince(sinceMillis).map { it.customerId }.toSet().size

    fun observeByCustomerAndType(customerId: String, type: String): Flow<List<InvoiceEntity>> =
        dao.observeByCustomerAndType(customerId, type)

    suspend fun countUnsyncedSince(sinceMillis: Long): Int =
        dao.countUnsyncedSince(sinceMillis)

    /** Count of [type] vouchers created within [fromMillis, toMillis) — drives the per-type yearly sequence. */
    suspend fun countByTypeInRange(type: String, fromMillis: Long, toMillis: Long): Int =
        dao.countByTypeInRange(type, fromMillis, toMillis)

    suspend fun save(entity: InvoiceEntity) = dao.upsert(entity)
}
