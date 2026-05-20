package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(invoices: List<InvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND type = :type ORDER BY createdAt DESC")
    fun observeByCustomerAndType(customerId: String, type: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    fun observeByCustomerRange(customerId: String, fromMillis: Long, toMillis: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND type = :type AND createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    fun observeByCustomerTypeRange(customerId: String, type: String, fromMillis: Long, toMillis: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE createdAt >= :sinceMillis AND status != 'CANCELLED'")
    suspend fun listSince(sinceMillis: Long): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE createdAt >= :sinceMillis AND syncedAt IS NULL")
    suspend fun countUnsyncedSince(sinceMillis: Long): Int

    @Query("SELECT * FROM invoices WHERE syncedAt IS NULL LIMIT :limit")
    suspend fun findUnsynced(limit: Int = 50): List<InvoiceEntity>

    @Query("UPDATE invoices SET syncedAt = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)

    @Query("SELECT * FROM invoices WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    fun observeAllByRange(from: Long, to: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE type = :type AND createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    fun observeAllByTypeAndRange(type: String, from: Long, to: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int
}