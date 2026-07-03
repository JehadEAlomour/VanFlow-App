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

    suspend fun upsert(unit: ProductUnit) = dao.upsert(unit.toEntity())

    suspend fun upsertAll(units: List<ProductUnit>) = dao.upsertAll(units.map { it.toEntity() })

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun deleteByProduct(productId: String) = dao.deleteByProduct(productId)

    suspend fun deleteAll() = dao.deleteAll()
}
