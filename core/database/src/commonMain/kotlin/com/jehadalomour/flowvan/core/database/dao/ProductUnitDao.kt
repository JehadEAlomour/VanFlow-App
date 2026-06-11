package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.ProductUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductUnitDao {

    @Query("SELECT * FROM product_units ORDER BY conversion_qty ASC")
    fun observeAll(): Flow<List<ProductUnitEntity>>

    @Query("SELECT * FROM product_units WHERE product_id = :productId ORDER BY conversion_qty ASC")
    fun observeByProduct(productId: String): Flow<List<ProductUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: ProductUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(units: List<ProductUnitEntity>)

    @Query("DELETE FROM product_units WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM product_units WHERE product_id = :productId")
    suspend fun deleteByProduct(productId: String)
}
