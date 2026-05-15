package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.ShiftEntity

@Dao
interface ShiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shifts: List<ShiftEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE userId = :userId AND status = 'ACTIVE' LIMIT 1")
    suspend fun findActive(userId: String): ShiftEntity?

    @Query("UPDATE shifts SET endedAt = :endedAt, status = 'ENDED' WHERE id = :id")
    suspend fun endShift(id: String, endedAt: Long)

    @Query("SELECT COUNT(*) FROM shifts")
    suspend fun count(): Int
}