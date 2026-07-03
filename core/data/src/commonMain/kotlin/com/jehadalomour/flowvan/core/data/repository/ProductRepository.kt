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
}
