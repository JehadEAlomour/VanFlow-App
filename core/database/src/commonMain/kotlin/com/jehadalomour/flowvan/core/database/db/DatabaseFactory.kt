package com.jehadalomour.flowvan.shared.data.local.db

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun builder(): RoomDatabase.Builder<FlowVanDatabase>
}