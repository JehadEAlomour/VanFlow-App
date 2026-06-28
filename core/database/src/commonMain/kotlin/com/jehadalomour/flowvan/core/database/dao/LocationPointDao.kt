package com.jehadalomour.flowvan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.core.database.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationPointEntity)

    @Query("SELECT COUNT(*) FROM location_points WHERE synced = 0")
    suspend fun countUnsynced(): Int

    /** Reactive pending-queue size for the Home "N بانتظار المزامنة" chip. */
    @Query("SELECT COUNT(*) FROM location_points WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM location_points WHERE synced = 0 LIMIT :limit")
    suspend fun findUnsynced(limit: Int = 100): List<LocationPointEntity>

    @Query("UPDATE location_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    /**
     * Queue cap (F12 §4): keep only the newest [cap] pending pings, dropping the oldest
     * beyond it. Bounds storage on days fully offline — the dashboard cares about recent
     * movement and the bulk endpoint is capped anyway. No-op while under the cap.
     */
    @Query(
        """
        DELETE FROM location_points WHERE synced = 0 AND id NOT IN (
            SELECT id FROM location_points WHERE synced = 0 ORDER BY recordedAt DESC LIMIT :cap
        )
        """,
    )
    suspend fun trimQueueToCap(cap: Int)
}