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

    @Query("SELECT * FROM invoices WHERE createdAt >= :sinceMillis AND status != 'CANCELLED'")
    suspend fun listSince(sinceMillis: Long): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int
}