package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE isOnRoute = 1 ORDER BY visitOrder ASC")
    fun observeRouteCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY isOnRoute DESC, visitOrder ASC, nameAr ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM customers WHERE isOnRoute = 1")
    suspend fun countOnRoute(): Int

    @Query("UPDATE customers SET balance = balance + :delta WHERE id = :id")
    suspend fun adjustBalance(id: String, delta: Double)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    /**
     * Replace the whole cache so customers no longer returned by the server
     * (e.g. reassigned to another salesman) disappear locally instead of
     * lingering. The list is already scoped to the logged-in rep server-side.
     */
    @Transaction
    suspend fun replaceAll(customers: List<CustomerEntity>) {
        deleteAll()
        upsertAll(customers)
    }
}