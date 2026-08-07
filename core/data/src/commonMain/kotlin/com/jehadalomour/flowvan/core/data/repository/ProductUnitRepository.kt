package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.ProductUnitDao
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.database.mapper.toEntity
import com.jehadalomour.flowvan.core.model.ProductUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductUnitRepository(private val dao: ProductUnitDao) {

    fun observeAll(): Flow<List<ProductUnit>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeByProduct(productId: String): Flow<List<ProductUnit>> =
        dao.observeByProduct(productId).map { rows -> rows.map { it.toDomain() } }

    suspend fun findById(id: String): ProductUnit? = dao.findById(id)?.toDomain()

    suspend fun findByProductAndCode(productId: String, code: String): ProductUnit? =
        dao.findByProductAndCode(productId, code)?.toDomain()

    suspend fun upsert(unit: ProductUnit) = dao.upsert(unit.toEntity())

    suspend fun upsertAll(units: List<ProductUnit>) = dao.upsertAll(units.map { it.toEntity() })

    /** Set this unit pool's absolute van quantity (from the per-rep van-stock feed). */
    suspend fun setStock(id: String, qty: Int) = dao.setStock(id, qty)

    suspend fun adjustStock(id: String, delta: Int) = dao.adjustStock(id, delta)

    /**
     * Refill the catalog's units WITHOUT losing per-unit stock.
     *
     * The catalog pull happens on login and on every home refresh, and it carries no stock —
     * so a wipe-and-reinsert (the old `deleteAll` + `upsertAll`) zeroed every variant pool
     * several times a day. Existing rows keep their [ProductUnit.vanStock]; only units the
     * server no longer lists are removed, in chunks so a large catalog can't blow past
     * SQLite's bound-variable limit.
     */
    suspend fun mergeAll(units: List<ProductUnit>) {
        val stockById = dao.stockSnapshot().associate { it.id to it.vanStock }
        dao.upsertAll(units.map { u -> u.toEntity().copy(vanStock = stockById[u.id] ?: u.vanStock) })
        val keep = units.mapTo(mutableSetOf()) { it.id }
        val stale = stockById.keys.filterNot { it in keep }
        stale.chunked(500).forEach { dao.deleteByIds(it) }
    }

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun deleteByProduct(productId: String) = dao.deleteByProduct(productId)

    suspend fun deleteAll() = dao.deleteAll()
}
