package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jehadalomour.flowvan.core.database.entity.TobaccoTaxProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TobaccoTaxProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<TobaccoTaxProfileEntity>)

    @Query("SELECT * FROM tobacco_tax_profiles")
    suspend fun list(): List<TobaccoTaxProfileEntity>

    @Query("SELECT * FROM tobacco_tax_profiles")
    fun observeAll(): Flow<List<TobaccoTaxProfileEntity>>

    @Query("DELETE FROM tobacco_tax_profiles")
    suspend fun deleteAll()

    /** Replace the cache so profiles removed/deactivated server-side disappear locally. */
    @Transaction
    suspend fun replaceAll(profiles: List<TobaccoTaxProfileEntity>) {
        deleteAll()
        upsertAll(profiles)
    }
}
