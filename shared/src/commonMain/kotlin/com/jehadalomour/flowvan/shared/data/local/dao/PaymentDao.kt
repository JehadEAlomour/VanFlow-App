package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(payments: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    fun observeById(id: String): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    fun observeByCustomerRange(customerId: String, fromMillis: Long, toMillis: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND method = :method AND createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    fun observeByCustomerMethodRange(customerId: String, method: String, fromMillis: Long, toMillis: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE createdAt >= :sinceMillis AND status = 'CONFIRMED'")
    suspend fun listConfirmedSince(sinceMillis: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE method = :method AND createdAt >= :sinceMillis AND status = 'CONFIRMED'")
    suspend fun listByMethodSince(method: String, sinceMillis: Long): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments WHERE createdAt >= :sinceMillis AND syncedAt IS NULL")
    suspend fun countUnsyncedSince(sinceMillis: Long): Int

    @Query("SELECT * FROM payments WHERE syncedAt IS NULL LIMIT :limit")
    suspend fun findUnsynced(limit: Int = 50): List<PaymentEntity>

    @Query("UPDATE payments SET syncedAt = :now WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, now: Long)

    @Query("SELECT * FROM payments WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    fun observeAllByRange(from: Long, to: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE method = :method AND createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    fun observeAllByMethodAndRange(method: String, from: Long, to: Long): Flow<List<PaymentEntity>>

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun count(): Int
}