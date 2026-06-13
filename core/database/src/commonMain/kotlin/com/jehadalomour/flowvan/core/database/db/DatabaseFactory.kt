package com.jehadalomour.flowvan.core.database.db

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun builder(): RoomDatabase.Builder<FlowVanDatabase>
}