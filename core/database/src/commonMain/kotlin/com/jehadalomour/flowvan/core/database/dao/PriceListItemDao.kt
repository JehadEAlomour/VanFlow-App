package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jehadalomour.flowvan.core.database.entity.PriceListItemEntity

@Dao
interface PriceListItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PriceListItemEntity>)

    /** All item prices for one list (loaded into the voucher screen for a customer). */
    @Query("SELECT * FROM price_list_items WHERE priceListId = :priceListId")
    suspend fun listFor(priceListId: String): List<PriceListItemEntity>

    /** The price for one product under a list, or null when the item isn't on it. */
    @Query("SELECT unitPrice FROM price_list_items WHERE priceListId = :priceListId AND sku = :sku LIMIT 1")
    suspend fun priceFor(priceListId: String, sku: String): Double?

    @Query("DELETE FROM price_list_items")
    suspend fun deleteAll()

    /** Replace the whole cache so lists/items removed server-side disappear locally. */
    @Transaction
    suspend fun replaceAll(items: List<PriceListItemEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
