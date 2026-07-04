package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jehadalomour.flowvan.core.database.entity.OfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(offers: List<OfferEntity>)

    /** Active offers in the server's ranking order (highest priority first, oldest wins ties). */
    @Query("SELECT * FROM offers WHERE isActive = 1 ORDER BY priority DESC, createdAt ASC")
    fun observeActive(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE isActive = 1 ORDER BY priority DESC, createdAt ASC")
    suspend fun listActive(): List<OfferEntity>

    @Query("SELECT MAX(cachedAt) FROM offers")
    suspend fun lastRefreshedAt(): Long?

    @Query("DELETE FROM offers")
    suspend fun deleteAll()

    /** Replace the whole cache so offers deactivated/deleted server-side disappear locally. */
    @Transaction
    suspend fun replaceAll(offers: List<OfferEntity>) {
        deleteAll()
        upsertAll(offers)
    }
}
