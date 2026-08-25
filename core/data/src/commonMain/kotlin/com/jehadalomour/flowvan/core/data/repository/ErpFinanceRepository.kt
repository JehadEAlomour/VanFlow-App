package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.ErpFinanceDao
import com.jehadalomour.flowvan.core.database.entity.ErpCustomerCacheEntity
import com.jehadalomour.flowvan.core.database.entity.ErpRepCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Cache of ERP money (the book of record) for offline-first display: the UI
 * observes these rows, and a ViewModel writes the latest fetch in. When a fetch
 * fails the previous row simply stays, carrying its "as of" time — so the screen
 * shows live figures online and the last-known ones (dated) offline.
 *
 * Deliberately entity-in / entity-out: the ViewModel owns the DTO↔entity mapping
 * (and the statement JSON), so this layer needs no dependency on core/network.
 */
class ErpFinanceRepository(private val dao: ErpFinanceDao) {

    fun observeCustomer(customerId: String): Flow<ErpCustomerCacheEntity?> =
        dao.observeCustomer(customerId)

    /** Read the current cached row (to merge a partial refresh without clobbering). */
    suspend fun getCustomer(customerId: String): ErpCustomerCacheEntity? =
        dao.getCustomer(customerId)

    suspend fun cacheCustomer(row: ErpCustomerCacheEntity) = dao.upsertCustomer(row)

    fun observeRep(repId: String): Flow<ErpRepCacheEntity?> = dao.observeRep(repId)

    suspend fun cacheRep(row: ErpRepCacheEntity) = dao.upsertRep(row)
}
