package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM products ORDER BY category, nameAr")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE vanStock < minStock ORDER BY vanStock ASC")
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun findBySku(sku: String): ProductEntity?

    @Query("UPDATE products SET vanStock = vanStock + :delta WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Int)

    @Query("UPDATE products SET vanStock = :qty WHERE id = :id")
    suspend fun setStock(id: String, qty: Int)

    @Query("SELECT id FROM products")
    suspend fun allIds(): List<String>

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}