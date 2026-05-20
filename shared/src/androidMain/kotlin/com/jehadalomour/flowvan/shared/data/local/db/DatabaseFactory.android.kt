package com.jehadalomour.flowvan.shared.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun builder(): RoomDatabase.Builder<FlowVanDatabase> {
        val dbFile = context.getDatabasePath(FLOW_VAN_DB_NAME)
        return Room.databaseBuilder<FlowVanDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath,
            factory = { FlowVanDatabaseConstructor.initialize() },
        )
    }
}