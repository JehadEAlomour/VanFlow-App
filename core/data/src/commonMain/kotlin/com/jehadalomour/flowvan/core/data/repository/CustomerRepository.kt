package com.jehadalomour.flowvan.core.data.repository

import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import com.jehadalomour.flowvan.core.database.mapper.toDomain
import com.jehadalomour.flowvan.core.model.Customer
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

    /** Offline-first cache refill from the backend (upsert — keeps other rows). */
    suspend fun cacheAll(customers: List<CustomerEntity>) = dao.upsertAll(customers)

    /**
     * Full-sync replacement: swap the entire local cache for the server's list
     * (which is scoped to the logged-in salesman), so customers that are no
     * longer theirs are dropped instead of lingering. Use only for the complete
     * refresh, never for single-customer caching.
     */
    suspend fun replaceAll(customers: List<CustomerEntity>) = dao.replaceAll(customers)

    /** Insert/replace a single customer (e.g. one just created on the backend). */
    suspend fun save(customer: CustomerEntity) = dao.upsertAll(listOf(customer))
}
