package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.LocationPointEntity

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationPointEntity)

    @Query("SELECT COUNT(*) FROM location_points WHERE synced = 0")
    suspend fun countUnsynced(): Int

    @Query("SELECT * FROM location_points WHERE synced = 0 LIMIT :limit")
    suspend fun findUnsynced(limit: Int = 100): List<LocationPointEntity>

    @Query("UPDATE location_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}