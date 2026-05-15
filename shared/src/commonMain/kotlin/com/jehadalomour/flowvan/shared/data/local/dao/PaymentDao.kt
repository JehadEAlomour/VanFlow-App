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

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE createdAt >= :sinceMillis AND status = 'CONFIRMED'")
    suspend fun listConfirmedSince(sinceMillis: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE method = :method AND createdAt >= :sinceMillis AND status = 'CONFIRMED'")
    suspend fun listByMethodSince(method: String, sinceMillis: Long): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments WHERE createdAt >= :sinceMillis AND syncedAt IS NULL")
    suspend fun countUnsyncedSince(sinceMillis: Long): Int

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun count(): Int
}