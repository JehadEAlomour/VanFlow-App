package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
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

    suspend fun save(entity: InvoiceEntity) = dao.upsert(entity)
}
