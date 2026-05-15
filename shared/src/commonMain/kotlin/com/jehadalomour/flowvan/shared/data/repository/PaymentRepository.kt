package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.PaymentDao
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val dao: PaymentDao) {

    suspend fun confirmedTotalSince(sinceMillis: Long): Double =
        dao.listConfirmedSince(sinceMillis).sumOf { it.amount }

    fun observeByCustomer(customerId: String): Flow<List<PaymentEntity>> =
        dao.observeByCustomer(customerId)

    suspend fun totalByMethodSince(method: String, sinceMillis: Long): Double =
        dao.listByMethodSince(method, sinceMillis).sumOf { it.amount }

    suspend fun countUnsyncedSince(sinceMillis: Long): Int =
        dao.countUnsyncedSince(sinceMillis)

    suspend fun save(entity: PaymentEntity) = dao.upsert(entity)
}
