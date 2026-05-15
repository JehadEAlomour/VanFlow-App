package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.ProductDao
import com.jehadalomour.flowvan.shared.data.local.mapper.toDomain
import com.jehadalomour.flowvan.shared.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(private val dao: ProductDao) {
    fun observeAll(): Flow<List<Product>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeLowStock(): Flow<List<Product>> =
        dao.observeLowStock().map { rows -> rows.map { it.toDomain() } }

    suspend fun findById(id: String): Product? = dao.findById(id)?.toDomain()

    suspend fun adjustStock(id: String, delta: Int) = dao.adjustStock(id, delta)
}
