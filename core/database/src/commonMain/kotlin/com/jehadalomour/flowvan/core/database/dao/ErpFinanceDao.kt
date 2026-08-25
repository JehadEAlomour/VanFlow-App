package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.core.database.entity.ErpCustomerCacheEntity
import com.jehadalomour.flowvan.core.database.entity.ErpRepCacheEntity
import kotlinx.coroutines.flow.Flow

/** Cached ERP money (customer balance/statement + the rep's own balance). */
@Dao
interface ErpFinanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(row: ErpCustomerCacheEntity)

    @Query("SELECT * FROM erp_customer_cache WHERE customerId = :customerId LIMIT 1")
    fun observeCustomer(customerId: String): Flow<ErpCustomerCacheEntity?>

    @Query("SELECT * FROM erp_customer_cache WHERE customerId = :customerId LIMIT 1")
    suspend fun getCustomer(customerId: String): ErpCustomerCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRep(row: ErpRepCacheEntity)

    @Query("SELECT * FROM erp_rep_cache WHERE repId = :repId LIMIT 1")
    fun observeRep(repId: String): Flow<ErpRepCacheEntity?>
}
