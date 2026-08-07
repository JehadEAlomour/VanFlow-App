package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.ProductDao
import com.jehadalomour.flowvan.core.database.entity.ProductEntity
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(private val dao: ProductDao) {
    fun observeAll(): Flow<List<Product>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeLowStock(): Flow<List<Product>> =
        dao.observeLowStock().map { rows -> rows.map { it.toDomain() } }

    suspend fun findById(id: String): Product? = dao.findById(id)?.toDomain()

    suspend fun findBySku(sku: String): Product? = dao.findBySku(sku)?.toDomain()

    suspend fun adjustStock(id: String, delta: Int) = dao.adjustStock(id, delta)

    /** Set absolute van quantity (used when pulling per-rep stock from the backend). */
    suspend fun setStock(id: String, qty: Int) = dao.setStock(id, qty)

    /** Offline-first cache refill from the backend. */
    suspend fun cacheAll(products: List<ProductEntity>) = dao.upsertAll(products)

    /**
     * Refill the catalog AND drop items the server no longer lists.
     *
     * [cacheAll] only upserts, so an item deleted in the ERP survived on every device
     * forever — and because the dead row kept its last-known `vanStock` while the live
     * one is re-seeded at 0 and re-overlaid from the van feed, the catalog list (which
     * filters on `vanStock > 0`) showed ONLY the dead rows. That is how a rep ended up
     * with eleven renamed colour items and none of the real ones, each opening a sheet
     * with no units because the units had moved to the surviving item.
     *
     * Customers already work this way (`CustomerRepository.replaceAll`); products did not.
     * Deleting cascades to `product_units`, which is correct: a unit of a dead item is dead.
     */
    suspend fun replaceAll(products: List<ProductEntity>) {
        dao.upsertAll(products)
        val keep = products.mapTo(mutableSetOf()) { it.id }
        // Chunked so a large catalog can't blow past SQLite's bound-variable limit.
        dao.allIds().filterNot { it in keep }.chunked(500).forEach { dao.deleteByIds(it) }
    }
}
