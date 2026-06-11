package com.jehadalomour.flowvan.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jehadalomour.flowvan.shared.data.local.entity.RouteStopEntity

@Dao
interface RouteStopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stops: List<RouteStopEntity>)

    @Query("SELECT * FROM route_stops WHERE userId = :userId AND planDate = :planDate ORDER BY stopOrder")
    suspend fun listForDate(userId: String, planDate: Long): List<RouteStopEntity>
}