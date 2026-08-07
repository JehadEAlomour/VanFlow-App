package com.jehadalomour.flowvan.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.core.database.entity.ProductUnitEntity
import kotlinx.coroutines.flow.Flow

/** Just the per-unit stock, so a catalog refresh can carry it across the merge. */
data class ProductUnitStock(
    val id: String,
    @ColumnInfo(name = "van_stock") val vanStock: Int,
)

@Dao
interface ProductUnitDao {

    @Query("SELECT * FROM product_units ORDER BY conversion_qty ASC")
    fun observeAll(): Flow<List<ProductUnitEntity>>

    @Query("SELECT * FROM product_units WHERE product_id = :productId ORDER BY conversion_qty ASC")
    fun observeByProduct(productId: String): Flow<List<ProductUnitEntity>>

    @Query("SELECT * FROM product_units WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProductUnitEntity?

    @Query("SELECT * FROM product_units WHERE product_id = :productId AND code = :code LIMIT 1")
    suspend fun findByProductAndCode(productId: String, code: String): ProductUnitEntity?

    @Query("SELECT id, van_stock FROM product_units")
    suspend fun stockSnapshot(): List<ProductUnitStock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: ProductUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(units: List<ProductUnitEntity>)

    @Query("UPDATE product_units SET van_stock = van_stock + :delta WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Int)

    @Query("UPDATE product_units SET van_stock = :qty WHERE id = :id")
    suspend fun setStock(id: String, qty: Int)

    @Query("DELETE FROM product_units WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM product_units WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM product_units WHERE product_id = :productId")
    suspend fun deleteByProduct(productId: String)

    @Query("DELETE FROM product_units")
    suspend fun deleteAll()
}
