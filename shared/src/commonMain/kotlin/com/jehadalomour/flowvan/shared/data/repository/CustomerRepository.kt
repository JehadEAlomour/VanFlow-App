package com.jehadalomour.flowvan.shared.data.repository

import com.jehadalomour.flowvan.shared.data.local.dao.CustomerDao
import com.jehadalomour.flowvan.shared.data.local.mapper.toDomain
import com.jehadalomour.flowvan.shared.domain.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepository(private val dao: CustomerDao) {

    fun observeRoute(): Flow<List<Customer>> =
        dao.observeRouteCustomers().map { rows -> rows.map { it.toDomain() } }

    fun observeAll(): Flow<List<Customer>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeById(id: String): Flow<Customer?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun findById(id: String): Customer? = dao.findById(id)?.toDomain()

    suspend fun adjustBalance(id: String, delta: Double) = dao.adjustBalance(id, delta)
}
